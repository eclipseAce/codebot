package io.codebot.apt.crud.autocode;

import com.squareup.javapoet.CodeBlock;

public interface Code {
    void appendTo(CodeBlock.Builder codeBuilder);
}
