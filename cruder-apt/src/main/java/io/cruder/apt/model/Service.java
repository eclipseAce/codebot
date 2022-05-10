package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import io.cruder.apt.type.Type;
import io.cruder.apt.util.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

public class Service {
    private static final String CRUD_SERVICE_FQN = "io.cruder.CrudService";
    private static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";

    private final Type type;
    private final ClassName typeName;
    private final Entity entity;
    private final Repository repository;
    private final List<Method> abstractMethods;

    private final List<MethodImplementor> methodImplementors;

    public Service(Type type, List<MethodImplementor> methodImplementors) {
        AnnotationMirror annotation = AnnotationUtils.find(type.asTypeElement(), CRUD_SERVICE_FQN)
                .orElseThrow(() -> new IllegalArgumentException("No @CrudService present"));

        this.type = type;
        this.typeName = ClassName.get(type.asTypeElement());
        this.entity = new Entity(type.getFactory().getType(
                AnnotationUtils.<TypeMirror>findValue(annotation, "entity").get()
        ));
        this.repository = new Repository(type.getFactory().getType(
                AnnotationUtils.<TypeMirror>findValue(annotation, "repository").get()
        ));
        this.abstractMethods = type.getMethods().stream()
                .filter(method -> method.getModifiers().contains(Modifier.ABSTRACT))
                .map(method -> new Method(type, method))
                .collect(Collectors.collectingAndThen(Collectors.toList(), ImmutableList::copyOf));

        this.methodImplementors = ImmutableList.copyOf(methodImplementors);
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
                .builder(repository.getTypeName(), "repository", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build());
        for (Method abstractMethod : abstractMethods) {
            MethodSpec.Builder methodBuilder = MethodSpec.overriding(
                    abstractMethod.getExecutableElement(),
                    type.asDeclaredType(),
                    type.getFactory().getTypeUtils()
            );
            NameAllocator nameAlloc = new NameAllocator();
            abstractMethod.getParameters().forEach(p -> nameAlloc.newName(p.getName()));

            for (MethodImplementor implementor : methodImplementors) {
                implementor.implement(this, serviceBuilder, abstractMethod, methodBuilder, nameAlloc);
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

    public Repository getRepository() {
        return repository;
    }
}
