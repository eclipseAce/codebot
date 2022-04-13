package io.cruder.apt.dsl;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnotationDSL extends DSLSupport {
    @Getter
    private final AnnotationSpec.Builder builder;

    public static AnnotationDSL annotate(ClassName type,
                                         @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        AnnotationDSL dsl = new AnnotationDSL(AnnotationSpec.builder(type));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public void member(String name, String format, Object... args) {
        builder.addMember(name, format, args);
    }
}
