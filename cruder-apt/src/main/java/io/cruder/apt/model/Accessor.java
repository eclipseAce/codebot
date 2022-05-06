package io.cruder.apt.model;

import javax.lang.model.element.ExecutableElement;

public class Accessor {
    private ExecutableElement element;
    private AccessorKind kind;
    private String accessedName;
    private Type accessedType;
}
