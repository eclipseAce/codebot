package io.codebot.apt.code;

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

    public Expression writeAsVariableTo(CodeBuffer codeBuffer, String nameSuggestion) {
        String name = codeBuffer.nameAllocator().newName(nameSuggestion);
        codeBuffer.add("$1T $2N = $3L;\n", getType().getTypeMirror(), name, getCode());
        return new Expression(CodeBlock.of("$N", name), getType());
    }
}