package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;

public interface CodeSnippet<R> {
    R writeTo(CodeBuffer context);
}
