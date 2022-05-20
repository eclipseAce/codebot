package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;

public interface CodeBuffer {
    NameAllocator nameAllocator();

    void add(CodeBlock code);

    default void add(String format, Object ...args) {
        add(CodeBlock.of(format, args));
    }
}
