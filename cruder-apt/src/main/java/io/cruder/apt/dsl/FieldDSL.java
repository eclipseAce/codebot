package io.cruder.apt.dsl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class FieldDSL extends DSLSupport {
    @Getter
    private final FieldSpec.Builder builder;

    public static FieldDSL field(List<Modifier> modifiers, TypeName type, String name,
                                 @DelegatesTo(FieldDSL.class) Closure<?> cl) {
        FieldDSL dsl = new FieldDSL(FieldSpec.builder(type, name)
                .addModifiers(modifiers.toArray(new Modifier[modifiers.size()])));
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
