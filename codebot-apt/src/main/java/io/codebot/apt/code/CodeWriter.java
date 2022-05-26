package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;

public interface CodeWriter {
    void write(CodeBlock code);

    void write(String format, Object... args);

    Variable writerNewVariable(String nameSuggestion, Expression expression);

    String newName(String nameSuggestion);

    boolean isEmpty();

    CodeWriter fork();

    CodeBlock getCode();

    static CodeWriter create() {
        return new SimpleCodeWriter();
    }

    class SimpleCodeWriter implements CodeWriter {
        private final CodeBlock.Builder builder;
        private final NameAllocator nameAllocator;

        public SimpleCodeWriter() {
            this(new NameAllocator());
        }

        private SimpleCodeWriter(NameAllocator nameAllocator) {
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
        public String newName(String nameSuggestion) {
            return nameAllocator.newName(nameSuggestion);
        }

        @Override
        public Variable writerNewVariable(String nameSuggestion, Expression expression) {
            Variable variable = Variable.of(expression.getType(), newName(nameSuggestion));
            write("$1T $2N = $3L;\n", variable.getType(), variable.getName(), expression.getCode());
            return variable;
        }

        @Override
        public boolean isEmpty() {
            return builder.isEmpty();
        }

        @Override
        public CodeWriter fork() {
            return new SimpleCodeWriter(nameAllocator.clone());
        }

        @Override
        public CodeBlock getCode() {
            return builder.build();
        }
    }
}
