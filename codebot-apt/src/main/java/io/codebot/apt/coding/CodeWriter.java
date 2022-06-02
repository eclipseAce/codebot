package io.codebot.apt.coding;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;

import javax.lang.model.type.TypeMirror;

public interface CodeWriter {
    void write(CodeBlock code);

    void write(String format, Object... args);

    Variable writeNewVariable(String nameSuggestion, TypeMirror type);

    Variable writeNewVariable(String nameSuggestion, TypeMirror type, CodeBlock initial);

    void beginControlFlow(String controlFlow, Object... args);

    void nextControlFlow(String controlFlow, Object... args);

    void endControlFlow();

    void endControlFlow(String controlFlow, Object... args);

    String newName(String nameSuggestion);

    CodeWriter newWriter();

    boolean isEmpty();

    CodeBlock toCode();

    static CodeWriter create() {
        return new SimpleCodeWriter();
    }

    static CodeWriter create(MethodSpec.Builder methodBuilder) {
        CodeWriter code = create();
        methodBuilder.parameters.forEach(it -> code.newName(it.name));
        return code;
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
        public Variable writeNewVariable(String nameSuggestion, TypeMirror type) {
            Variable variable = Variable.of(type, newName(nameSuggestion));
            write("$T $N;\n", variable.getType(), variable.getName());
            return variable;
        }

        @Override
        public Variable writeNewVariable(String nameSuggestion, TypeMirror type, CodeBlock initial) {
            Variable variable = Variable.of(type, newName(nameSuggestion));
            write("$T $N = $L;\n", variable.getType(), variable.getName(), initial);
            return variable;
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
            return new SimpleCodeWriter(nameAllocator.clone());
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
}
