package io.cruder.apt.model;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public class Method {
    private final ExecutableElement element;
    private final TypeMirror returnType;
    private final List<Parameter> parameters;
    private final List<? extends TypeMirror> exceptions;

    Method(ExecutableElement element,
           TypeMirror returnType,
           List<Parameter> parameters,
           List<? extends TypeMirror> exceptions) {
        this.element = element;
        this.returnType = returnType;
        this.parameters = parameters;
        this.exceptions = exceptions;
    }
}
