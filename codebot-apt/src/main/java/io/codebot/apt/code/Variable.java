package io.codebot.apt.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.lang.model.type.TypeMirror;

public interface Variable {
    String getName();

    TypeMirror getType();

    default Expression asExpression() {
        return Expression.of(getType(), "$N", getName());
    }

    static Variable of(TypeMirror type, String name) {
        return new SimpleVariable(type, name);
    }

    @AllArgsConstructor
    class SimpleVariable implements Variable {
        private final @Getter TypeMirror type;
        private final @Getter String name;
    }
}
