package io.cruder.autoservice;

import com.squareup.javapoet.ClassName;
import io.cruder.autoservice.annotation.AutoService;
import lombok.Getter;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceDescriptor {
    private final @Getter TypeElement serviceElement;
    private final @Getter EntityDescriptor entity;
    private final @Getter List<MethodDescriptor> methods;

    public ServiceDescriptor(ProcessingContext ctx, TypeElement service) {
        this.serviceElement = service;

        AnnotationMirror anno = ctx.utils.findAnnotation(service, AutoService.class.getName())
                .orElseThrow(() -> new IllegalArgumentException("No @AutoService present"));
        TypeMirror type = ctx.utils.findClassAnnotationValue(anno, "value")
                .filter(it -> it.getKind() == TypeKind.DECLARED)
                .orElseThrow(() -> new IllegalArgumentException("Not DeclaredType for entity"));
        this.entity = new EntityDescriptor(ctx, ctx.utils.asTypeElement(type));

        this.methods = ElementFilter.methodsIn(service.getEnclosedElements()).stream()
                .map(method -> new MethodDescriptor(ctx, this, method))
                .collect(Collectors.toList());
    }

    public ClassName getName() {
        return ClassName.get(serviceElement);
    }

    public ClassName getEntityClassName() {
        return entity.getClassName();
    }
}
