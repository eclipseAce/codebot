package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Variable;

import java.util.List;

public abstract class FilterSnippet implements Snippet {
    protected final Entity entity;
    protected final List<Variable> parameters;
    protected final String variableName;

    protected FilterSnippet(Entity entity, List<Variable> parameters, String variableName) {
        this.entity = entity;
        this.parameters = parameters;
        this.variableName = variableName;
    }

    protected CodeBlock fromParameters() {
        CodeBlock.Builder code = CodeBlock.builder();
        for (Variable param : parameters) {
            CodeBlock.Builder paramCode = CodeBlock.builder();
            if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
                paramCode.add(nullCheckIfRequired(param, fromParameter(param)));
            }
            if (paramCode.isEmpty()) {
                paramCode.add(fromParameterMethods(param));
            }
            if (paramCode.isEmpty()) {
                paramCode.add(fromParameterGetters(param));
            }
            code.add(paramCode.build());
        }
        return code.build();
    }

    protected abstract CodeBlock fromParameter(Variable param);

    protected CodeBlock fromParameterMethods(Variable param) {
        CodeBlock.Builder code = CodeBlock.builder();
        for (Executable method : param.getType().getMethods()) {
            code.add(fromParameterMethod(param, method));
        }
        return nullCheckIfRequired(param, code.build());
    }

    protected abstract CodeBlock fromParameterMethod(Variable param, Executable method);

    protected CodeBlock fromParameterGetters(Variable param) {
        CodeBlock.Builder code = CodeBlock.builder();
        for (GetAccessor getter : param.getType().getGetters()) {
            if (entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
                code.add(fromParameterGetter(param, getter));
            }
        }
        return nullCheckIfRequired(param, code.build());
    }

    protected abstract CodeBlock fromParameterGetter(Variable param, GetAccessor getter);

    protected CodeBlock nullCheckIfRequired(Variable param, CodeBlock code) {
        if (code.isEmpty()) {
            return CodeBlock.of("");
        }
        if (param.getType().isPrimitive()) {
            return code;
        }
        return CodeBlock.builder()
                .beginControlFlow("if ($1N != null)", param.getSimpleName())
                .add(code)
                .endControlFlow()
                .build();
    }
}
