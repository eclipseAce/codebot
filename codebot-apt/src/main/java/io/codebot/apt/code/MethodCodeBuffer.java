package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.Variable;

public class MethodCodeBuffer implements CodeBuffer {
    private final MethodSpec.Builder methodBuilder;
    private final NameAllocator nameAllocator;

    public MethodCodeBuffer(Executable method,
                            MethodSpec.Builder methodBuilder) {
        this.methodBuilder = methodBuilder;
        this.nameAllocator = new NameAllocator();
        for (Variable param : method.getParameters()) {
            this.nameAllocator.newName(param.getSimpleName());
        }
    }

    @Override
    public NameAllocator nameAllocator() {
        return nameAllocator;
    }

    @Override
    public void add(CodeBlock code) {
        methodBuilder.addCode(code);
    }

    @Override
    public void add(String format, Object... args) {
        methodBuilder.addCode(format, args);
    }
}
