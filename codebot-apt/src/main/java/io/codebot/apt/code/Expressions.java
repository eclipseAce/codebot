package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Expressions {

    public static Expression of(Type type, CodeBlock code) {
        return new ExpressionImpl(type, code);
    }

    @Value
    private static class ExpressionImpl implements Expression {
        Type type;
        CodeBlock code;
    }
}
