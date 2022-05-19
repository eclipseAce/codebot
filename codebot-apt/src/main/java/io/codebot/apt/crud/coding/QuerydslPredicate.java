package io.codebot.apt.crud.coding;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Variable;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class QuerydslPredicate extends LocalVariableScanner {
    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_FQN = "com.querydsl.core.types.EntityPath";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";

    private final MethodBodyContext context;
    private final Entity entity;
    private final List<LocalVariable> variables;

    private String builderVar;

    public QuerydslPredicate(MethodBodyContext context, Entity entity, List<LocalVariable> variables) {
        this.context = context;
        this.entity = entity;
        this.variables = variables;
    }

    public Expression createExpression() {
        builderVar = context.getNameAllocator().newName("builder");

        CodeBlock statements = scan(variables);
        if (statements.isEmpty()) {
            return null;
        }
        context.getCodeBuilder()
                .add("$1T $2N = new $1T();\n", ClassName.bestGuess(BOOLEAN_BUILDER_FQN), builderVar)
                .add(statements);
        return new Expression(
                CodeBlock.of("$N", builderVar),
                entity.getType().getFactory().getType(BOOLEAN_BUILDER_FQN)
        );
    }

    @Override
    protected CodeBlock scanVariable(LocalVariable variable) {
        if (!entity.getType().findGetter(variable.getName(), variable.getType()).isPresent()) {
            return null;
        }
        return CodeBlock.of("$1N.and($2L.$3N.eq($3N));\n",
                builderVar, getQueryVariable(entity.getTypeName()), variable.getName()
        );
    }

    @Override
    protected CodeBlock scanVariableMethod(LocalVariable variable, Executable method) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return null;
        }
        TypeElement entityPathElement = entity.getType().getFactory()
                .getElementUtils().getTypeElement(ENTITY_PATH_FQN);
        List<CodeBlock> args = Lists.newArrayList();
        for (Variable arg : method.getParameters()) {
            if (arg.getType().isAssignableTo(ENTITY_PATH_FQN)) {
                ClassName entityName = (ClassName) TypeName.get(
                        arg.getType().asMember(entityPathElement.getTypeParameters().get(0))
                );
                args.add(getQueryVariable(entityName));
            } else {
                return null;
            }
        }
        return CodeBlock.of("$1N.and($2N.$3N($4L));\n",
                builderVar, variable.getName(), method.getSimpleName(),
                CodeBlock.join(args, ", ")
        );
    }

    @Override
    protected CodeBlock scanVariableGetter(LocalVariable variable, GetAccessor getter) {
        if (!entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
            return null;
        }
        return CodeBlock.builder()
                .beginControlFlow("if ($1N.$2N() != null)",
                        variable.getName(), getter.getSimpleName())
                .add("$1N.and($2L.$3N.eq($4N.$5N()));\n",
                        builderVar, getQueryVariable(entity.getTypeName()), getter.getAccessedName(),
                        variable.getName(), getter.getSimpleName())
                .endControlFlow()
                .build();
    }

    protected CodeBlock getQueryVariable(ClassName entityName) {
        return CodeBlock.of("$1T.$2N",
                ClassName.get(entityName.packageName(), "Q" + entityName.simpleName()),
                StringUtils.uncapitalize(entityName.simpleName())
        );
    }
}
