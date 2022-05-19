package io.codebot.apt.crud.query;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeName;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Variable;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class QuerydslPredicateFactory {
    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_FQN = "com.querydsl.core.types.EntityPath";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";

    private Entity entity;
    private String builderVar;

    public Expression getExpression(List<Variable> params,
                                    Entity entity,
                                    NameAllocator nameAllocator,
                                    CodeBlock.Builder context) {
        this.entity = entity;
        this.builderVar = nameAllocator.newName("builder");

        CodeBlock.Builder statements = CodeBlock.builder();
        for (Variable param : params) {
            CodeBlock.Builder builder = CodeBlock.builder();

            build(builder, param);
            if (builder.isEmpty()) {
                param.getType().getMethods().forEach(method -> build(builder, param, method));
            }
            if (builder.isEmpty()) {
                param.getType().getGetters().forEach(getter -> build(builder, param, getter));
            }
            if (!builder.isEmpty() && !param.getType().isPrimitive()) {
                statements.beginControlFlow("if ($1N != null)", param.getSimpleName());
                statements.add(builder.build());
                statements.endControlFlow();
            } else {
                statements.add(builder.build());
            }
        }
        if (statements.isEmpty()) {
            return null;
        }
        context.add("$1T $2N = new $1T();\n", ClassName.bestGuess(BOOLEAN_BUILDER_FQN), builderVar);
        context.add(statements.build());
        return new Expression(
                CodeBlock.of("$N", builderVar),
                entity.getType().getFactory().getType(BOOLEAN_BUILDER_FQN)
        );
    }

    private void build(CodeBlock.Builder builder, Variable param) {
        if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
            builder.add("$1N.and($2L.$3N.eq($3N));\n",
                    builderVar, getQueryVar(entity.getTypeName()), param.getSimpleName()
            );
        }
    }

    private void build(CodeBlock.Builder builder, Variable param, Executable method) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return;
        }
        TypeElement entityPathElement = entity.getType().getFactory()
                .getElementUtils().getTypeElement(ENTITY_PATH_FQN);
        List<CodeBlock> args = Lists.newArrayList();
        for (Variable arg : method.getParameters()) {
            if (arg.getType().isAssignableTo(ENTITY_PATH_FQN)) {
                ClassName entityName = (ClassName) TypeName.get(
                        arg.getType().asMember(entityPathElement.getTypeParameters().get(0))
                );
                args.add(getQueryVar(entityName));
            } else {
                return;
            }
        }
        builder.add("$1N.and($2N.$3N($4L));\n",
                builderVar, param.getSimpleName(), method.getSimpleName(),
                CodeBlock.join(args, ", ")
        );
    }

    private void build(CodeBlock.Builder builder, Variable param, GetAccessor getter) {
        if (entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
            builder.beginControlFlow("if ($1N.$2N() != null)",
                    param.getSimpleName(), getter.getSimpleName());
            builder.add("$1N.and($2L.$3N.eq($4N.$5N()));\n",
                    builderVar, getQueryVar(entity.getTypeName()), getter.getAccessedName(),
                    param.getSimpleName(), getter.getSimpleName());
            builder.endControlFlow();
        }
    }

    private CodeBlock getQueryVar(ClassName entityName) {
        return CodeBlock.of("$1T.$2N",
                ClassName.get(entityName.packageName(), "Q" + entityName.simpleName()),
                StringUtils.uncapitalize(entityName.simpleName())
        );
    }
}
