package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CodeWriters {

    public static CodeWriter create() {
        return new CodeWriterImpl(new NameAllocator());
    }

    private static class CodeWriterImpl implements CodeWriter {
        private final CodeBlock.Builder builder;
        private final NameAllocator nameAllocator;

        public CodeWriterImpl(NameAllocator nameAllocator) {
            this.builder = CodeBlock.builder();
            this.nameAllocator = nameAllocator;
        }

        @Override
        public CodeWriterImpl add(CodeBlock code) {
            builder.add(code);
            return this;
        }

        @Override
        public CodeWriterImpl add(String format, Object... args) {
            builder.add(format, args);
            return this;
        }

        @Override
        public CodeWriter fork() {
            return new CodeWriterImpl(nameAllocator.clone());
        }

        @Override
        public String allocateName(String nameSuggestion) {
            return nameAllocator.newName(nameSuggestion);
        }

        @Override
        public Variable declareVariable(String nameSuggestion, Expression expression) {
            Variable variable = Variables.of(expression.getType(), allocateName(nameSuggestion));
            add("$1T $2N = $3L;\n", variable.getType(), variable.getName(), expression.getCode());
            return variable;
        }

        @Override
        public boolean isEmpty() {
            return builder.isEmpty();
        }

        @Override
        public CodeBlock getCode() {
            return builder.build();
        }
    }
}
