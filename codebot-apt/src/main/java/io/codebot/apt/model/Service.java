package io.codebot.apt.model;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.codebot.apt.type.*;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class Service {
    private static final String CRUD_SERVICE_FQN = "io.codebot.CrudService";
    private static final String AUTOWIRED_FQN
            = "org.springframework.beans.factory.annotation.Autowired";
    private static final String JPA_REPOSITORY_FQN
            = "org.springframework.data.jpa.repository.JpaRepository";
    private static final String JPA_SPECIFICATION_EXECUTOR_FQN
            = "org.springframework.data.jpa.repository.JpaSpecificationExecutor";

    private final Type type;
    private final ClassName typeName;

    private final Type entityType;
    private final ClassName entityTypeName;
    private final Type entityIdType;
    private final TypeName entityIdTypeName;
    private final String entityIdName;
    private final GetAccessor entityIdGetter;

    private final List<Executable> abstractMethods;
    private final List<MethodProcessor> methodProcessors;

    public Service(Type type, List<MethodProcessor> methodProcessors) {
        Annotation annotation = type.findAnnotation(CRUD_SERVICE_FQN)
                .orElseThrow(() -> new IllegalArgumentException("No @CrudService present"));

        this.type = type;
        this.typeName = ClassName.get(type.asTypeElement());

        Variable entityIdField = type.fields().stream()
                .filter(it -> it.isAnnotationPresent("javax.persistence.Id"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't determin ID of entity " + type));

        this.entityType = type;
        this.entityTypeName = ClassName.get(type.asTypeElement());
        this.entityIdType = entityIdField.type();
        this.entityIdTypeName = TypeName.get(entityIdType.typeMirror());
        this.entityIdName = entityIdField.simpleName();
        this.entityIdGetter = type.findGetter(entityIdName, entityIdType).orElse(null);

        this.abstractMethods = type.methods().stream()
                .filter(method -> method.hasModifiers(Modifier.ABSTRACT))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));
        this.methodProcessors = ImmutableList.copyOf(methodProcessors);
    }

    public JavaFile implement() {
        ClassName implTypeName = ClassName.get(typeName.packageName(), typeName.simpleName() + "Impl");
        TypeSpec.Builder serviceBuilder = TypeSpec.classBuilder(implTypeName)
                .addModifiers(Modifier.PUBLIC);
        if (type.isInterface()) {
            serviceBuilder.addSuperinterface(typeName);
        } else {
            serviceBuilder.superclass(typeName);
        }
        serviceBuilder.addField(FieldSpec
                .builder(
                        ParameterizedTypeName.get(
                                ClassName.bestGuess(JPA_REPOSITORY_FQN),
                                entityTypeName,
                                entityIdTypeName.box()
                        ),
                        "repository",
                        Modifier.PRIVATE
                )
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build());
        serviceBuilder.addField(FieldSpec
                .builder(
                        ParameterizedTypeName.get(
                                ClassName.bestGuess(JPA_SPECIFICATION_EXECUTOR_FQN),
                                entityTypeName
                        ),
                        "specificationExecutor",
                        Modifier.PRIVATE
                )
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build());
        for (Executable abstractMethod : abstractMethods) {
            MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                    abstractMethod.element(),
                    type.asDeclaredType(),
                    type.factory().typeUtils()
            );
            NameAllocator nameAlloc = new NameAllocator();
            abstractMethod.parameters().forEach(p -> nameAlloc.newName(p.simpleName()));

            for (MethodProcessor processor : methodProcessors) {
                processor.process(this, serviceBuilder, abstractMethod, methodBuilder, nameAlloc);
            }

            serviceBuilder.addMethod(methodBuilder.build());
        }
        return JavaFile.builder(implTypeName.packageName(), serviceBuilder.build()).build();
    }

    public Type getType() {
        return type;
    }

    public ClassName getTypeName() {
        return typeName;
    }

    public Type getEntityType() {
        return entityType;
    }

    public ClassName getEntityTypeName() {
        return entityTypeName;
    }

    public Type getEntityIdType() {
        return entityIdType;
    }

    public TypeName getEntityIdTypeName() {
        return entityIdTypeName;
    }

    public String getEntityIdName() {
        return entityIdName;
    }

    public GetAccessor getEntityIdGetter() {
        return entityIdGetter;
    }
}
