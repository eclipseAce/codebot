package io.codebot.apt.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Variable;

import java.util.ArrayList;
import java.util.List;

public class SpecificationJpaFindSnippet extends AbstractJpaFindSnippet {
    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private CodeBlock jpaSpecificationExecutor;

    public void setJpaSpecificationExecutor(CodeBlock jpaSpecificationExecutor) {
        this.jpaSpecificationExecutor = jpaSpecificationExecutor;
    }

    @Override
    protected Expression find(CodeBuffer codeBuffer) {
        NameAllocator localNames = codeBuffer.nameAllocator().clone();
        String rootVar = localNames.newName("root");
        String queryVar = localNames.newName("query");
        String builderVar = localNames.newName("builder");
        String predicatesVar = localNames.newName("predicates");

        CodeBlock specificationBody = new ContextVariableScanner() {
            @Override
            public CodeBlock scanVariable(ContextVariable variable) {
                if (getEntity().getType().findGetter(variable.getName(), variable.getType()).isPresent()) {
                    return CodeBlock.of("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                            predicatesVar, builderVar, rootVar, variable.getName());
                }
                return null;
            }

            @Override
            public CodeBlock scanVariableMethod(ContextVariable variable, Executable method) {
                if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
                    return null;
                }
                List<String> formats = Lists.newArrayList();
                List<Object> formatArgs = Lists.newArrayList();
                for (Variable arg : method.getParameters()) {
                    if (arg.getType().isAssignableFrom(ROOT_FQN, getEntity().getType().getTypeMirror())) {
                        formats.add("$N");
                        formatArgs.add(rootVar);
                    } else if (arg.getType().isAssignableFrom(CRITERIA_QUERY_FQN)) {
                        formats.add("$N");
                        formatArgs.add(queryVar);
                    } else if (arg.getType().isAssignableFrom(CRITERIA_BUILDER_FQN)) {
                        formats.add("$N");
                        formatArgs.add(builderVar);
                    } else {
                        return null;
                    }
                }
                return CodeBlock.of("$1N.add($2N.$3N($4L));\n",
                        predicatesVar, variable.getName(), method.getSimpleName(),
                        CodeBlock.of(String.join(", ", formats), formatArgs.toArray(new Object[0])));
            }

            @Override
            public CodeBlock scanVariableGetter(ContextVariable variable, GetAccessor getter) {
                if (getEntity().getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
                    return CodeBlock.builder()
                            .beginControlFlow("if ($1N.$2N() != null)", variable.getName(), getter.getSimpleName())
                            .add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                                    predicatesVar, builderVar, rootVar,
                                    getter.getAccessedName(), variable.getName(), getter.getSimpleName())
                            .endControlFlow()
                            .build();
                }
                return null;
            }
        }.scan(getQueryVariables());

        if (!specificationBody.isEmpty()) {
            CodeBlock specification = CodeBlock.builder()
                    .add("($1N, $2N, $3N) -> {\n$>", rootVar, queryVar, builderVar)
                    .add("$1T<$2T> $3N = new $1T<>();\n",
                            ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar)
                    .add(specificationBody)
                    .add("return $1N.and($2N.toArray(new $3T[0]));\n",
                            builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN))
                    .add("$<}")
                    .build();
            if (getPageableVariableName() != null) {
                return new Expression(
                        CodeBlock.of("$1L.findAll($2L, $3N)",
                                jpaSpecificationExecutor, specification, getPageableVariableName()),
                        getEntity().getType().getFactory().getType(PAGE_FQN, getEntity().getType().getTypeMirror())
                );
            }
            return new Expression(
                    CodeBlock.of("$1L.findAll($2L)", jpaSpecificationExecutor, specification),
                    getEntity().getType().getFactory().getListType(getEntity().getType().getTypeMirror())
            );
        }
        return null;
    }
}
