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

            injectIfAbsent(implementationBuilder, "querydslPredicateExecutor", ParameterizedTypeName.get(
                    ClassName.bestGuess("org.springframework.data.querydsl.QuerydslPredicateExecutor"),
                    TypeName.get(entity.getType())
            ));

            if (crudType == CrudType.CREATE || crudType == CrudType.UPDATE) {
                Variable entityVar = Variable.of(entity.getType(), names.newName("entity"));
                if (crudType == CrudType.CREATE) {
                    methodBuilder.addStatement("$1T $2N = new $1T()", entityVar.getType(), entityVar.getName());
                } else {
                    QueryMapping idSource = QueryMapping
                            .find(processingEnv, entity, serviceMethod.getParameters())
                            .stream()
                            .filter(it -> it.attr.equals(entity.getIdAttribute()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Can't find a way to load entity with id"));
                    methodBuilder.addStatement("$1T $2N = this.jpaRepository.getById($3L)",
                            entityVar.getType(), entityVar.getName(), idSource.valExpr.getCode());
                }

                WriteMapping.find(processingEnv, entity.getType(), serviceMethod.getParameters()).forEach(source -> {
                    SetAttributeHandler handler = SetAttributeHandler.find(
                            processingEnv, entity, serviceMethods,
                            source.targetSetter.getWriteName(),
                            source.targetSetter.getWriteType()
                    );
                    if (handler != null) {
                        methodBuilder.addStatement(handler.invoke(entityVar, source.value));
                    } else {
                        methodBuilder.addStatement("$N.$N($L)",
                                entityVar.getName(),
                                source.targetSetter.getSimpleName(),
                                source.value.getCode());
                    }
                });

                methodBuilder.addStatement("this.jpaRepository.save($N)", entityVar.getName());
                convertAndReturn(methodBuilder, names, serviceMethod, entity, entityVar);
            } //
            else if (crudType == CrudType.QUERY) {
                TypeMirror returnType = serviceMethod.getReturnType();
                List<? extends Parameter> params = serviceMethod.getParameters();
                if (params.size() == 1 && typeOps.isSame(params.get(0).getType(), entity.getIdAttributeType())) {
                    Variable filterVar = null;
                    List<FilterHandler> filterHandlers = FilterHandler.findAll(processingEnv, serviceMethods);
                    for (FilterHandler filterHandler : filterHandlers) {
                        if (filterHandler.supports(params.get(0).getType())) {
                            filterVar = Variable.of(typeOps.getDeclared(PREDICATE_FQN), names.newName("filter"));
                            methodBuilder.addStatement("$T $N = $L",
                                    filterVar.getType(), filterVar.getName(),
                                    filterHandler.invoke(params.get(0)).getCode());
                            break;
                        }
                    }
                    if (filterVar != null) {
                        for (FilterHandler filterHandler : filterHandlers) {
                            if (filterHandler.supports(typeOps.getDeclared(PREDICATE_FQN))) {
                                methodBuilder.addStatement("$N = $L",
                                        filterVar.getName(), filterHandler.invoke(filterVar).getCode());
                            }
                        }
                        Variable entityVar = Variable.of(entity.getType(), names.newName("entity"));
                        methodBuilder.addStatement(
                                "$T $N = this.querydslPredicateExecutor.findOne($N).orElse(null)",
                                entityVar.getType(), entityVar.getName(), filterVar.getName());
                        convertAndReturn(methodBuilder, names, serviceMethod, entity, entityVar);
                    }

                }
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
                return DELETE;
            }
            if (method.getSimpleName().startsWith("find")) {
                return QUERY;
            }
            return UNKNOWN;
        }
    }

    private void convertAndReturn(MethodSpec.Builder builder, NameAllocator names,
                                  Method method, Entity entity, Variable fromVar) {
        TypeMirror returnType = method.getReturnType();
        if (typeOps.isAssignable(entity.getIdAttributeType(), returnType)) {
            builder.addStatement("return $N.$N()",
                    fromVar.getName(), entity.getIdReadMethod().getSimpleName());
        }//
        else if (typeOps.isDeclared(returnType)) {
            DeclaredType targetType = (DeclaredType) returnType;

            Variable resultVar = Variable.of(returnType, names.newName("result"));
            builder.addStatement("$1T $2N = new $1T()", resultVar.getType(), resultVar.getName());

            WriteMapping.find(processingEnv, targetType, Collections.singletonList(fromVar)).forEach(mapping -> {
                builder.addStatement("$N.$N($L)",
                        resultVar.getName(),
                        mapping.targetSetter.getSimpleName(),
                        mapping.value.getCode());
            });

            builder.addStatement("return $N", resultVar.getName());
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

    private interface SetAttributeHandler {
        CodeBlock invoke(Expression entityExpr, Expression valueExpr);

        static SetAttributeHandler find(ProcessingEnvironment processingEnv,
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

    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_BASE_FQN = "com.querydsl.core.types.dsl.EntityPathBase";

    private interface FilterHandler {
        boolean supports(TypeMirror type);

        Expression invoke(Expression valueExpr);

        static List<FilterHandler> findAll(ProcessingEnvironment processingEnv,
                                           MethodCollection methods) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);
            List<FilterHandler> handlers = Lists.newArrayList();

            outer:
            for (Method method : methods) {
                List<? extends Parameter> params = method.getParameters();
                Set<Modifier> modifiers = method.getModifiers();
                if (modifiers.contains(Modifier.PRIVATE)
                        || !typeOps.isAssignable(method.getReturnType(), PREDICATE_FQN)
                        || params.size() < 1) {
                    continue;
                }
                List<Expression> extraParams = Lists.newArrayList();
                for (int i = 1; i < params.size(); i++) {
                    Parameter param = params.get(i);
                    if (typeOps.isAssignable(param.getType(), ENTITY_PATH_BASE_FQN)) {
                        DeclaredType entityType = (DeclaredType) typeOps.resolveTypeParameter(
                                (DeclaredType) param.getType(), ENTITY_PATH_BASE_FQN, 0);
                        ClassName entityTypeName = ClassName.get((TypeElement) entityType.asElement());
                        ClassName queryTypeName = ClassName.get(
                                entityTypeName.packageName(), "Q" + entityTypeName.simpleName());
                        extraParams.add(Expression.of(typeOps.getDeclared(queryTypeName.canonicalName()),
                                "$T.$N", queryTypeName, StringUtils.uncapitalize(entityTypeName.simpleName())));
                    } else {
                        continue outer;
                    }
                }
                handlers.add(new FilterHandler() {
                    @Override
                    public boolean supports(TypeMirror type) {
                        return typeOps.isSame(type, params.get(0).getType());
                    }

                    @Override
                    public Expression invoke(Expression valueExpr) {
                        return Expression.of(
                                params.get(0).getType(),
                                "$N($L, $L)",
                                method.getSimpleName(), valueExpr.getCode(),
                                extraParams.stream().map(Expression::getCode).collect(CodeBlock.joining(", "))
                        );
                    }
                });
            }
            return handlers;
        }
    }

    @RequiredArgsConstructor
    private static class WriteMapping {
        public final WriteMethod targetSetter;
        public final Expression value;

        public static List<WriteMapping> find(ProcessingEnvironment processingEnv,
                                              DeclaredType targetType,
                                              List<? extends Variable> variables) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);
            Methods methodUtils = Methods.instanceOf(processingEnv);
            MethodCollection entityMethods = methodUtils.allOf(targetType);

            List<WriteMapping> writeMappings = Lists.newArrayList();
            for (Variable variable : variables) {
                WriteMethod entitySetter = entityMethods.findWriter(variable.getName(), variable.getType());
                if (entitySetter != null) {
                    writeMappings.add(new WriteMapping(entitySetter, variable));
                    continue;
                }
                if (typeOps.isDeclared(variable.getType())) {
                    for (ReadMethod paramGetter : methodUtils.allOf((DeclaredType) variable.getType()).readers()) {
                        entitySetter = entityMethods.findWriter(paramGetter.getReadName(), paramGetter.getReadType());
                        if (entitySetter != null) {
                            writeMappings.add(new WriteMapping(entitySetter, paramGetter.toExpression(variable)));
                        }
                    }
                }
            }
            return writeMappings;
        }
    }

    @RequiredArgsConstructor
    private static class QueryMapping {
        public final String attr;
        public final Expression valExpr;

        public static List<QueryMapping> find(ProcessingEnvironment processingEnv,
                                              Entity entity,
                                              List<? extends Variable> variables) {
            TypeOps typeOps = TypeOps.instanceOf(processingEnv);
            Methods methodUtils = Methods.instanceOf(processingEnv);
            MethodCollection entityMethods = methodUtils.allOf(entity.getType());

            List<QueryMapping> queryMappings = Lists.newArrayList();
            for (Variable variable : variables) {
                ReadMethod entityGetter = entityMethods.findReader(variable.getName(), variable.getType());
                if (entityGetter != null) {
                    queryMappings.add(new QueryMapping(entityGetter.getReadName(), variable));
                    continue;
                }
                if (typeOps.isDeclared(variable.getType())) {
                    for (ReadMethod paramGetter : methodUtils.allOf((DeclaredType) variable.getType()).readers()) {
                        entityGetter = entityMethods.findReader(paramGetter.getReadName(), paramGetter.getReadType());
                        if (entityGetter != null) {
                            queryMappings.add(new QueryMapping(
                                    entityGetter.getReadName(),
                                    paramGetter.toExpression(variable)
                            ));
                        }
                    }
                }
            }
            return queryMappings;
        }
    }
}
