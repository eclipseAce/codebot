package io.codebot.apt.crud.coding;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.Variable;

import java.util.ArrayList;
import java.util.List;

public class JpaSpecification extends LocalVariableScanner {
    private static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";
    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private final MethodBodyContext context;
    private final Entity entity;
    private final List<LocalVariable> variables;

    private String rootVar;
    private String queryVar;
    private String builderVar;
    private String predicatesVar;

    public JpaSpecification(MethodBodyContext context, Entity entity, List<LocalVariable> variables) {
        this.context = context;
        this.entity = entity;
        this.variables = variables;
    }

    public Expression createExpression() {
        NameAllocator nameAlloc = context.getNameAllocator().clone();
        rootVar = nameAlloc.newName("root");
        queryVar = nameAlloc.newName("query");
        builderVar = nameAlloc.newName("builder");
        predicatesVar = nameAlloc.newName("predicates");

        CodeBlock body = scan(variables);
        if (body.isEmpty()) {
            return null;
        }
        CodeBlock expression = CodeBlock.builder()
                .add("($1N, $2N, $3N) -> {\n$>", rootVar, queryVar, builderVar)
                .add("$1T<$2T> $3N = new $1T<>();\n",
                        ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar)
                .add(body)
                .add("return $1N.and($2N.toArray(new $3T[0]));\n",
                        builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN))
                .add("$<}")
                .build();
        Type expressionType = entity.getType().getFactory()
                .getType(SPECIFICATION_FQN, entity.getType().getTypeMirror());
        return new Expression(expression, expressionType);
    }

    @Override
    protected CodeBlock scanVariable(LocalVariable variable) {
        if (!entity.getType().findGetter(variable.getName(), variable.getType()).isPresent()) {
            return null;
        }
        return CodeBlock.of("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                predicatesVar, builderVar, rootVar, variable.getName());
    }

    @Override
    protected CodeBlock scanVariableMethod(LocalVariable variable, Executable method) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return null;
        }
        List<String> formats = Lists.newArrayList();
        List<Object> formatArgs = Lists.newArrayList();
        for (Variable arg : method.getParameters()) {
            if (arg.getType().isAssignableFrom(ROOT_FQN, entity.getType().getTypeMirror())) {
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
    protected CodeBlock scanVariableGetter(LocalVariable variable, GetAccessor getter) {
        if (!entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
            return null;
        }
        return CodeBlock.builder()
                .beginControlFlow("if ($1N.$2N() != null)", variable.getName(), getter.getSimpleName())
                .add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                        predicatesVar, builderVar, rootVar,
                        getter.getAccessedName(), variable.getName(), getter.getSimpleName())
                .endControlFlow()
                .build();
    }
}
