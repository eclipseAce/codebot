package io.cruder.autoservice;

import com.google.common.collect.Sets;
import com.squareup.javapoet.*;
import lombok.Value;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceImplementor {
    private final Context ctx;
    private final ServiceDescriptor service;
    private final TypeSpec.Builder builder;

    private final Set<MapperMethod> mapperMethods = Sets.newLinkedHashSet();

    public ServiceImplementor(Context ctx, ServiceDescriptor service) throws IOException {
        this.ctx = ctx;
        this.service = service;
        this.builder = createServiceImplBuilder();

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

        if (!mapperMethods.isEmpty()) {
            ClassName mapperName = ClassName.get(
                    getServicePackageQualifiedName(), getServiceName().simpleName() + "Mapper");
            TypeSpec mapper = TypeSpec.interfaceBuilder(mapperName)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec
                            .builder(ClassName.get("org.mapstruct", "Mapper"))
                            .addMember("componentModel", "$S", "spring")
                            .build())
                    .addMethods(mapperMethods.stream()
                            .map(method -> MethodSpec
                                    .methodBuilder(method.getMethodName())
                                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                    .addParameter(method.getFrom(), "from")
                                    .addParameter(ParameterSpec.builder(method.getTo(), "to")
                                            .addAnnotation(ClassName.get("org.mapstruct", "MappingTarget"))
                                            .build())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
            JavaFile.builder(mapperName.packageName(), mapper).build().writeTo(ctx.filer);
            this.builder.addField(FieldSpec
                    .builder(mapperName, "mapper", Modifier.PRIVATE)
                    .addAnnotation(ClassName.get("org.springframework.beans.factory.annotation", "Autowired"))
                    .build());
        }

        JavaFile.builder(getServiceImplName().packageName(), builder.build()).build().writeTo(ctx.filer);
    }

    private MethodSpec implementCreateMethod(MethodDescriptor method) {
        MethodSpec.Builder builder = MethodSpec.overriding(method.getMethodElement())
                .addAnnotation(ClassName.get("org.springframework.transaction.annotation", "Transactional"))
                .addStatement("$1T entity = new $1T()", service.getEntity().getEntityElement());
        method.getUnrecognizedParameterElements().entrySet().stream()
                .filter(entry -> entry.getValue().asType().getKind() == TypeKind.DECLARED)
                .forEach(entry -> {
                    builder.addStatement("mapper.$1L($2L, entity)",
                            getMapperMethod(entry.getValue(), service.getEntity().getEntityElement()).getMethodName(),
                            entry.getKey());
                });
        builder.addStatement("repository.save(entity)");
        if (method.getResultKind() == MethodDescriptor.ResultKind.IDENTIFIER) {
            if (!service.getEntity().getIdField().isReadable()) {
                throw new IllegalArgumentException("Entity ID field is unreadable: "
                        + service.getEntity().getEntityElement());
            }
            builder.addStatement("return entity.$1L()", service.getEntity().getIdField().getGetterName());
        } //
        else if (method.getResultKind() == MethodDescriptor.ResultKind.DATA_OBJECT) {
            builder.addStatement("$1T dto = new $1T()", method.getResultElement());
            builder.addStatement("mapper.$1L(entity, dto)",
                    getMapperMethod(method.getResultElement(), service.getEntity().getEntityElement()));
            builder.addStatement("return dto");
        }
        return builder.build();
    }

    private MapperMethod getMapperMethod(ClassName from, ClassName to) {
        MapperMethod method = new MapperMethod(from, to);
        mapperMethods.add(method);
        return method;
    }

    private MapperMethod getMapperMethod(TypeElement from, TypeElement to) {
        return getMapperMethod(ClassName.get(from), ClassName.get(to));
    }

    private MapperMethod getMapperMethod(VariableElement from, TypeElement to) {
        return getMapperMethod(ClassName.get(ctx.asTypeElement(from.asType())), ClassName.get(to));
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

    @Value
    private static class MapperMethod {
        ClassName from;
        ClassName to;

        public String getMethodName() {
            return String.format("map%sTo%s", from.simpleName(), to.simpleName());
        }
    }
}
