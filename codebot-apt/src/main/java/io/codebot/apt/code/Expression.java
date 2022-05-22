package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public interface Expression {
    CodeBlock getCode();

    Type getType();
}
