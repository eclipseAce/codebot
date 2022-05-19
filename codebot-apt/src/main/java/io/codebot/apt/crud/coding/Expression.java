package io.codebot.apt.crud.coding;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public class Expression {
    private final CodeBlock code;
    private final Type type;

    public Expression(CodeBlock code, Type type) {
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
