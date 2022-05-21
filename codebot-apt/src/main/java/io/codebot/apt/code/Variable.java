package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public interface Variable {
    String getName();

    Type getType();

    default Expression asExpression() {
        return Expressions.of(getType(), CodeBlock.of("$N", getName()));
    }
}
