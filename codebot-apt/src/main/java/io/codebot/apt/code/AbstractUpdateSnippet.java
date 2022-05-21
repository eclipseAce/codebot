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

public abstract class AbstractUpdateSnippet implements UpdateSnippet {
    private Entity entity;

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void update(CodeBuilder codeBuilder, List<Variable> variables, Type returnType) {
        Expression findExpr = null;
        Map<String, Expression> sources = Maps.newLinkedHashMap();
        for (Variable variable : variables) {
            if (variable.getName().equals(getEntity().getIdName())
                    && variable.getType().isAssignableTo(getEntity().getIdType())) {
                findExpr = doFindById(codeBuilder, variable.asExpression());
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
                    if (findExpr == null) {
                        findExpr = doFindById(codeBuilder, Expressions.of(
                                getter.getAccessedType(),
                                CodeBlock.of("$1N.$2N()", variable.getName(), getter.getSimpleName())
                        ));
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

        if (findExpr == null) {
            // no id found
            return;
        }
        Variable entityVar = findExpr.asVariable(codeBuilder, "entity");
        doUpdate(codeBuilder, entityVar.asExpression(), sources);

        if (returnType.isVoid()) {
            return;
        }
        String tempVar = codeBuilder.names().newName("temp");
        codeBuilder.add("$1T $2N = new $1T();\n", returnType.getTypeMirror(), tempVar);
        for (SetAccessor setter : returnType.getSetters()) {
            findExpr.getType().findGetter(setter.getAccessedName(), setter.getAccessedType()).ifPresent(it ->
                    codeBuilder.add("$1N.$2N($3N.$4N());\n",
                            tempVar, setter.getSimpleName(), entityVar.getName(), it.getSimpleName()
                    )
            );
        }
        codeBuilder.add("return $N;\n", tempVar);
    }

    protected abstract Expression doFindById(CodeBuilder codeBuilder, Expression id);

    protected abstract void doUpdate(CodeBuilder codeBuilder, Expression target, Map<String, Expression> sources);
}
