package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;

public interface CodeWriter {
    CodeWriter add(CodeBlock code);

    CodeWriter add(String format, Object... args);

    CodeWriter fork();

    String allocateName(String nameSuggestion);

    Variable declareVariable(String nameSuggestion, Expression expression);

    boolean isEmpty();

    CodeBlock getCode();
}
