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

public abstract class AbstractUpdateBuilder implements UpdateBuilder {
    private Entity entity;

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void update(CodeWriter codeWriter, List<Variable> variables, Type returnType) {
        Expression entityId = null;
        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Variable variable : variables) {
            if (variable.getName().equals(getEntity().getIdName())
                    && variable.getType().isAssignableTo(getEntity().getIdType())) {
                if (entityId == null) {
                    entityId = variable.asExpression();
                }
                continue;
            }
            Optional<SetAccessor> setter = getEntity().getType()
                    .findSetter(variable.getName(), variable.getType());
            if (setter.isPresent()) {
                sources.put(variable.getName(), variable.asExpression());
                continue;
            }
            for (GetAccessor getter : variable.getType().getGetters()) {
                if (getter.getAccessedName().equals(getEntity().getIdName())
                        && getter.getAccessedType().isAssignableTo(getEntity().getIdType())) {
                    if (entityId == null) {
                        entityId = Expressions.of(
                                getter.getAccessedType(),
                                CodeBlock.of("$1N.$2N()", variable.getName(), getter.getSimpleName())
                        );
                    }
                    continue;
                }
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

        if (entityId == null) {
            // no id found
            return;
        }
        Variable result = doUpdate(codeWriter, entityId, sources);

        if (returnType.isVoid()) {
            return;
        }

        codeWriter.add("return $L;\n", doMappings(codeWriter, result, returnType));
    }

    protected abstract Variable doUpdate(CodeWriter codeWriter, Expression targetId,
                                         Map<String, Expression> sources);

    protected CodeBlock doMappings(CodeWriter codeWriter, Variable source, Type targetType) {
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
