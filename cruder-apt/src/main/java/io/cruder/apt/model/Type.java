package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class Type {
    private final Types typeUtils;
    private final Elements elementUtils;

    private final TypeMirror typeMirror;
    private final TypeElement typeElement;
    private final List<VariableElement> fields;
    private final List<ExecutableElement> methods;
    private final List<Accessor> accessors;

    Type(TypeFactory factory,
         TypeMirror typeMirror,
         TypeElement typeElement,
         List<VariableElement> fields,
         List<ExecutableElement> methods,
         List<Accessor> accessors) {
        this.typeUtils = factory.getTypeUtils();
        this.elementUtils = factory.getElementUtils();
        this.typeMirror = typeMirror;
        this.typeElement = typeElement;
        this.fields = fields != null ? ImmutableList.copyOf(fields) : ImmutableList.of();
        this.methods = methods != null ? ImmutableList.copyOf(methods) : ImmutableList.of();
        this.accessors = accessors != null ? ImmutableList.copyOf(accessors) : ImmutableList.of();
    }

    Type(TypeFactory factory, PrimitiveType primitiveType) {
        this(factory, primitiveType, null, null, null, null);
    }

    Type(TypeFactory factory, DeclaredType declaredType) {
        this(factory, declaredType, (TypeElement) declaredType.asElement())
    }
}
