package io.codebot.apt.model;

import com.squareup.javapoet.CodeBlock;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.lang.model.type.TypeMirror;

public interface Variable extends Expression {
    String getName();

    default CodeBlock getCode() {
        return CodeBlock.of("$N", getName());
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
