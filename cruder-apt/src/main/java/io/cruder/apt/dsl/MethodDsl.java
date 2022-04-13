package io.cruder.apt.dsl;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MethodDsl extends DslSupport {
    private final MethodSpec.Builder builder;

    public static MethodSpec method(List<Modifier> modifiers, String name,
                                    @DelegatesTo(MethodDsl.class) Closure<?> cl) {
        MethodDsl dsl = new MethodDsl(MethodSpec.methodBuilder(name)
                .addModifiers(modifiers));
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

    public void parameter(TypeName type, String name,
                          @DelegatesTo(ParameterDsl.class) Closure<?> cl) {
        builder.addParameter(ParameterDsl.parameter(type, name, cl));
    }

    public void parameter(TypeName type, String name) {
        builder.addParameter(type, name);
    }

    public void returns(TypeName type) {
        builder.returns(type);
    }

    public void body(String format, Object... args) {
        builder.addCode(format, args);
    }
}
