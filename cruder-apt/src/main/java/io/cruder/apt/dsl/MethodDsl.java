package io.cruder.apt.dsl;


import com.squareup.javapoet.MethodSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MethodDsl {
    private final MethodSpec.Builder builder;

    public static MethodSpec methodDecl(List<Modifier> modifiers, String name, @DelegatesTo(MethodDsl.class) Closure<?> cl) {
        MethodDsl dsl = new MethodDsl(MethodSpec.methodBuilder(name).addModifiers(modifiers));
        cl.setDelegate(dsl);
        cl.call();
        return dsl.builder.build();
    }
}
