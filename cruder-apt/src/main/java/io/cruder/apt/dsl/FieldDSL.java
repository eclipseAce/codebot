package io.cruder.apt.dsl;

import com.google.common.collect.Iterables;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldDSL extends DSLSupport {
    private final FieldSpec.Builder builder;

    public static FieldDSL field(Iterable<Modifier> modifiers, TypeName typeName, String name,
                                 @DelegatesTo(FieldDSL.class) Closure<?> cl) {
        FieldDSL dsl = new FieldDSL(FieldSpec.builder(typeName, name)
                .addModifiers(Iterables.toArray(modifiers, Modifier.class)));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public FieldDSL annotate(ClassName typeName,
                             @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        builder.addAnnotation(AnnotationDSL.annotate(typeName, cl).build());
        return this;
    }

    public FieldDSL annotate(Iterable<ClassName> types) {
        types.forEach(builder::addAnnotation);
        return this;
    }

    public FieldDSL annotate(ClassName type) {
        builder.addAnnotation(type);
        return this;
    }

    public FieldSpec build() {
        return this.builder.build();
    }
}
