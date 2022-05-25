package io.codebot.apt.code;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;

public interface Field extends Variable {
    VariableElement getElement();

    DeclaredType getContainingType();
}
