package io.codebot.apt.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;

import java.util.List;
import java.util.Objects;

public abstract class AbstractFindSnippet implements CodeSnippet<Expression> {
    private Entity entity;
    private final List<ContextVariable> contextVariables = Lists.newArrayList();

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void addContextVariable(String name, Type type) {
        contextVariables.add(new ContextVariable(name, type));
    }

    protected Entity getEntity() {
        return entity;
    }

    protected List<ContextVariable> getContextVariables() {
        return contextVariables;
    }

    @Override
    public Expression writeTo(CodeBuffer codeBuffer) {
        Expression result = null;
        if (contextVariables.size() == 1
                && contextVariables.get(0).getName().equals(entity.getIdName())
                && contextVariables.get(0).getType().isAssignableTo(entity.getIdType())) {
            result = findById(codeBuffer, contextVariables.get(0));
        }
        if (result == null) {
            result = find(codeBuffer);
        }
        if (result == null) {
            result = findAll(codeBuffer);
        }
        return result;
    }

    protected abstract Expression findById(CodeBuffer codeBuffer, ContextVariable idVariable);

    protected abstract Expression findAll(CodeBuffer codeBuffer);

    protected abstract Expression find(CodeBuffer codeBuffer);

    protected static class ContextVariable {
        private final String name;
        private final Type type;

        ContextVariable(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }
    }

    protected interface ContextVariableScanner {
        default CodeBlock scan(List<ContextVariable> variables) {
            CodeBlock.Builder builder = CodeBlock.builder();
            for (ContextVariable variable : variables) {
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
                builder.add(postScanVariable(variable, code));
            }
            return builder.build();
        }

        default CodeBlock postScanVariable(ContextVariable variable, CodeBlock code) {
            if (!code.isEmpty() && !variable.getType().isPrimitive()) {
                return CodeBlock.builder()
                        .beginControlFlow("if ($1N != null)", variable.getName())
                        .add(code)
                        .endControlFlow()
                        .build();
            }
            return code;
        }

        CodeBlock scanVariable(ContextVariable variable);

        CodeBlock scanVariableMethod(ContextVariable variable, Executable method);

        CodeBlock scanVariableGetter(ContextVariable variable, GetAccessor getter);
    }
}
