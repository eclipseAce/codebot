package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.type.Type;
import lombok.Value;

import javax.lang.model.element.ExecutableElement;

public final class CodeWriters {
    private CodeWriters() {
    }

    public static CodeWriter create() {
        return new CodeWriterImpl(new NameAllocator());
    }

    public static CodeWriter create(ExecutableElement method) {
        CodeWriter codeWriter = create();
        method.getParameters().forEach(param ->
                codeWriter.newName(param.getSimpleName().toString())
        );
        return codeWriter;
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
        public String newName(String nameSuggestion) {
            return nameAllocator.newName(nameSuggestion);
        }

        @Override
        public Variable newVariable(String nameSuggestion, Type type) {
            Variable variable = new VariableImpl(type, newName(nameSuggestion));
            add("$1T $2N;\n", variable.getType().getTypeMirror(), variable.getName());
            return variable;
        }

        @Override
        public Variable newVariable(String nameSuggestion, Expression expression) {
            Variable variable = new VariableImpl(expression.getType(), newName(nameSuggestion));
            add("$1T $2N = $3L;\n", variable.getType().getTypeMirror(), variable.getName(), expression.getCode());
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

    @Value
    private static class VariableImpl implements Variable {
        Type type;
        String name;
    }
}
