package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.model.Variable;

import javax.lang.model.type.TypeMirror;

public class SimpleCodeWriter implements CodeWriter {
    private final CodeBlock.Builder builder;
    private final NameAllocator nameAllocator;

    public static CodeWriter create() {
        return new SimpleCodeWriter();
    }

    public static CodeWriter create(MethodSpec.Builder methodBuilder) {
        CodeWriter code = create();
        methodBuilder.parameters.forEach(it -> code.newName(it.name));
        return code;
    }

    SimpleCodeWriter() {
        this(new NameAllocator());
    }

    SimpleCodeWriter(NameAllocator nameAllocator) {
        this.builder = CodeBlock.builder();
        this.nameAllocator = nameAllocator;
    }

    @Override
    public void write(CodeBlock code) {
        builder.add(code);
    }

    @Override
    public void write(String format, Object... args) {
        builder.add(format, args);
    }

    @Override
    public <R> R write(CodeSnippet<R> snippet) {
        return snippet.writeTo(this);
    }

    @Override
    public void beginControlFlow(String controlFlow, Object... args) {
        builder.beginControlFlow(controlFlow, args);
    }

    @Override
    public void nextControlFlow(String controlFlow, Object... args) {
        builder.nextControlFlow(controlFlow, args);
    }

    @Override
    public void endControlFlow() {
        builder.endControlFlow();
    }

    @Override
    public void endControlFlow(String controlFlow, Object... args) {
        builder.endControlFlow(controlFlow, args);
    }

    @Override
    public String newName(String nameSuggestion) {
        return nameAllocator.newName(nameSuggestion);
    }

    @Override
    public CodeWriter newWriter() {
        return new io.codebot.apt.code.SimpleCodeWriter(nameAllocator.clone());
    }

    @Override
    public boolean isEmpty() {
        return builder.isEmpty();
    }

    @Override
    public CodeBlock toCode() {
        return builder.build();
    }
}
