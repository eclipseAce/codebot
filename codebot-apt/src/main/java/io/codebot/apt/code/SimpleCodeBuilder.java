package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;

public class SimpleCodeBuilder implements CodeBuffer {
    private final CodeBlock.Builder codeBuilder;
    private final NameAllocator nameAllocator;

    public SimpleCodeBuilder(NameAllocator nameAllocator) {
        this.codeBuilder = CodeBlock.builder();
        this.nameAllocator = nameAllocator.clone();
    }

    @Override
    public NameAllocator nameAllocator() {
        return nameAllocator;
    }

    @Override
    public void add(CodeBlock code) {
        codeBuilder.add(code);
    }

    public CodeBlock toCode() {
        return codeBuilder.build();
    }
}