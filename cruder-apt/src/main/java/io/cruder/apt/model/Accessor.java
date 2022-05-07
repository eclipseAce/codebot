package io.cruder.apt.model;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;

public class Accessor {
    private ExecutableElement element;
    private AccessorKind kind;
    private String accessedName;
    private TypeMirror accessedType;

    Accessor(ExecutableElement element,
             AccessorKind kind,
             String accessedName,
             TypeMirror accessedType) {
        this.element = element;
        this.kind = kind;
        this.accessedName = accessedName;
        this.accessedType = accessedType;
    }
}
