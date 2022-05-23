package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;
import lombok.Value;

public final class Expressions {
    private Expressions() {
    }

    public static Expression of(Type type, CodeBlock code) {
        return new ExpressionImpl(type, code);
    }

    @Value
    private static class ExpressionImpl implements Expression {
        Type type;
        CodeBlock code;
    }
}
