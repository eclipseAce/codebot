package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.codebot.apt.coding.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class EntityServiceProcessor extends AbstractProcessor {
    private TypeOps typeOps;
    private Annotations annotationUtils;
    private Methods methodUtils;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(EntityService.class.getName());
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeOps = TypeOps.instanceOf(processingEnv);
        this.annotationUtils = Annotations.instanceOf(processingEnv);
        this.methodUtils = Methods.instanceOf(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(EntityService.class))) {
            Annotation annotation = annotationUtils.find(element, EntityService.class);
            try {
                process(element, annotation);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        Throwables.getStackTraceAsString(e),
                        element, annotation.getMirror()
                );
            }
        }
        return false;
    }

    protected void process(TypeElement element, Annotation annotation) throws IOException {
        Entity entity = Entity.of(processingEnv, annotation.getType("value"));
        MethodCollection serviceMethods = methodUtils.allOf(typeOps.getDeclared(element));
        MethodCollection entityMethods = methodUtils.allOf(entity.getType());

        ClassName serviceName = ClassName.get(element);
        ClassName implementationName = ClassName.get(serviceName.packageName(), serviceName.simpleName() + "Impl");
        TypeSpec.Builder implementationBuilder = TypeSpec.classBuilder(implementationName)
                .addModifiers(Modifier.PUBLIC);
        if (element.getKind() == ElementKind.CLASS) {
            implementationBuilder.superclass(serviceName);
        } else if (element.getKind() == ElementKind.INTERFACE) {
            implementationBuilder.addSuperinterface(serviceName);
        } else {
            throw new IllegalArgumentException("EntityService must be class or interface");
        }

        for (Method serviceMethod : serviceMethods) {
            if (!serviceMethod.getModifiers().contains(Modifier.ABSTRACT)) {
                continue;
            }

            CrudType crudType = CrudType.match(serviceMethod);
            MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                    serviceMethod.getElement(),
                    serviceMethod.getContainingType(),
                    processingEnv.getTypeUtils()
            );
            NameAllocator names = new NameAllocator();
            methodBuilder.parameters.forEach(it -> names.newName(it.name));

            injectIfAbsent(implementationBuilder, "jpaRepository", ParameterizedTypeName.get(
                    ClassName.bestGuess("org.springframework.data.jpa.repository.JpaRepository"),
                    TypeName.get(entity.getType()),
                    TypeName.get(entity.getIdAttributeType()).box()
            ));

            if (crudType == CrudType.CREATE || crudType == CrudType.UPDATE) {
                Variable entityVar = Variable.of(entity.getType(), names.newName("entity"));
                if (crudType == CrudType.CREATE) {
                    methodBuilder.addStatement("$1T $2N = new $1T()", entityVar.getType(), entityVar.getName());
                } else {
                    QuerySource idSource = QuerySource
                            .findQuerySources(processingEnv, entity, serviceMethod.getParameters())
                            .stream()
                            .filter(it -> it.attr.equals(entity.getIdAttribute()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Can't find a way to load entity with id"));
                    methodBuilder.addStatement("$1T $2N = this.jpaRepository.getById($3L)",
                            entityVar.getType(), entityVar.getName(), idSource.valExpr.getCode());
                }

                WriteSource.findWriteSources(processingEnv, entity, serviceMethod.getParameters()).forEach(source -> {
                    AttributeHandler handler = AttributeHandler.findAttributeHandler(
                            processingEnv, entity, serviceMethods,
                            source.attrSetter.getWriteName(),
                            source.attrSetter.getWriteType()
                    );
                    if (handler != null) {
                        methodBuilder.addStatement(handler.invoke(entityVar, source.valExpr));
                    } else {
                        methodBuilder.addStatement("$N.$N($L)",
                                entityVar.getName(),
                                source.attrSetter.getSimpleName(),
                                source.valExpr.getCode());
                    }
                });

                methodBuilder.addStatement("this.jpaRepository.save($N)", entityVar.getName());
            }

            implementationBuilder.addMethod(methodBuilder.build());
        }

        JavaFile.builder(implementationName.packageName(), implementationBuilder.build()).build()
                .writeTo(processingEnv.getFiler());
    }

    private enum CrudType {
        CREATE, UPDATE, DELETE, QUERY, UNKNOWN;

        public static CrudType match(Method method) {
            if (method.getSimpleName().startsWith("create")) {
                return CREATE;
            }
            if (method.getSimpleName().startsWith("update")) {
                return UPDATE;
            }
            if (method.getSimpleName().startsWith("delete")) {
                return UPDATE;
            }
            if (method.getSimpleName().startsWith("find")) {
                return QUERY;
            }
            return UNKNOWN;
        }
    }

    private void injectIfAbsent(TypeSpec.Builder typeBuilder, String name, TypeName typeName) {
        for (FieldSpec field : typeBuilder.fieldSpecs) {
            if (field.name.equals(name)) {
                return;
            }
        }
        typeBuilder.addField(FieldSpec
                .builder(typeName, name, Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess("org.springframework.beans.factory.annotation.Autowired"))
                .build()
        );
    }

    private interface AttributeHandler {
        CodeBlock invoke(Expression entityExpr, Expression valueExpr);

        static AttributeHandler findAttributeHandler(ProcessingEnvironment processingEnv,
                                                     Entity entity, MethodCollection methods,
                                                     String attribute, TypeMirror attributeType) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);

            for (Method method : methods) {
                List<? extends Parameter> parameters = method.getParameters();
                Set<Modifier> modifiers = method.getModifiers();
                if (modifiers.contains(Modifier.ABSTRACT)
                        || modifiers.contains(Modifier.PRIVATE)
                        || !method.getSimpleName().equals("set" + StringUtils.capitalize(attribute))
                        || parameters.size() != 2
                        || !typeOps.isSame(parameters.get(0).getType(), entity.getType())
                        || !typeOps.isAssignable(parameters.get(1).getType(), attributeType)) {
                    continue;
                }
                return (entityExpr, valueExpr) -> CodeBlock.of("$N($L, $L)",
                        method.getSimpleName(), entityExpr.getCode(), valueExpr.getCode());
            }
            return null;
        }
    }

    @RequiredArgsConstructor
    private static class WriteSource {
        public final WriteMethod attrSetter;
        public final Expression valExpr;

        public static List<WriteSource> findWriteSources(ProcessingEnvironment processingEnv,
                                                         Entity entity, List<? extends Variable> variables) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);
            Methods methodUtils = Methods.instanceOf(processingEnv);
            MethodCollection entityMethods = methodUtils.allOf(entity.getType());

            List<WriteSource> writeSources = Lists.newArrayList();
            for (Variable variable : variables) {
                WriteMethod entitySetter = entityMethods.findWriter(variable.getName(), variable.getType());
                if (entitySetter != null) {
                    writeSources.add(new WriteSource(entitySetter, variable));
                    continue;
                }
                if (typeOps.isDeclared(variable.getType())) {
                    for (ReadMethod paramGetter : methodUtils.allOf((DeclaredType) variable.getType()).readers()) {
                        entitySetter = entityMethods.findWriter(paramGetter.getReadName(), paramGetter.getReadType());
                        if (entitySetter != null) {
                            writeSources.add(new WriteSource(entitySetter, paramGetter.toExpression(variable)));
                        }
                    }
                }
            }
            return writeSources;
        }
    }

    @RequiredArgsConstructor
    private static class QuerySource {
        public final String attr;
        public final Expression valExpr;

        public static List<QuerySource> findQuerySources(ProcessingEnvironment processingEnv,
                                                         Entity entity, List<? extends Variable> variables) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);
            Methods methodUtils = Methods.instanceOf(processingEnv);
            MethodCollection entityMethods = methodUtils.allOf(entity.getType());

            List<QuerySource> querySources = Lists.newArrayList();
            for (Variable variable : variables) {
                ReadMethod entityGetter = entityMethods.findReader(variable.getName(), variable.getType());
                if (entityGetter != null) {
                    querySources.add(new QuerySource(entityGetter.getReadName(), variable));
                    continue;
                }
                if (typeOps.isDeclared(variable.getType())) {
                    for (ReadMethod paramGetter : methodUtils.allOf((DeclaredType) variable.getType()).readers()) {
                        entityGetter = entityMethods.findReader(paramGetter.getReadName(), paramGetter.getReadType());
                        if (entityGetter != null) {
                            querySources.add(new QuerySource(
                                    entityGetter.getReadName(),
                                    paramGetter.toExpression(variable)
                            ));
                        }
                    }
                }
            }
            return querySources;
        }
    }
}
