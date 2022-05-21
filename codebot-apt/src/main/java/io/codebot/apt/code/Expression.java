package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public interface Expression {
    CodeBlock getCode();

    Type getType();

    default Variable asVariable(CodeBuilder codeBuilder, String nameSuggestion) {
        String name = codeBuilder.names().newName(nameSuggestion);
        codeBuilder.add("$1T $2N = $3L;\n", getType().getTypeMirror(), name, getCode());
        return Variables.of(getType(), name);
    }
}
