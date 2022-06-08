package io.codebot.apt.model;

import javax.lang.model.type.TypeMirror;

public interface ReadMethod extends Method {
    String getReadName();

    TypeMirror getReadType();

    default Expression toExpression(Expression caller) {
        return Expression.of(getReadType(), "$L.$N()", caller.getCode(), getSimpleName());
    }
}
