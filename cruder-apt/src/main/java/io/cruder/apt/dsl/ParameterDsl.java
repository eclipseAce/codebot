package io.cruder.apt.dsl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ParameterDsl extends DslSupport {
    private final ParameterSpec.Builder builder;

    public static ParameterSpec parameter(TypeName type, String name,
                                          @DelegatesTo(ParameterDsl.class) Closure<?> cl) {
        ParameterDsl dsl = new ParameterDsl(ParameterSpec.builder(type, name));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl.builder.build();
    }

    public void annotate(ClassName type,
                         @DelegatesTo(AnnotationDsl.class) Closure<?> cl) {
        builder.addAnnotation(AnnotationDsl.annotate(type, cl));
    }

    public void annotate(List<? extends ClassName> types) {
        types.forEach(builder::addAnnotation);
    }

    public void annotate(ClassName type) {
        builder.addAnnotation(type);
    }
}
