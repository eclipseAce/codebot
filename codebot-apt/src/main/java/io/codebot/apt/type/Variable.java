package io.codebot.apt.type;

import javax.lang.model.element.VariableElement;

public interface Variable {
    VariableElement asElement();

    String simpleName();

    Type type();
}
