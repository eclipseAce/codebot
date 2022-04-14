package io.cruder.apt.dsl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ParameterDSL extends DSLSupport {
    private final ParameterSpec.Builder builder;

    public static ParameterDSL parameter(TypeName typeName, String name,
                                         @DelegatesTo(ParameterDSL.class) Closure<?> cl) {
        ParameterDSL dsl = new ParameterDSL(ParameterSpec.builder(typeName, name));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public ParameterDSL annotate(ClassName typeName,
                                 @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        builder.addAnnotation(AnnotationDSL.annotate(typeName, cl).build());
        return this;
    }

    public ParameterDSL annotate(Iterable<ClassName> typeNames) {
        typeNames.forEach(builder::addAnnotation);
        return this;
    }

    public ParameterDSL annotate(ClassName typeNames) {
        builder.addAnnotation(typeNames);
        return this;
    }

    public ParameterSpec build() {
        return builder.build();
    }
}
