package io.codebot.apt.model;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.codebot.apt.type.Type;
import io.codebot.apt.util.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
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
    private final Entity entity;
    private final List<Method> abstractMethods;

    private final List<MethodProcessor> methodProcessors;

    public Service(Type type, List<MethodProcessor> methodProcessors) {
        AnnotationMirror annotation = AnnotationUtils.find(type.asTypeElement(), CRUD_SERVICE_FQN)
                .orElseThrow(() -> new IllegalArgumentException("No @CrudService present"));

        this.type = type;
        this.typeName = ClassName.get(type.asTypeElement());
        this.entity = new Entity(type.factory().getType(AnnotationUtils.<TypeMirror>findValue(annotation).get()));
        this.abstractMethods = type.methods().stream()
                .filter(method -> method.getModifiers().contains(Modifier.ABSTRACT))
                .map(method -> new Method(type, method))
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
                                entity.getTypeName(),
                                entity.getIdTypeName().box()
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
                                entity.getTypeName()
                        ),
                        "specificationExecutor",
                        Modifier.PRIVATE
                )
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build());
        for (Method abstractMethod : abstractMethods) {
            MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                    abstractMethod.getExecutableElement(),
                    type.asDeclaredType(),
                    type.factory().typeUtils()
            );
            NameAllocator nameAlloc = new NameAllocator();
            abstractMethod.getParameters().forEach(p -> nameAlloc.newName(p.getName()));

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

    public Entity getEntity() {
        return entity;
    }
}
