package io.codebot.apt.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JpaFilterSnippet extends FilterSnippet {
    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";
    private static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";

    private String rootVar;
    private String criteriaQueryVar;
    private String criteriaBuilderVar;
    private String predicateListVar;

    public JpaFilterSnippet(Entity entity, List<Variable> parameters, String variableName) {
        super(entity, parameters, variableName);
    }

    @Override
    public void appendTo(CodeBlock.Builder code, NameAllocator nameAllocator) {
        NameAllocator localNameAllocator = nameAllocator.clone();
        rootVar = localNameAllocator.newName("root");
        criteriaQueryVar = localNameAllocator.newName("query");
        criteriaBuilderVar = localNameAllocator.newName("builder");
        predicateListVar = localNameAllocator.newName("predicates");

        CodeBlock specificationBody = fromParameters();
        if (specificationBody.isEmpty()) {
            return;
        }

        Map<String, Object> args = new HashMap<String, Object>() {{
            put("spec", variableName);
            put("root", rootVar);
            put("query", criteriaQueryVar);
            put("builder", criteriaBuilderVar);
            put("predicates", predicateListVar);
            put("listType", ArrayList.class);
            put("specType", ClassName.bestGuess(SPECIFICATION_FQN));
            put("predicateType", ClassName.bestGuess(PREDICATE_FQN));
            put("entityType", entity.getTypeName());
            put("specBody", specificationBody);
        }};
        String format = "" +
                "$specType:T<$entityType:T> $spec:N = " +
                "($root:N, $query:N, $builder:N) -> {\n$>" +
                "$listType:T<$predicateType:T> $predicates:N = new $listType:T<>();\n" +
                "$specBody:L" +
                "return $builder:N.and($predicates:N.toArray(new $predicateType:T[0]));\n" +
                "$<};\n";
        code.addNamed(format, args);
    }

    @Override
    protected CodeBlock fromParameter(Variable param) {
        return CodeBlock.of("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                predicateListVar, criteriaBuilderVar, rootVar, param.getSimpleName()
        );
    }

    @Override
    protected CodeBlock fromParameterMethod(Variable param, Executable method) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return CodeBlock.of("");
        }
        List<String> formats = Lists.newArrayList();
        List<Object> formatArgs = Lists.newArrayList();
        for (Variable methodParam : method.getParameters()) {
            if (methodParam.getType().isAssignableFrom(ROOT_FQN, entity.getType().getTypeMirror())) {
                formats.add("$N");
                formatArgs.add(rootVar);
            } else if (methodParam.getType().isAssignableFrom(CRITERIA_QUERY_FQN)) {
                formats.add("$N");
                formatArgs.add(criteriaQueryVar);
            } else if (methodParam.getType().isAssignableFrom(CRITERIA_BUILDER_FQN)) {
                formats.add("$N");
                formatArgs.add(criteriaBuilderVar);
            } else {
                return CodeBlock.of("");
            }
        }
        return CodeBlock.of("$1N.add($2N.$3N($4L));\n",
                predicateListVar, param.getSimpleName(), method.getSimpleName(),
                CodeBlock.of(String.join(", ", formats), formatArgs.toArray(new Object[0]))
        );
    }

    @Override
    protected CodeBlock fromParameterGetter(Variable param, GetAccessor getter) {
        return CodeBlock.builder()
                .beginControlFlow("if ($1N.$2N() != null)", param.getSimpleName(), getter.getSimpleName())
                .add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                        predicateListVar, criteriaBuilderVar, rootVar,
                        getter.getAccessedName(), param.getSimpleName(), getter.getSimpleName()
                )
                .endControlFlow()
                .build();
    }
}
