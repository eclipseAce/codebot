package io.cruder.apt.dsl;

import com.google.common.collect.Iterables;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class TypeDSL extends DSLSupport {
    private final TypeSpec.Builder builder;

    public static TypeDSL declClass(Iterable<Modifier> modifiers, String typeName,
                                    @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        TypeDSL dsl = new TypeDSL(TypeSpec.classBuilder(typeName)
                .addModifiers(Iterables.toArray(modifiers, Modifier.class)));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public static TypeDSL declInterface(Iterable<Modifier> modifiers, String typeName,
                                        @DelegatesTo(TypeDSL.class) Closure<?> cl) {
        TypeDSL dsl = new TypeDSL(TypeSpec.interfaceBuilder(typeName)
                .addModifiers(Iterables.toArray(modifiers, Modifier.class)));
        cl.rehydrate(dsl, cl.getOwner(), dsl).call();
        return dsl;
    }

    public TypeDSL modifiers(Iterable<Modifier> modifiers) {
        modifiers.forEach(builder::addModifiers);
        return this;
    }

    public TypeDSL superclass(TypeName typeName) {
        builder.superclass(typeName);
        return this;
    }

    public TypeDSL superinterface(TypeName typeName) {
        builder.addSuperinterface(typeName);
        return this;
    }

    public TypeDSL superinterfaces(Iterable<? extends TypeName> typeNames) {
        builder.addSuperinterfaces(typeNames);
        return this;
    }

    public TypeDSL annotate(ClassName typeName,
                            @DelegatesTo(AnnotationDSL.class) Closure<?> cl) {
        builder.addAnnotation(AnnotationDSL.annotate(typeName, cl).build());
        return this;
    }

    public TypeDSL annotate(Iterable<ClassName> typeNames) {
        typeNames.forEach(builder::addAnnotation);
        return this;
    }

    public TypeDSL annotate(ClassName typeName) {
        builder.addAnnotation(typeName);
        return this;
    }

    public TypeDSL field(Iterable<Modifier> modifiers, TypeName typeName, String name,
                         @DelegatesTo(FieldDSL.class) Closure<?> cl) {
        builder.addField(FieldDSL.field(modifiers, typeName, name, cl).build());
        return this;
    }

    public TypeDSL property(TypeName typeName, String name,
                            @DelegatesTo(PropertyDSL.class) Closure<?> cl) {
        PropertyDSL dsl = PropertyDSL.property(typeName, name, cl);
        builder.addField(dsl.buildField()).addMethods(dsl.buildAccessors());
        return this;
    }

    public TypeDSL method(List<Modifier> modifiers, String name,
                          @DelegatesTo(MethodDSL.class) Closure<?> cl) {
        builder.addMethod(MethodDSL.method(modifiers, name, cl).build());
        return this;
    }

    public TypeSpec build() {
        return builder.build();
    }
}
