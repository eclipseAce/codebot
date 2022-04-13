package io.cruder.apt.dsl;


import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class MethodDSL extends DSLSupport {
    @Getter
    private final MethodSpec.Builder builder;

    public static MethodDSL method(List<Modifier> modifiers, String name,
                                   @DelegatesTo(MethodDSL.class) Closure<?> cl) {
        MethodDSL dsl = new MethodDSL(MethodSpec.methodBuilder(name)
                .addModifiers(modifiers));
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

    public void parameter(TypeName type, String name,
                          @DelegatesTo(ParameterDSL.class) Closure<?> cl) {
        builder.addParameter(ParameterDSL.parameter(type, name, cl).getBuilder().build());
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
