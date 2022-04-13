package io.cruder.apt.dsl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class FieldDsl extends DslSupport {
    private final FieldSpec.Builder builder;

    public static FieldSpec field(List<Modifier> modifiers, TypeName type, String name,
                                  @DelegatesTo(FieldDsl.class) Closure<?> cl) {
        FieldDsl dsl = new FieldDsl(FieldSpec.builder(type, name)
                .addModifiers(modifiers.toArray(new Modifier[modifiers.size()])));
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