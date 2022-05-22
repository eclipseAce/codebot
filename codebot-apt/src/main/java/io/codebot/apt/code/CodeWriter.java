package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public interface CodeWriter {
    CodeWriter add(CodeBlock code);

    CodeWriter add(String format, Object... args);

    CodeWriter fork();

    String newName(String nameSuggestion);

    Variable newVariable(String nameSuggestion, Type type);

    Variable newVariable(String nameSuggestion, Expression expression);

    boolean isEmpty();

    CodeBlock getCode();
}
