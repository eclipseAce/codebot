package io.cruder.apt.model;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

public class Accessor {
    private final ExecutableElement element;
    private final AccessorKind kind;
    private final String propertyName;
    private final TypeMirror propertyType;

    Accessor(ExecutableElement element,
             AccessorKind kind,
             String propertyName,
             TypeMirror propertyType) {
        this.element = element;
        this.kind = kind;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    public ExecutableElement getElement() {
        return element;
    }

    public AccessorKind getKind() {
        return kind;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public TypeMirror getPropertyType() {
        return propertyType;
    }
}