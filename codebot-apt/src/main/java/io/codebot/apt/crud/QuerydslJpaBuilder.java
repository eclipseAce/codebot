package io.codebot.apt.crud;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import io.codebot.apt.code.*;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class QuerydslJpaBuilder extends JpaBuilder {
    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_FQN = "com.querydsl.core.types.EntityPath";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";

    private CodeBlock querydslPredicateExecutor;

    public void setQuerydslPredicateExecutor(CodeBlock querydslPredicateExecutor) {
        this.querydslPredicateExecutor = querydslPredicateExecutor;
    }

    @Override
    protected Variable doQuery(Method overridden, MethodWriter methodWriter, List<Parameter> params) {
        String builderVar = methodWriter.body().allocateName("builder");

        Entity entity = getEntity();
        Type entityType = entity.getType();
        TypeFactory typeFactory = entityType.getFactory();
        CodeBlock predicateBuild = new ParameterScanner() {
            @Override
            public CodeBlock scanVariable(Variable variable) {
                if (entityType.findGetter(variable.getName(), variable.getType()).isPresent()) {
                    return CodeBlock.of("$1N.and($2L.$3N.eq($3N));\n",
                            builderVar, getEntityQuery(entity.getTypeName()), variable.getName()
                    );
                }
                return null;
            }

            @Override
            public CodeBlock scanVariableMethod(Variable variable, Executable method) {
                if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
                    return null;
                }
                TypeElement entityPathElement = typeFactory
                        .getElementUtils().getTypeElement(ENTITY_PATH_FQN);
                List<CodeBlock> args = Lists.newArrayList();
                for (io.codebot.apt.type.Parameter arg : method.getParameters()) {
                    if (arg.getType().isAssignableTo(ENTITY_PATH_FQN)) {
                        ClassName entityName = (ClassName) TypeName.get(
                                arg.getType().asMember(entityPathElement.getTypeParameters().get(0))
                        );
                        args.add(getEntityQuery(entityName));
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
            public CodeBlock scanVariableGetter(Variable variable, GetAccessor getter) {
                if (entityType.findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
                    return CodeBlock.builder()
                            .beginControlFlow("if ($1N.$2N() != null)",
                                    variable.getName(), getter.getSimpleName())
                            .add("$1N.and($2L.$3N.eq($4N.$5N()));\n",
                                    builderVar, getEntityQuery(entity.getTypeName()), getter.getAccessedName(),
                                    variable.getName(), getter.getSimpleName())
                            .endControlFlow()
                            .build();
                }
                return null;
            }
        }.scan(params);

        if (!predicateBuild.isEmpty()) {
            methodWriter.body()
                    .add("$1T $2N = new $1T();\n", ClassName.bestGuess(BOOLEAN_BUILDER_FQN), builderVar)
                    .add(predicateBuild);
            Parameter pageable = getPageableParameter(overridden);
            if (pageable != null) {
                return methodWriter.body().declareVariable("result", Expressions.of(
                        typeFactory.getType(PAGE_FQN, entityType.getTypeMirror()),
                        CodeBlock.of("$1L.findAll($2N, $3N)",
                                querydslPredicateExecutor, builderVar, pageable.getName())
                ));
            }
            return methodWriter.body().declareVariable("result", Expressions.of(
                    typeFactory.getIterableType(entityType.getTypeMirror()),
                    CodeBlock.of("$1L.findAll($2N)", querydslPredicateExecutor, builderVar)
            ));
        }
        return doQuery(overridden, methodWriter);
    }

    protected CodeBlock getEntityQuery(ClassName entityName) {
        return CodeBlock.of("$1T.$2N",
                ClassName.get(entityName.packageName(), "Q" + entityName.simpleName()),
                StringUtils.uncapitalize(entityName.simpleName())
        );
    }
}
