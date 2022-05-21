package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;

import javax.lang.model.element.ExecutableElement;

public final class CodeBuilders {
    private CodeBuilders() {
    }

    public static CodeBuilder create() {
        return new SimpleCodeBuilder(CodeBlock.builder(), new NameAllocator());
    }

    public static CodeBuilder create(NameAllocator existingNames) {
        return new SimpleCodeBuilder(CodeBlock.builder(), existingNames.clone());
    }

    public static CodeBuilder create(ExecutableElement method) {
        NameAllocator names = new NameAllocator();
        method.getParameters().forEach(param ->
                names.newName(param.getSimpleName().toString())
        );
        return new SimpleCodeBuilder(CodeBlock.builder(), names);
    }

    private static class SimpleCodeBuilder implements CodeBuilder {
        private final CodeBlock.Builder builder;
        private final NameAllocator nameAllocator;

        SimpleCodeBuilder(CodeBlock.Builder builder, NameAllocator nameAllocator) {
            this.builder = builder;
            this.nameAllocator = nameAllocator;
        }

        @Override
        public NameAllocator names() {
            return nameAllocator;
        }

        @Override
        public SimpleCodeBuilder add(CodeBlock code) {
            builder.add(code);
            return this;
        }

        @Override
        public SimpleCodeBuilder add(String format, Object... args) {
            builder.add(format, args);
            return this;
        }

        @Override
        public CodeBlock toCode() {
            return builder.build();
        }

        @Override
        public void appendTo(CodeBlock.Builder code) {
            code.add(toCode());
        }

        @Override
        public void appendTo(MethodSpec.Builder method) {
            method.addCode(toCode());
        }
    }
}
