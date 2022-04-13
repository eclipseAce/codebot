package io.cruder.apt.dsl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TypeDsl extends DslSupport {
    private final TypeSpec.Builder builder;

    public static TypeSpec declClass(List<Modifier> modifiers, String name,
                                     @DelegatesTo(TypeDsl.class) Closure<?> cl) {
        TypeDsl dsl = new TypeDsl(TypeSpec.classBuilder(name)
                .addModifiers(modifiers.toArray(new Modifier[modifiers.size()])));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl.builder.build();
    }

    public static TypeSpec declInterface(List<Modifier> modifiers, String name,
                                         @DelegatesTo(TypeDsl.class) Closure<?> cl) {
        TypeDsl dsl = new TypeDsl(TypeSpec.interfaceBuilder(name)
                .addModifiers(modifiers.toArray(new Modifier[modifiers.size()])));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl.builder.build();
    }

    public void modifiers(Modifier... modifiers) {
        builder.addModifiers(modifiers);
    }

    public void superclass(TypeName type) {
        builder.superclass(type);
    }

    public void superinterface(TypeName type) {
        builder.addSuperinterface(type);
    }

    public void superinterfaces(List<? extends TypeName> types) {
        builder.addSuperinterfaces(types);
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

    public void field(List<Modifier> modifiers, TypeName type, String name,
                      @DelegatesTo(FieldDsl.class) Closure<?> cl) {
        builder.addField(FieldDsl.field(modifiers, type, name, cl));
    }

    public void method(List<Modifier> modifiers, String name,
                       @DelegatesTo(MethodDsl.class) Closure<?> cl) {
        builder.addMethod(MethodDsl.method(modifiers, name, cl));
    }
}
