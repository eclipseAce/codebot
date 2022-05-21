package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;

public interface CodeBuilder {
    NameAllocator names();

    CodeBuilder add(CodeBlock code);

    CodeBuilder add(String format, Object... args);

    CodeBlock toCode();

    void appendTo(CodeBlock.Builder code);

    void appendTo(MethodSpec.Builder method);


}
