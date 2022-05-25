package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;

import javax.lang.model.type.TypeMirror;

public interface Variable {
    String getName();

    TypeMirror getType();

    default Expression asExpression() {
        return Expressions.of(getType(), CodeBlock.of("$N", getName()));
    }
}
