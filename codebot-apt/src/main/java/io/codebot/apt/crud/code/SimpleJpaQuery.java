package io.codebot.apt.crud.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.crud.Service;
import io.codebot.apt.type.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimpleJpaQuery implements Query {
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    @Override
    public Snippet query(Entity entity, Service service, Executable method, NameAllocator names) {
        TypeFactory typeFactory = entity.getType().getFactory();

        List<Variable> params = Lists.newArrayList();
        List<Variable> pageables = Lists.newArrayList();
        method.getParameters().forEach(it -> {
            if (it.getType().isAssignableTo(PAGEABLE_FQN)) {
                pageables.add(it);
            } else {
                params.add(it);
            }
        });

        CodeBlock repository = CodeBlock.of("this.repository");
        CodeBlock executor = CodeBlock.of("this.specificationExecutor");

        CodeBlock.Builder statements = CodeBlock.builder();
        CodeBlock.Builder expression = CodeBlock.builder();
        Type expressionType;

        if (params.size() == 1
                && params.get(0).getSimpleName().equals(entity.getIdName())
                && params.get(0).getType().isAssignableTo(entity.getIdType())) {
            expression.add("$1L.getById($2N)", repository, params.get(0).getSimpleName());
            expressionType = entity.getType();
        } //
        else {
            CodeBlock.Builder specificationBody = CodeBlock.builder();

            NameAllocator localNames = names.clone();
            localNames.newName("root", "root");
            localNames.newName("query", "query");
            localNames.newName("builder", "builder");
            localNames.newName("predicates", "predicates");

            for (Variable param : params) {
                CodeBlock.Builder build = CodeBlock.builder();

                if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
                    analyzeParameter(entity, param, localNames, build);
                }

                if (build.isEmpty()) {
                    for (Executable paramMethod : param.getType().getMethods()) {
                        analyzeMethods(entity, param, paramMethod, localNames, build);
                    }
                }

                if (build.isEmpty()) {
                    for (GetAccessor getter : param.getType().getGetters()) {
                        if (entity.getType().findGetter(
                                getter.getAccessedName(), getter.getAccessedType()
                        ).isPresent()) {
                            analyzeGetters(entity, param, getter, localNames, build);
                        }
                    }
                }

                if (!build.isEmpty()) {
                    specificationBody
                            .beginControlFlow("if ($1N != null)", param.getSimpleName())
                            .add(build.build())
                            .endControlFlow();
                }
            }
            if (!specificationBody.isEmpty()) {
                CodeBlock specification = CodeBlock.builder().addNamed(
                        "($root:N, $query:N, $builder:N) -> {\n$>"
                                + "$listType:T<$predicateType:T> $predicates:N = new $listType:T<>();\n"
                                + "return $builder:N.and($predicates:N.toArray(new $predicateType:T[0]));\n"
                                + "$<}",
                        new HashMap<String, Object>() {{
                            put("root", localNames.get("root"));
                            put("query", localNames.get("query"));
                            put("builder", localNames.get("builder"));
                            put("predicates", localNames.get("predicates"));
                            put("listType", ArrayList.class);
                            put("predicateType", ClassName.bestGuess(PREDICATE_FQN));
                        }}
                ).build();

                if (pageables.isEmpty()) {
                    expression.add("$1L.findAll($2L)", executor, specification);
                    expressionType = typeFactory.getType(Iterable.class.getName(), entity.getType().getTypeMirror());
                } else {
                    expression.add("$1L.findAll($2L, $3N)", executor, specification, pageables.get(0).getSimpleName());
                    expressionType = typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror());
                }
            } else {
                if (pageables.isEmpty()) {
                    expression.add("$1L.findAll()", repository);
                    expressionType = typeFactory.getType(List.class.getName(), entity.getType().getTypeMirror());
                } else {
                    expression.add("$1L.findAll($2N)", repository, pageables.get(0).getSimpleName());
                    expressionType = typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror());
                }
            }
        }
        return new Snippet(statements.build(), expression.build(), expressionType);
    }

    private void analyzeParameter(Entity entity,
                                  Variable param,
                                  NameAllocator names,
                                  CodeBlock.Builder code) {
        code.add("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                names.get("predicates"), names.get("builder"), names.get("root"), param.getSimpleName()
        );
    }

    private void analyzeMethods(Entity entity,
                                Variable param,
                                Executable method,
                                NameAllocator names,
                                CodeBlock.Builder code) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return;
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
                return;
            }
        }
        code.add("$1N.add($2N.$3N($4L));\n",
                names.get("predicates"), param.getSimpleName(), method.getSimpleName(),
                CodeBlock.of(String.join(", ", formats), formatArgs.toArray(new Object[0]))
        );
    }

    private void analyzeGetters(Entity entity,
                                Variable param,
                                GetAccessor getter,
                                NameAllocator names,
                                CodeBlock.Builder code) {
        code.beginControlFlow("if ($1N.$2N() != null)",
                param.getSimpleName(), getter.getSimpleName()
        );
        code.add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                names.get("predicates"), names.get("builder"), names.get("root"),
                getter.getAccessedName(), param.getSimpleName(), getter.getSimpleName()
        );
        code.endControlFlow();
    }
}
