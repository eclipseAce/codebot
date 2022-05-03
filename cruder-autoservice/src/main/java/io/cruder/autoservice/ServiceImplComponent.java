package io.cruder.autoservice;

import com.squareup.javapoet.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;

@RequiredArgsConstructor
public class ServiceImplComponent implements Component {
    private final @Getter ClassName name;
    private final ServiceDescriptor service;

    private RepositoryComponent repository;
    private ServiceMapperComponent mapper;

    @Override
    public void init(ProcessingContext ctx) {
        this.repository = ctx.getRepositoryComponent(service.getEntity());
        this.mapper = ctx.getServiceMapperComponent(service);
    }

    @Override
    public JavaFile createJavaFile() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        if (service.getServiceElement().getKind() == ElementKind.INTERFACE) {
            builder.addSuperinterface(ClassName.get(service.getServiceElement()));
        } else {
            builder.superclass(ClassName.get(service.getServiceElement()));
        }

        for (MethodDescriptor method : service.getMethods()) {
            switch (method.getMethodKind()) {
                case CREATE:
                    builder.addMethod(implementCreateMethod(method, mapper, repository));
                    break;
                case UPDATE:
                case QUERY:
                case UNKNOWN:
            }
        }
        builder.addField(FieldSpec
                .builder(mapper.getName(), "mapper", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("$1T.getMapper($2T.class)",
                        ClassName.get("org.mapstruct.factory", "Mappers"),
                        mapper.getName())
                .build());
        builder.addField(FieldSpec
                .builder(repository.getName(), "repository", Modifier.PRIVATE)
                .addAnnotation(ClassName.get("org.springframework.beans.factory.annotation", "Autowired"))
                .build());
        return JavaFile.builder(name.packageName(), builder.build()).build();
    }

    private MethodSpec implementCreateMethod(MethodDescriptor method,
                                             ServiceMapperComponent mapper,
                                             RepositoryComponent repository) {
        NameAllocator nameAlloc = method.newNameAllocator();
        String entityVar = nameAlloc.newName("entity");

        MethodSpec.Builder builder = MethodSpec.overriding(method.getMethodElement())
                .addAnnotation(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                .addStatement("$1T $2N = new $1T()", service.getEntity().getEntityElement(), entityVar);

        method.getUnrecognizedParameterElements().entrySet().stream()
                .filter(entry -> entry.getValue().asType().getKind() == TypeKind.DECLARED)
                .forEach(entry -> {
                    builder.addStatement("mapper.$1L($2L, $3N)",
                            mapper.mapping(entry.getValue(), service.getEntity()),
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
            String mappingFn = mapper.mapping(service.getEntity(), method.getResultElement());
            builder.addStatement("$1T $2N = new $1T()", method.getResultElement(), dtoVar);
            builder.addStatement("mapper.$1L($2N, $3N)", mappingFn, entityVar, dtoVar);
            builder.addStatement("return $1N", dtoVar);
        }
        return builder.build();
    }
}
