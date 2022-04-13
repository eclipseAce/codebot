package io.cruder.apt.dsl;

import com.squareup.javapoet.*;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TypeDSL extends DSLSupport {
    @Getter
    private final TypeSpec.Builder builder;

    public static TypeDSL declClass(List<Modifier> modifiers, String name,
                                    @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        TypeDSL dsl = new TypeDSL(TypeSpec.classBuilder(name)
                .addModifiers(modifiers.toArray(new Modifier[modifiers.size()])));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public static TypeDSL declInterface(List<Modifier> modifiers, String name,
                                        @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        TypeDSL dsl = new TypeDSL(TypeSpec.interfaceBuilder(name)
                .addModifiers(modifiers.toArray(new Modifier[modifiers.size()])));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
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
                         @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        builder.addAnnotation(AnnotationDSL.annotate(type, cl).getBuilder().build());
    }

    public void annotate(List<? extends ClassName> types) {
        types.forEach(builder::addAnnotation);
    }

    public void annotate(ClassName type) {
        builder.addAnnotation(type);
    }

    public void field(List<Modifier> modifiers, TypeName type, String name,
                      @DelegatesTo(FieldDSL.class) Closure<?> cl) {
        builder.addField(FieldDSL.field(modifiers, type, name, cl).getBuilder().build());
    }

    public void property(TypeName type, String name,
                         @DelegatesTo(PropertyDSL.class) Closure<?> cl) {
        PropertyDSL dsl = PropertyDSL.property(type, name, cl);
        builder.addField(dsl.getFieldBuilder().build());
        if (!dsl.isNoGetter()) {
            builder.addMethod(dsl.getGetterBuilder().build());
        }
        if (!dsl.isNoSetter()) {
            builder.addMethod(dsl.getSetterBuilder().build());
        }
    }

    public void method(List<Modifier> modifiers, String name,
                       @DelegatesTo(MethodDSL.class) Closure<?> cl) {
        builder.addMethod(MethodDSL.method(modifiers, name, cl).getBuilder().build());
    }
}
