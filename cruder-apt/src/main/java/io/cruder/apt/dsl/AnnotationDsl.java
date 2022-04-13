package io.cruder.apt.dsl;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AnnotationDsl extends DslSupport {
    private final AnnotationSpec.Builder builder;

    public static AnnotationSpec annotate(ClassName type,
                                          @DelegatesTo(AnnotationDsl.class) Closure<?> cl) {
        AnnotationDsl dsl = new AnnotationDsl(AnnotationSpec.builder(type));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl.builder.build();
    }

    public void member(String name, String format, Object... args) {
        builder.addMember(name, format, args);
    }
}
