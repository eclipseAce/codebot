package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import io.codebot.apt.model.Variable;

import javax.lang.model.type.TypeMirror;

public interface CodeWriter {
    void write(CodeBlock code);

    void write(String format, Object... args);

    <R> R write(CodeSnippet<R> snippet);

    void beginControlFlow(String controlFlow, Object... args);

    void nextControlFlow(String controlFlow, Object... args);

    void endControlFlow();

    void endControlFlow(String controlFlow, Object... args);

    String newName(String nameSuggestion);

    CodeWriter newWriter();

    boolean isEmpty();

    CodeBlock toCode();
}
