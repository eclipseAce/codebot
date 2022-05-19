package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;

public interface CodeSnippet<R> {
    R write(CodeBlock.Builder code, NameAllocator names);
}
