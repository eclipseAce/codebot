package io.codebot.apt.code;

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

public class QuerydslFilterSnippet extends FilterSnippet {
    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_FQN = "com.querydsl.core.types.EntityPath";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";

    public QuerydslFilterSnippet(Entity entity, List<Variable> parameters, String variableName) {
        super(entity, parameters, variableName);
    }

    @Override
    public void appendTo(CodeBlock.Builder code, NameAllocator names) {
        CodeBlock buildStatements = fromParameters();
        if (!buildStatements.isEmpty()) {
            code.add("$1T $2N = new $1T();\n",
                    ClassName.bestGuess(BOOLEAN_BUILDER_FQN), variableName
            );
            code.add(buildStatements);
        }
    }

    @Override
    protected CodeBlock fromParameter(Variable param) {
        return CodeBlock.builder()
                .beginControlFlow("if ($1N != null)", param.getSimpleName())
                .add("$1N.and($2L.$3N.eq($3N));\n",
                        variableName, getQueryVar(entity.getTypeName()), param.getSimpleName()
                )
                .endControlFlow()
                .build();
    }

    @Override
    protected CodeBlock fromParameterMethod(Variable param, Executable method) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return CodeBlock.of("");
        }
        TypeElement entityPathElement = entity.getType().getFactory().getElementUtils()
                .getTypeElement(ENTITY_PATH_FQN);

        List<CodeBlock> args = Lists.newArrayList();
        for (Variable arg : method.getParameters()) {
            if (arg.getType().isAssignableTo(ENTITY_PATH_FQN)) {
                args.add(getQueryVar((ClassName) TypeName.get(
                        arg.getType().asMember(entityPathElement.getTypeParameters().get(0))
                )));
            } else {
                return CodeBlock.of("");
            }
        }
        return CodeBlock.of("$1N.and($2N.$3N($4L));\n",
                variableName, param.getSimpleName(), method.getSimpleName(), CodeBlock.join(args, ", ")
        );
    }

    @Override
    protected CodeBlock fromParameterGetter(Variable param, GetAccessor getter) {
        return CodeBlock.builder()
                .beginControlFlow("if ($1N.$2N() != null)", param.getSimpleName(), getter.getSimpleName())
                .add("$1N.and($2L.$3N.eq($4N.$5N()));\n",
                        variableName, getQueryVar(entity.getTypeName()), getter.getAccessedName(),
                        param.getSimpleName(), getter.getSimpleName()
                )
                .endControlFlow()
                .build();
    }

    private CodeBlock getQueryVar(ClassName entityName) {
        return CodeBlock.of("$1T.$2N",
                ClassName.get(entityName.packageName(), "Q" + entityName.simpleName()),
                StringUtils.uncapitalize(entityName.simpleName())
        );
    }
}
