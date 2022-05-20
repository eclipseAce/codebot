package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public class FindExpression {
    private final CodeBlock code;
    private final Type type;

    FindExpression(CodeBlock code, Type type) {
        this.code = code;
        this.type = type;
    }

    public CodeBlock getCode() {
        return code;
    }

    public Type getType() {
        return type;
    }
}