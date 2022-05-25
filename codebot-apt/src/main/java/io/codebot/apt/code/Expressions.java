package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

import javax.lang.model.type.TypeMirror;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Expressions {

    public static Expression of(TypeMirror type, CodeBlock code) {
        return new ExpressionImpl(type, code);
    }

    @Value
    private static class ExpressionImpl implements Expression {
        TypeMirror type;
        CodeBlock code;
    }
}
