package io.codebot.apt.code;

import io.codebot.apt.type.Type;

import javax.lang.model.element.VariableElement;

public interface Field extends Variable {
    VariableElement getElement();

    Type getDeclaringType();
}
