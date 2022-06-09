package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;

public interface CodeWriter {
    void write(CodeBlock code);

    void write(String format, Object... args);

    <R> R write(CodeSnippet<R> snippet);

    void beginControlFlow(String controlFlow, Object... args);

    void nextControlFlow(String controlFlow, Object... args);

    void endControlFlow();

    void endControlFlow(String controlFlow, Object... args);

    String newName(String nameSuggestion);

    String nextVariableName();

    CodeWriter forkNew();

    boolean isEmpty();

    CodeBlock toCode();
}
