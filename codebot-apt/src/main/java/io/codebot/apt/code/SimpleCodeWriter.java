package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;

import java.util.concurrent.atomic.AtomicInteger;

public class SimpleCodeWriter implements CodeWriter {
    private final CodeBlock.Builder builder;
    private final NameAllocator nameAllocator;

    private final AtomicInteger variableNumber;

    public static CodeWriter create() {
        return new SimpleCodeWriter();
    }

    public static CodeWriter create(MethodSpec.Builder methodBuilder) {
        CodeWriter code = create();
        methodBuilder.parameters.forEach(it -> code.newName(it.name));
        return code;
    }

    SimpleCodeWriter() {
        this(new NameAllocator(), 1);
    }

    SimpleCodeWriter(NameAllocator nameAllocator, int initialVariableNumber) {
        this.builder = CodeBlock.builder();
        this.nameAllocator = nameAllocator;
        this.variableNumber = new AtomicInteger(initialVariableNumber);
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
    public String nextVariableName() {
        int current = variableNumber.getAndIncrement();
        return nameAllocator.newName("var" + current);
    }

    @Override
    public CodeWriter forkNew() {
        return new SimpleCodeWriter(nameAllocator.clone(), variableNumber.get());
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
