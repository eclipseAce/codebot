package io.codebot.apt.coding;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Set;

public interface Method {
    ExecutableElement getElement();

    String getSimpleName();

    Set<Modifier> getModifiers();

    DeclaredType getContainingType();

    TypeMirror getReturnType();

    List<? extends Parameter> getParameters();
}
