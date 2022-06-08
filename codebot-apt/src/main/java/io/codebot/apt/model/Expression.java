package io.codebot.apt.model;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.lang.model.type.TypeMirror;

public interface Expression {
    CodeBlock getCode();

    TypeMirror getType();

    static Expression of(TypeMirror type, CodeBlock code) {
        return new SimpleExpression(type, code);
    }

    static Expression of(TypeMirror type, String format, Object... args) {
        return of(type, CodeBlock.of(format, args));
    }

    @AllArgsConstructor
    class SimpleExpression implements Expression {
        private final @Getter TypeMirror type;
        private final @Getter CodeBlock code;
    }
}
