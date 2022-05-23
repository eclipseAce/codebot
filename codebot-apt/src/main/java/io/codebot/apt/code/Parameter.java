package io.codebot.apt.code;

import javax.lang.model.element.VariableElement;

public interface Parameter extends Variable {
    VariableElement getElement();
}
