package io.codebot.apt.code;

import io.codebot.apt.type.Type;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Set;

public interface Method {
    ExecutableElement getElement();

    String getSimpleName();

    Set<Modifier> getModifiers();

    Type getContainingType();

    Type getReturnType();

    List<? extends Parameter> getParameters();
}
