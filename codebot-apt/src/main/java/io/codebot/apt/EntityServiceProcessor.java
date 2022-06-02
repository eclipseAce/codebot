package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.codebot.apt.coding.CodeWriter;
import io.codebot.apt.coding.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
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

    private static final String PAGE_FQN = "org.springframework.data.domain.Page";
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    protected void process(TypeElement element, Annotation annotation) throws IOException {
        Entity entity = Entity.of(processingEnv, annotation.getType("value"));
        MethodCollection serviceMethods = methodUtils.allOf(typeOps.getDeclared(element));

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
            CodeWriter writer = CodeWriter.create(methodBuilder);
            MappingCodes mappingCodes = MappingCodes.instanceOf(processingEnv, serviceMethods);
            ConversionCodes conversionCodes = ConversionCodes.instanceOf(processingEnv, mappingCodes);
            QuerydslCodes querydslCodes = QuerydslCodes.instanceOf(
                    processingEnv, implementationBuilder, entity, serviceMethods);

            if (crudType == CrudType.CREATE || crudType == CrudType.UPDATE) {
                Variable entityVar;
                if (crudType == CrudType.CREATE) {
                    entityVar = writer.writeNewVariable("entity", entity.getType(),
                            CodeBlock.of("new $T()", entity.getType()));
                } else {
                    Variable predicateVar = querydslCodes.createPredicate(writer, serviceMethod.getParameters(),
                            getter -> getter.getReadName().equals(entity.getIdAttribute()));
                    entityVar = querydslCodes.findOneEntity(writer, predicateVar);
                }
                mappingCodes.copyProperties(writer, entityVar, serviceMethod.getParameters());
                querydslCodes.saveEntity(writer, entityVar);
                conversionCodes.convertAndReturn(writer, entity, entityVar, serviceMethod.getReturnType());
            } //
            else if (crudType == CrudType.QUERY) {
                List<Parameter> filterParams = Lists.newArrayList();
                List<Parameter> pageableParams = Lists.newArrayList();
                for (Parameter param : serviceMethod.getParameters()) {
                    if (typeOps.isAssignable(param.getType(), PAGEABLE_FQN)) {
                        pageableParams.add(param);
                    } else {
                        filterParams.add(param);
                    }
                }
                Variable predicateVar = querydslCodes.createPredicate(writer, filterParams);
                Variable resultVar;
                if (typeOps.isAssignableToList(serviceMethod.getReturnType())) {
                    resultVar = querydslCodes.findAllEntities(writer, predicateVar, null);
                } else if (typeOps.isAssignable(serviceMethod.getReturnType(), PAGE_FQN) && !pageableParams.isEmpty()) {
                    resultVar = querydslCodes.findAllEntities(writer, predicateVar, pageableParams.get(0));
                } else {
                    resultVar = querydslCodes.findOneEntity(writer, predicateVar);
                }
                conversionCodes.convertAndReturn(writer, entity, resultVar, serviceMethod.getReturnType());
            }

            methodBuilder.addCode(writer.toCode());
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
}
