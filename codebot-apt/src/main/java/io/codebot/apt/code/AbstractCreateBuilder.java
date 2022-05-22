package io.codebot.apt.code;

import com.google.common.collect.Maps;
import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.SetAccessor;
import io.codebot.apt.type.Type;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractCreateBuilder implements CreateBuilder {
    private Entity entity;

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void create(CodeWriter codeWriter, List<Variable> variables, Type returnType) {
        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Variable variable : variables) {
            Optional<SetAccessor> setter = getEntity().getType()
                    .findSetter(variable.getName(), variable.getType());
            if (setter.isPresent()) {
                sources.put(variable.getName(), variable.asExpression());
                continue;
            }
            for (GetAccessor getter : variable.getType().getGetters()) {
                setter = getEntity().getType()
                        .findSetter(getter.getAccessedName(), getter.getAccessedType());
                if (setter.isPresent()) {
                    sources.put(getter.getAccessedName(), Expressions.of(
                            getter.getAccessedType(),
                            CodeBlock.of("$1N.$2N()", variable.getName(), getter.getSimpleName())
                    ));
                }
            }
        }
        Variable result = doCreate(codeWriter, sources);

        if (returnType.isVoid()) {
            return;
        }

        codeWriter.add("return $L;\n", doMappings(codeWriter, result, returnType));
    }

    protected abstract Variable doCreate(CodeWriter codeWriter, Map<String, Expression> sources);

    protected CodeBlock doMappings(CodeWriter codeWriter, Variable source, Type targetType) {
        if (source.getType().equals(getEntity().getType())
                && targetType.isAssignableFrom(getEntity().getIdType())) {
            return CodeBlock.of("$1N.$2N()", source.getName(), getEntity().getIdGetter().getSimpleName());
        }

        String tempVar = codeWriter.newName("temp");
        codeWriter.add("$1T $2N = new $1T();\n", targetType.getTypeMirror(), tempVar);
        for (SetAccessor setter : targetType.getSetters()) {
            source.getType().findGetter(setter.getAccessedName(), setter.getAccessedType()).ifPresent(it ->
                    codeWriter.add("$1N.$2N($3N.$4N());\n",
                            tempVar, setter.getSimpleName(), source.getName(), it.getSimpleName()
                    )
            );
        }
        return CodeBlock.of("$N", tempVar);
    }
}
