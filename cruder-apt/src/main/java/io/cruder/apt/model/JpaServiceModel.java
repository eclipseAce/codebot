package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;
import io.cruder.apt.util.TypeResolver;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.List;

@Getter
@ToString
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JpaServiceModel {
    private final ModelContext ctx;
    private final JpaEntityModel entity;
    private final List<MethodModel> methods;

    public static JpaServiceModel serviceOf(ModelContext ctx, DeclaredType serviceType) {
        TypeResolver typeResolver = new TypeResolver(serviceType, null);
        return new JpaServiceModel(
                ctx,
                ctx.findAnnotationValue(serviceType.asElement(), "io.cruder.JpaService")
                        .map(it -> JpaEntityModel.entityOf(ctx, (DeclaredType) it.getValue()))
                        .orElseThrow(() -> new IllegalArgumentException("No @JpaService present")),
                ImmutableList.copyOf(MethodModel.methodsOf(ctx, typeResolver, (TypeElement) serviceType.asElement()))
        );
    }

}
