package io.codebot.apt.model;

import javax.lang.model.element.VariableElement;

public interface Parameter extends Variable {
    VariableElement getElement();
}
