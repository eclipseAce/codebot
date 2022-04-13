package io.cruder.apt.dsl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class ParameterDSL extends DSLSupport {
    @Getter
    private final ParameterSpec.Builder builder;

    public static ParameterDSL parameter(TypeName type, String name,
                                         @DelegatesTo(ParameterDSL.class) Closure<?> cl) {
        ParameterDSL dsl = new ParameterDSL(ParameterSpec.builder(type, name));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public void annotate(ClassName type,
                         @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        builder.addAnnotation(AnnotationDSL.annotate(type, cl).getBuilder().build());
    }

    public void annotate(List<? extends ClassName> types) {
        types.forEach(builder::addAnnotation);
    }

    public void annotate(ClassName type) {
        builder.addAnnotation(type);
    }
}
