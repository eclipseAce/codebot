package io.cruder.autoservice;

import com.squareup.javapoet.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import java.util.Optional;

@RequiredArgsConstructor
public class ServiceImplComponent implements Component {
    private final @Getter ClassName name;
    private final ServiceDescriptor service;

    private RepositoryComponent repository;
    private MapperComponent mapper;

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
            NameAllocator nameAlloc = new NameAllocator();
            method.getParameters().keySet().forEach(nameAlloc::newName);

            switch (method.getMethodKind()) {
                case CREATE:
                    builder.addMethod(implementCreateMethod(method, nameAlloc));
                    break;
                case UPDATE:
                    builder.addMethod(implementUpdateMethod(method, nameAlloc));
                    break;
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

    private MethodSpec implementCreateMethod(MethodDescriptor method, NameAllocator nameAlloc) {
        String entityVar = nameAlloc.newName("entity");

        MethodSpec.Builder builder = MethodSpec.overriding(method.getMethodElement())
                .addAnnotation(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                .addStatement("$1T $2N = new $1T()", service.getEntityClassName(), entityVar);

        builder.addCode(buildMappingCode(method, entityVar));
        builder.addStatement("repository.save($1N)", entityVar);
        builder.addCode(buildReturnCode(method, mapper, nameAlloc, entityVar));
        return builder.build();
    }

    private MethodSpec implementUpdateMethod(MethodDescriptor method, NameAllocator nameAlloc) {
        String entityVar = nameAlloc.newName("entity");

        MethodSpec.Builder builder = MethodSpec.overriding(method.getMethodElement())
                .addAnnotation(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                .addStatement(
                        "$1T $2N = repository.findById($3L).orElseThrow(() -> new $4T($5S))",
                        service.getEntityClassName(),
                        entityVar,
                        buildIdExpression(method),
                        ClassName.get("javax.persistence", "EntityNotFoundException"),
                        service.getEntityClassName().simpleName() + " not exists"
                );

        builder.addCode(buildMappingCode(method, entityVar));
        builder.addStatement("repository.save($1N)", entityVar);
        builder.addCode(buildReturnCode(method, mapper, nameAlloc, entityVar));
        return builder.build();
    }

    private CodeBlock buildIdExpression(MethodDescriptor method) {
        Optional<ParameterDescriptor> idParam = method.findFirstParameter(
                p -> p.couldBeIdentifierOf(service.getEntity()));
        if (idParam.isPresent()) {
            return CodeBlock.of("$1N", idParam.get().getName());
        }
        for (ParameterDescriptor param : method.findParameters(ParameterDescriptor::isDeclaredType)) {
            Optional<PropertyDescriptor> idProp = param.getBean()
                    .findFirstProperty(it -> it.isReadable() && it.couldBeIdentifierOf(service.getEntity()));
            if (idProp.isPresent()) {
                return CodeBlock.of("$1N.$2L()", param.getName(), idProp.get().getGetterName());
            }
        }
        return null;
    }

    private CodeBlock buildMappingCode(MethodDescriptor method, String entityVar) {
        CodeBlock.Builder cb = CodeBlock.builder();
        method.findParameters(ParameterDescriptor::isDeclaredType).forEach(param -> {
            cb.addStatement("mapper.$1L($2L, $3N)",
                    mapper.mapping(param.getParameterElement(), service.getEntity()),
                    param.getName(), entityVar);
        });
        return cb.build();
    }

    private CodeBlock buildReturnCode(MethodDescriptor method, MapperComponent mapper, NameAllocator nameAlloc, String entityVar) {
        CodeBlock.Builder cb = CodeBlock.builder();
        if (method.getResultKind() == MethodDescriptor.ResultKind.IDENTIFIER) {
            if (!service.getEntity().getIdProperty().isReadable()) {
                throw new IllegalArgumentException("Entity ID field is unreadable: " + service.getEntityClassName());
            }
            cb.addStatement("return $1N.$2L()", entityVar, service.getEntity().getIdProperty().getGetterName());
        } //
        else if (method.getResultKind() != MethodDescriptor.ResultKind.NONE) {
            String dtoVar = nameAlloc.newName("dto");
            String mappingFn = mapper.mapping(service.getEntity(), method.getResultElement());
            cb.addStatement("$1T $2N = new $1T()", method.getResultElement(), dtoVar);
            cb.addStatement("mapper.$1L($2N, $3N)", mappingFn, entityVar, dtoVar);
            cb.addStatement("return $1N", dtoVar);
        }
        return cb.build();
    }
}
