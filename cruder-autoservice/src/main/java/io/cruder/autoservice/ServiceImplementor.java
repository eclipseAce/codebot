package io.cruder.autoservice;

import com.google.auto.common.AnnotationMirrors;
import com.squareup.javapoet.*;
import io.cruder.autoservice.annotation.AutoService;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.stream.Collectors;

public class ServiceImplementor {
    private static final ClassName TRANSACTIONAL_NAME = ClassName.get(
            "org.springframework.transaction.annotation", "Transactional"
    );
    private static final ClassName MAPPER_NAME = ClassName.get(
            "org.mapstruct", "Mapper"
    );
    private static final ClassName MAPPING_TARGET_NAME = ClassName.get(
            "org.mapstruct", "MappingTarget"
    );
    private static final ClassName JPA_REPOSITORY_NAME = ClassName.get(
            "org.springframework.data.jpa.repository", "JpaRepository"
    );
    private static final ClassName JPA_SPECIFICATION_EXECUTOR_NAME = ClassName.get(
            "org.springframework.data.jpa.repository", "JpaSpecificationExecutor"
    );
    private static final ClassName AUTOWIRED_NAME = ClassName.get(
            "org.springframework.beans.factory.annotation", "Autowired"
    );
    private static final ClassName ENTITY_NOT_FOUND_EXCEPTION_NAME = ClassName.get(
            "javax.persistence", "EntityNotFoundException"
    );

    private final ProcessingEnvironment processingEnv;
    private final Types types;
    private final Elements elements;

    private final TypeElement serviceElement;

    private ClassName serviceImplName;
    private TypeSpec.Builder serviceImplBuilder;

    private ClassName mapperName;
    private TypeSpec.Builder mapperBuilder;

    private ClassName repositoryName;
    private TypeSpec.Builder repositoryBuilder;

    public ServiceImplementor(ProcessingEnvironment processingEnv, TypeElement serviceElement) {
        this.processingEnv = processingEnv;
        this.types = processingEnv.getTypeUtils();
        this.elements = processingEnv.getElementUtils();
        this.serviceElement = serviceElement;
    }

    public void implement() {
        serviceImplName = getServiceImplName();
        serviceImplBuilder = TypeSpec.classBuilder(serviceImplName)
                .addModifiers(Modifier.PUBLIC);
        if (serviceElement.getKind().isInterface()) {
            serviceImplBuilder.addSuperinterface(serviceElement.asType());
        } else {
            serviceImplBuilder.superclass(serviceElement.asType());
        }

        repositoryName = getRepositoryName();
        repositoryBuilder = TypeSpec.interfaceBuilder(repositoryName)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName
                        .get(JPA_REPOSITORY_NAME, getEntityName(), TypeName.LONG.box()))
                .addSuperinterface(ParameterizedTypeName
                        .get(JPA_SPECIFICATION_EXECUTOR_NAME, getEntityName()));
        serviceImplBuilder.addField(FieldSpec
                .builder(repositoryName, "repository", Modifier.PRIVATE)
                .addAnnotation(AUTOWIRED_NAME)
                .build());

        ElementFilter.methodsIn(serviceElement.getEnclosedElements()).forEach(method -> {
            String methodName = method.getSimpleName().toString();
            if (methodName.startsWith("create")) {
                implementCreateMethod(method);
            } else if (methodName.startsWith("update")) {
                implementUpdateMethod(method);
            } else if (methodName.startsWith("find")) {
                implementQueryMethod(method);
            }
        });
    }

    private void implementCreateMethod(ExecutableElement method) {
        MethodSpec.Builder methodBuilder = MethodSpec.overriding(method)
                .addAnnotation(TRANSACTIONAL_NAME);
        methodBuilder.addStatement("$1T entity = new $1T()", getEntityName());
        if (method.getParameters().size() > 0) {
            VariableElement paramElement = method.getParameters().get(0);
            MethodSpec mapParamToEntityMethod = addMapperMethod(
                    (DeclaredType) paramElement.asType(), getEntityType());
            methodBuilder.addStatement("mapper.$1L($2L, entity)",
                    mapParamToEntityMethod.name, paramElement.getSimpleName());
        }
        methodBuilder.addStatement("repository.save(entity)");

        TypeMirror returnType = method.getReturnType();
        if (returnType.getKind() == TypeKind.LONG) { // TODO: add dynamic id type detection
            methodBuilder.addStatement("return entity.getId()");
        } //
        else if (returnType.getKind() != TypeKind.VOID) {
            MethodSpec mapEntityToReplyMethod = addMapperMethod(
                    getEntityType(), (DeclaredType) returnType);
            methodBuilder
                    .addStatement("$1T reply = new $1T", returnType)
                    .addStatement("mapper.$1L(entity, reply)", mapEntityToReplyMethod.name)
                    .addStatement("return reply");
        }
        serviceImplBuilder.addMethod(methodBuilder.build());
    }

    private void implementUpdateMethod(ExecutableElement method) {
        if (method.getParameters().isEmpty() || method.getReturnType().getKind() != TypeKind.VOID) {
            throw new UnsupportedOperationException("Could not implement method " + method);
        }
        VariableElement paramElement = method.getParameters().get(0);
        MethodSpec.Builder methodBuilder = MethodSpec.overriding(method)
                .addAnnotation(TRANSACTIONAL_NAME);
        methodBuilder.addStatement("$1T entity = repository.findById($2L.getId()).orElseThrow(() -> new $3T($4S))",
                getEntityName(),
                paramElement.getSimpleName(),
                ENTITY_NOT_FOUND_EXCEPTION_NAME,
                getEntityName().simpleName() + " not found");
        MethodSpec mapParamToEntityMethod = addMapperMethod(
                (DeclaredType) paramElement.asType(), getEntityType());
        methodBuilder.addStatement("mapper.$1L($2L, entity)",
                mapParamToEntityMethod.name, paramElement.getSimpleName());
        methodBuilder.addStatement("repository.save(entity)");
        serviceImplBuilder.addMethod(methodBuilder.build());
    }

    private void implementQueryMethod(ExecutableElement method) {
        TypeMirror returnType = method.getReturnType();
        if (returnType.getKind() != TypeKind.DECLARED) {
            throw new UnsupportedOperationException("Could not implement method " + method);
        }
        if (method.getParameters().size() == 1
                && method.getParameters().get(0).asType().getKind() == TypeKind.LONG) {
            VariableElement idParam = method.getParameters().get(0);
            DeclaredType resultType = (DeclaredType) returnType;
            MethodSpec mapEntityToResultMethod = addMapperMethod(getEntityType(), resultType);
            MethodSpec.Builder methodBuilder = MethodSpec.overriding(method)
                    .addStatement("$1T entity = repository.findById($2L).orElseThrow(() -> new $3T($4S))",
                            getEntityType(),
                            idParam.getSimpleName(),
                            ENTITY_NOT_FOUND_EXCEPTION_NAME,
                            getEntityName().simpleName() + " not found")
                    .addStatement("$1T dto = new $1T()", resultType)
                    .addStatement("mapper.$1L(entity, dto)", mapEntityToResultMethod.name)
                    .addStatement("return dto");
            serviceImplBuilder.addMethod(methodBuilder.build());
        } else {
            boolean returnsPage = types.isSameType(
                    (((DeclaredType) returnType).asElement()).asType(),
                    elements.getTypeElement("org.springframework.data.domain.Page").asType());
            VariableElement pageableParam = method.getParameters().stream()
                    .filter(it -> types.isAssignable(it.asType(),
                            elements.getTypeElement("org.springframework.data.domain.Pageable").asType()))
                    .findFirst().orElse(null);
            VariableElement specParam = method.getParameters().stream()
                    .filter(it -> types.isAssignable(it.asType(),
                            types.getDeclaredType(
                                    elements.getTypeElement("org.springframework.data.jpa.domain.Specification"),
                                    getEntityType()
                            )))
                    .findFirst().orElse(null);
            if (returnsPage && pageableParam != null) {
                DeclaredType resultType = (DeclaredType) ((DeclaredType) returnType).getTypeArguments().get(0);
                MethodSpec mapEntityToResultMethod = addMapperMethod(getEntityType(), resultType);
                MethodSpec.Builder methodBuilder = MethodSpec.overriding(method)
                        .addStatement("Page<$1T> result = repository.findAll($2L, $3L)",
                                getEntityType(),
                                specParam != null ? specParam.getSimpleName() : "null",
                                pageableParam.getSimpleName())
                        .addCode("return result.map(it -> {$>")
                        .addStatement("$1T dto = new $1T()", resultType)
                        .addStatement("mapper.$1L(it, dto)", mapEntityToResultMethod.name)
                        .addStatement("return dto")
                        .addCode("$<});");
                serviceImplBuilder.addMethod(methodBuilder.build());
            }
        }
    }

    public void writeTo(Filer filer) throws IOException {
        if (serviceImplBuilder != null) {
            JavaFile.builder(serviceImplName.packageName(), serviceImplBuilder.build()).build().writeTo(filer);
        }
        if (mapperBuilder != null) {
            JavaFile.builder(mapperName.packageName(), mapperBuilder.build()).build().writeTo(filer);
        }
        if (repositoryBuilder != null) {
            JavaFile.builder(repositoryName.packageName(), repositoryBuilder.build()).build().writeTo(filer);
        }
    }

    protected MethodSpec addMapperMethod(DeclaredType from, DeclaredType to) {
        String methodName = String.format("map%sTo%s",
                from.asElement().getSimpleName(), to.asElement().getSimpleName());
        MethodSpec method = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(TypeName.get(from), "from")
                .addParameter(ParameterSpec.builder(TypeName.get(to), "to")
                        .addAnnotation(MAPPING_TARGET_NAME)
                        .build())
                .build();
        if (mapperBuilder == null) {
            mapperName = getMapperName();
            mapperBuilder = TypeSpec.interfaceBuilder(mapperName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(MAPPER_NAME)
                            .addMember("componentModel", "$S", "spring")
                            .build());
            serviceImplBuilder.addField(FieldSpec
                    .builder(mapperName, "mapper", Modifier.PRIVATE)
                    .addAnnotation(AUTOWIRED_NAME)
                    .build());
        }
        mapperBuilder.addMethod(method);
        return method;
    }

    protected String getRootPackageName() {
        String packageName = ((PackageElement) serviceElement.getEnclosingElement())
                .getQualifiedName().toString();
        return packageName.substring(0, packageName.lastIndexOf('.'));
    }

    protected DeclaredType getEntityType() {
        return (DeclaredType) serviceElement.getAnnotationMirrors().stream()
                .filter(it -> ((TypeElement) it.getAnnotationType().asElement())
                        .getQualifiedName().contentEquals(AutoService.class.getName()))
                .flatMap(it -> it.getElementValues().entrySet().stream())
                .collect(Collectors.toMap(it -> it.getKey().getSimpleName().toString(), it -> it.getValue()))
                .get("value").getValue();
    }

    protected ClassName getEntityName() {
        return ClassName.get((TypeElement) getEntityType().asElement());
    }

    protected ClassName getServiceImplName() {
        return ClassName.bestGuess(serviceElement.getQualifiedName() + "Impl");
    }

    protected ClassName getRepositoryName() {
        return ClassName.get(getRootPackageName() + ".repository",
                getEntityName().simpleName() + "Repository");
    }

    protected ClassName getMapperName() {
        return ClassName.get(getRootPackageName() + ".mapper",
                serviceElement.getSimpleName() + "Mapper");
    }
}
