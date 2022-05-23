package io.codebot.apt.code;

import io.codebot.apt.type.Type;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

public interface Method {
    ExecutableElement getElement();

    String getSimpleName();

    Type getContainingType();

    Type getReturnType();

    List<Parameter> getParameters();
}
