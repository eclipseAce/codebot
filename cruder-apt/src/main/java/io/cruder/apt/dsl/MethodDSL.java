package io.cruder.apt.dsl;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MethodDSL extends DSLSupport {
    private final MethodSpec.Builder builder;

    public static MethodDSL method(Iterable<Modifier> modifiers, String name,
                                   @DelegatesTo(MethodDSL.class) Closure<?> cl) {
        MethodDSL dsl = new MethodDSL(MethodSpec.methodBuilder(name)
                .addModifiers(modifiers));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public MethodDSL annotate(ClassName typeName,
                              @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        builder.addAnnotation(AnnotationDSL.annotate(typeName, cl).build());
        return this;
    }

    public MethodDSL annotate(Iterable<ClassName> typeNames) {
        typeNames.forEach(builder::addAnnotation);
        return this;
    }

    public MethodDSL annotate(ClassName typeName) {
        builder.addAnnotation(typeName);
        return this;
    }

    public MethodDSL parameter(TypeName typeName, String name,
                               @DelegatesTo(ParameterDSL.class) Closure<?> cl) {
        builder.addParameter(ParameterDSL.parameter(typeName, name, cl).build());
        return this;
    }

    public MethodDSL parameter(TypeName typeName, String name) {
        builder.addParameter(typeName, name);
        return this;
    }

    public MethodDSL returns(TypeName typeName) {
        builder.returns(typeName);
        return this;
    }

    public MethodDSL body(String format, Object... args) {
        builder.addCode(format, args);
        return this;
    }

    public MethodSpec build() {
        return builder.build();
    }
}
