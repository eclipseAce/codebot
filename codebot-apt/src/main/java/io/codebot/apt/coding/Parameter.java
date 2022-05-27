package io.codebot.apt.coding;

import javax.lang.model.element.VariableElement;

public interface Parameter extends Variable {
    VariableElement getElement();
}
