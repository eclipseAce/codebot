package io.cruder.apt.dsl;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnotationDSL extends DSLSupport {
    private final AnnotationSpec.Builder builder;

    public static AnnotationDSL annotate(ClassName typeName,
                                         @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        AnnotationDSL dsl = new AnnotationDSL(AnnotationSpec.builder(typeName));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public AnnotationDSL member(String name, String format, Object... args) {
        builder.addMember(name, format, args);
        return this;
    }

    public AnnotationSpec build() {
        return builder.build();
    }
}
