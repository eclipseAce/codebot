package io.cruder.autoservice.metadata;

import io.cruder.autoservice.annotation.AutoService;
import io.cruder.autoservice.util.Models;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ServiceDescriptor {
    private TypeElement serviceElement;
    private EntityDescriptor entity;
    private List<MethodDescriptor> methods;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }

    public static ServiceDescriptor of(Models models, TypeElement service) {
        ServiceDescriptor info = new ServiceDescriptor();
        info.serviceElement = service;

        AnnotationMirror anno = models.findAnnotation(service, AutoService.class.getName())
                .orElseThrow(() -> new IllegalArgumentException("No @AutoService present"));
        TypeMirror type = models.findClassAnnotationValue(anno, "value")
                .filter(it -> it.getKind() == TypeKind.DECLARED)
                .orElseThrow(() -> new IllegalArgumentException("Not DeclaredType for entity"));
        info.entity = EntityDescriptor.of(models, models.asTypeElement(type));

        info.methods = ElementFilter.methodsIn(service.getEnclosedElements()).stream()
                .map(method -> MethodDescriptor.of(models, info, method))
                .collect(Collectors.toList());
        return info;
    }
}
