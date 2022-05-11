package io.codebot.apt.type;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

public interface Executable {
    ExecutableElement asElement();

    String simpleName();

    Type returnType();

    List<Variable> parameters();

    List<Type> thrownTypes();
}
