package io.cruder.autoservice;

import com.squareup.javapoet.*;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeKind;
import java.io.IOException;

public class ServiceImplementor {
    private final ProcessingContext ctx;
    private final ServiceDescriptor service;
    private final TypeSpec.Builder builder;

    private final MapperComponent mapperComponent;
    private final RepositoryComponent repositoryComponent;

    public ServiceImplementor(ProcessingContext ctx, ServiceDescriptor service) throws IOException {
        this.ctx = ctx;
        this.service = service;
        this.builder = createServiceImplBuilder();
        this.mapperComponent = MapperComponent.forService(ctx, service);
        this.repositoryComponent = RepositoryComponent.forEntity(ctx, service.getEntity());

        for (MethodDescriptor method : service.getMethods()) {
            switch (method.getMethodKind()) {
                case CREATE:
                    this.builder.addMethod(implementCreateMethod(method));
                    break;
                case UPDATE:
                case QUERY:
                case UNKNOWN:
            }
        }

        if (mapperComponent.isNecessary()) {
            JavaFile mapper = mapperComponent.createComponent();
            builder.addField(FieldSpec
                    .builder(mapperComponent.getName(), "mapper", Modifier.PRIVATE, Modifier.FINAL)
                    .initializer("$1T.getMapper($2T.class)",
                            ClassName.get("org.mapstruct.factory", "Mappers"),
                            mapperComponent.getName())
                    .build());
            mapper.writeTo(ctx.filer);
        }

        if (repositoryComponent.isNecessary()) {
            JavaFile repository = repositoryComponent.createComponent();
            builder.addField(FieldSpec
                    .builder(repositoryComponent.getName(), "repository", Modifier.PRIVATE)
                    .addAnnotation(ClassName.get("org.springframework.beans.factory.annotation", "Autowired"))
                    .build());
            repository.writeTo(ctx.filer);
        }

        JavaFile.builder(getServiceImplName().packageName(), builder.build())
                .build().writeTo(ctx.filer);
    }

    private MethodSpec implementCreateMethod(MethodDescriptor method) {
        NameAllocator nameAlloc = method.newNameAllocator();
        String entityVar = nameAlloc.newName("entity");

        MethodSpec.Builder builder = MethodSpec.overriding(method.getMethodElement())
                .addAnnotation(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                .addStatement("$1T $2N = new $1T()", service.getEntity().getEntityElement(), entityVar);

        method.getUnrecognizedParameterElements().entrySet().stream()
                .filter(entry -> entry.getValue().asType().getKind() == TypeKind.DECLARED)
                .forEach(entry -> {
                    builder.addStatement("mapper.$1L($2L, $3N)",
                            mapperComponent.mapping(entry.getValue(), service.getEntity()),
                            entry.getKey(),
                            entityVar);
                });

        builder.addStatement("repository.save($1N)", entityVar);
        if (method.getResultKind() == MethodDescriptor.ResultKind.IDENTIFIER) {
            if (!service.getEntity().getIdField().isReadable()) {
                throw new IllegalArgumentException("Entity ID field is unreadable: "
                        + service.getEntity().getEntityElement());
            }
            builder.addStatement("return $1N.$2L()", entityVar, service.getEntity().getIdField().getGetterName());
        } //
        else if (method.getResultKind() == MethodDescriptor.ResultKind.DATA_OBJECT) {
            String dtoVar = nameAlloc.newName("dto");
            String mappingFn = mapperComponent.mapping(service.getEntity(), method.getResultElement());
            builder.addStatement("$1T $2N = new $1T()", method.getResultElement(), dtoVar);
            builder.addStatement("mapper.$1L($2N, $3N)", mappingFn, entityVar, dtoVar);
            builder.addStatement("return $1N", dtoVar);
        }
        return builder.build();
    }

    private String getServicePackageQualifiedName() {
        return ((PackageElement) service.getServiceElement().getEnclosingElement())
                .getQualifiedName().toString();
    }

    private ClassName getServiceImplName() {
        return ClassName.get(getServicePackageQualifiedName(),
                service.getServiceElement().getSimpleName().toString() + "Impl");
    }

    private ClassName getServiceName() {
        return ClassName.get(service.getServiceElement());
    }

    private TypeSpec.Builder createServiceImplBuilder() {
        ClassName serviceName = getServiceName();
        TypeSpec.Builder builder = TypeSpec.classBuilder(getServiceImplName())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        if (service.getServiceElement().getKind() == ElementKind.INTERFACE) {
            builder.addSuperinterface(serviceName);
        } else if (service.getServiceElement().getKind() == ElementKind.CLASS) {
            builder.superclass(serviceName);
        }
        return builder;
    }


}
