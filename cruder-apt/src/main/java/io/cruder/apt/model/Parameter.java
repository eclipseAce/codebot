package io.cruder.apt.model;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

public class Parameter {
    private final VariableElement element;
    private final TypeMirror type;

    Parameter(VariableElement element,
              TypeMirror type) {
        this.element = element;
        this.type = type;
    }
}
