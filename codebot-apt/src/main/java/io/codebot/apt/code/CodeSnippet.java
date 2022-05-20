package io.codebot.apt.code;

public interface CodeSnippet<R> {
    R writeTo(CodeBuffer context);
}
