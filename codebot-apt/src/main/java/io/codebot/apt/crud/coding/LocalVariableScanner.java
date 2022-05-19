package io.codebot.apt.crud.coding;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;

import java.util.List;
import java.util.Objects;

public abstract class LocalVariableScanner {

    protected CodeBlock scan(List<LocalVariable> variables) {
        CodeBlock.Builder builder = CodeBlock.builder();
        for (LocalVariable variable : variables) {
            CodeBlock code = scanVariable(variable);
            if (code == null || code.isEmpty()) {
                code = variable.getType().getMethods().stream()
                        .map(method -> scanVariableMethod(variable, method))
                        .filter(Objects::nonNull)
                        .collect(CodeBlock.joining(""));
            }
            if (code.isEmpty()) {
                code = variable.getType().getGetters().stream()
                        .map(getter -> scanVariableGetter(variable, getter))
                        .filter(Objects::nonNull)
                        .collect(CodeBlock.joining(""));
            }
            if (!code.isEmpty() && !variable.getType().isPrimitive()) {
                builder.beginControlFlow("if ($1N != null)", variable.getName());
                builder.add(code);
                builder.endControlFlow();
            } else {
                builder.add(code);
            }
        }
        return builder.build();
    }

    protected abstract CodeBlock scanVariable(LocalVariable variable);

    protected abstract CodeBlock scanVariableMethod(LocalVariable variable, Executable method);

    protected abstract CodeBlock scanVariableGetter(LocalVariable variable, GetAccessor getter);
}
