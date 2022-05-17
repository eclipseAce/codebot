package io.codebot.apt.crud.autocode;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

public class SimpleJpaQueryCodeFactory implements CodeFactory<Code> {
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private Entity entity;
    private List<Variable> parameters;
    private Variable pageableParameter;
    private Supplier<CodeBlock> repositorySupplier;

    @Override
    public Code getCode() {
        if (parameters.size() == 1
                && parameters.get(0).getSimpleName().equals(entity.getIdName())
                && parameters.get(0).getType().isAssignableTo(entity.getIdType())) {
            return findById(parameters.get(0));
        }

        CodeBlock code = analyzeParameters();
        if (!code.isEmpty()) {
            CodeBlock specification = CodeBlock.builder().addNamed(
                    "($root:N, $query:N, $builder:N) -> {\n$>"
                            + "$listType:T<$predicateType:T> $predicates:N = new $listType:T<>();\n"
                            + "$code:L"
                            + "return $builder:N.and($predicates:N.toArray(new $predicateType:T[0]));\n"
                            + "$<}",
                    new HashMap<String, Object>() {{
                        put("root", names.get("root"));
                        put("query", names.get("query"));
                        put("builder", names.get("builder"));
                        put("predicates", names.get("predicates"));
                        put("listType", ArrayList.class);
                        put("predicateType", ClassName.bestGuess(PREDICATE_FQN));
                        put("code", code);
                    }}
            ).build();
            return findAll(specification, pageableParameter);
        } else {
            return findAll(null, pageableParameter);
        }
    }

    protected Code findById(Variable idParam) {
        return new QueryCode(
                CodeBlock.of(""),
                CodeBlock.of("$1L.getById($2N)", repositorySupplier.get(), idParam.getSimpleName()),
                entity.getType()
        );
    }

    protected Code findAll(CodeBlock statements, CodeBlock expression, Variable pageableParam) {

    }

    protected CodeBlock analyzeParameters() {
        CodeBlock.Builder code = CodeBlock.builder();
        for (Variable param : parameters) {
            CodeBlock.Builder paramCode = CodeBlock.builder();
            if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
                paramCode.add(analyzeParameter(param));
            }
            if (paramCode.isEmpty()) {
                paramCode.add(analyzeParameterMethods(param));
            }
            if (paramCode.isEmpty()) {
                paramCode.add(analyzeParameterGetters(param));
            }
            code.add(paramCode.build());
        }
        return code.build();
    }

    protected CodeBlock analyzeParameter(Variable param) {
        return CodeBlock.of("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                names.get("predicates"), names.get("builder"), names.get("root"), param.getSimpleName()
        );
    }

    protected CodeBlock analyzeParameterMethods(Variable param) {
        CodeBlock.Builder code = CodeBlock.builder();
        for (Executable method : param.getType().getMethods()) {
            code.add(analyzeParameterMethod(param, method));
        }
        return code.build();
    }

    protected CodeBlock analyzeParameterMethod(Variable param, Executable method) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return CodeBlock.of("");
        }
        List<String> formats = Lists.newArrayList();
        List<Object> formatArgs = Lists.newArrayList();
        for (Variable arg : method.getParameters()) {
            if (arg.getType().isAssignableFrom(ROOT_FQN, entity.getType().getTypeMirror())) {
                formats.add("$N");
                formatArgs.add(names.get("root"));
            } else if (arg.getType().isAssignableFrom(CRITERIA_QUERY_FQN)) {
                formats.add("$N");
                formatArgs.add(names.get("query"));
            } else if (arg.getType().isAssignableFrom(CRITERIA_BUILDER_FQN)) {
                formats.add("$N");
                formatArgs.add(names.get("builder"));
            } else {
                return CodeBlock.of("");
            }
        }
        return CodeBlock.of("$1N.add($2N.$3N($4L));\n",
                names.get("predicates"), param.getSimpleName(), method.getSimpleName(),
                CodeBlock.of(String.join(", ", formats), formatArgs.toArray(new Object[0]))
        );
    }

    protected CodeBlock analyzeParameterGetters(Variable param) {
        CodeBlock.Builder code = CodeBlock.builder();
        for (GetAccessor getter : param.getType().getGetters()) {
            if (entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
                code.add(analyzeParameterGetter(param, getter));
            }
        }
        return code.build();
    }

    protected CodeBlock analyzeParameterGetter(Variable param, GetAccessor getter) {
        return CodeBlock.builder()
                .beginControlFlow("if ($1N.$2N() != null)", param.getSimpleName(), getter.getSimpleName())
                .add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                        names.get("predicates"), names.get("builder"), names.get("root"),
                        getter.getAccessedName(), param.getSimpleName(), getter.getSimpleName()
                )
                .endControlFlow();
    }

    public static class QueryCode implements Code {
        private final CodeBlock statements;
        private final CodeBlock expression;
        private final Type expressionType;

        public QueryCode(CodeBlock statements, CodeBlock expression, Type expressionType) {
            this.statements = statements;
            this.expression = expression;
            this.expressionType = expressionType;
        }

        @Override
        public void appendTo(CodeBlock.Builder codeBuilder) {

        }
    }
}
