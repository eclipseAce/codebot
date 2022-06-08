package io.codebot.apt.model;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

public interface Field extends Variable {
    VariableElement getElement();

    DeclaredType getContainingType();
}
