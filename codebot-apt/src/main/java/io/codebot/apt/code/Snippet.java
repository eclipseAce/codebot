package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.type.Type;

public interface Snippet {
    void appendTo(CodeBlock.Builder code, NameAllocator names);
}
