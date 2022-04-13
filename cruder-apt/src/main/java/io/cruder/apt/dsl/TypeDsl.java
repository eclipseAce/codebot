package io.cruder.apt.dsl;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TypeDsl {
    private final TypeSpec.Builder builder;

    public static TypeSpec classDecl(List<Modifier> modifiers, String name, @DelegatesTo(TypeDsl.class) Closure<?> cl) {
        TypeDsl dsl = new TypeDsl(TypeSpec.classBuilder(name).addModifiers(modifiers.toArray(new Modifier[modifiers.size()])));
        cl.setDelegate(dsl);
        cl.call();
        return dsl.builder.build();
    }

    public void modifiers(Modifier... modifiers) {
        builder.addModifiers(modifiers);
    }

    public void field(TypeName type, String name, @DelegatesTo(FieldDsl.class) Closure<?> cl) {
        builder.addField(FieldDsl.decl(type, name, cl));
    }

    public void method(List<Modifier> modifiers, String name, @DelegatesTo(MethodSpec.class) Closure<?> cl) {
        builder.addMethod(MethodDsl.methodDecl(modifiers, name, cl));
    }
}
