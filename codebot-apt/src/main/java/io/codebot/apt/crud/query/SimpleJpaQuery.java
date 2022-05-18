package io.codebot.apt.crud.query;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Variable;

import java.util.ArrayList;
import java.util.List;

public class SimpleJpaQuery {
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private final Entity entity;
    private final Executable queryMethod;
    private final List<Variable> queryParameters;
    private final Variable pageableParameter;
    private final Variable singleIdParameter;

    private final CodeBlock repository = CodeBlock.of("this.repository");
    private final CodeBlock executor = CodeBlock.of("this.jpaSpecificationExecutor");

    public SimpleJpaQuery(Entity entity, Executable queryMethod) {
        this.entity = entity;
        this.queryMethod = queryMethod;

        List<Variable> queryParams = Lists.newArrayList();
        Variable pageableParam = null;
        for (Variable param : queryMethod.getParameters()) {
            if (param.getType().isAssignableTo(PAGEABLE_FQN)) {
                if (pageableParam == null) {
                    pageableParam = param;
                }
                continue;
            }
            queryParams.add(param);
        }
        this.queryParameters = ImmutableList.copyOf(queryParams);
        this.pageableParameter = pageableParam;

        if (queryParameters.size() == 1
                && queryParameters.get(0).getSimpleName().equals(entity.getIdName())
                && queryParameters.get(0).getType().isAssignableTo(entity.getIdType())) {
            this.singleIdParameter = queryParameters.get(0);
        } else {
            this.singleIdParameter = null;
        }
    }

    public void appendTo(MethodSpec.Builder methodBuilder, NameAllocator nameAllocator) {
        if (singleIdParameter != null) {
            methodBuilder.addCode("$1L.getById($2N);\n", repository, singleIdParameter.getSimpleName());
            return;
        }

        NameAllocator localNameAllocator = nameAllocator.clone();
        String rootVar = localNameAllocator.newName("root");
        String queryVar = localNameAllocator.newName("query");
        String builderVar = localNameAllocator.newName("builder");
        String predicatesVar = localNameAllocator.newName("predicates");

        CodeBlock.Builder specificationBody = CodeBlock.builder();
        for (Variable param : queryParameters) {
            CodeBlock.Builder predicates = CodeBlock.builder();
            collectFromParameter(predicates, predicatesVar, rootVar, queryVar, builderVar, param);
            if (predicates.isEmpty()) {
                for (Executable paramMethod : param.getType().getMethods()) {
                    collectFromParameterMethod(predicates, predicatesVar, rootVar, queryVar, builderVar, param, paramMethod);
                }
            }
            if (predicates.isEmpty()) {
                for (GetAccessor paramGetter : param.getType().getGetters()) {
                    collectFromParameterGetter(predicates, predicatesVar, rootVar, queryVar, builderVar, param, paramGetter);
                }
            }
            if (!predicates.isEmpty() && !param.getType().isPrimitive()) {
                specificationBody.beginControlFlow("if ($1N != null)", param.getSimpleName());
                specificationBody.add(predicates.build());
                specificationBody.endControlFlow();
            } else {
                specificationBody.add(predicates.build());
            }
        }

        CodeBlock.Builder specification = CodeBlock.builder();
        if (!specificationBody.isEmpty()) {
            specification.add("($1N, $2N, $3N) -> {\n$>",
                    rootVar, queryVar, builderVar);
            specification.add("$1T<$2T> $3N = new $1T<>();\n",
                    ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar);
            specification.add(specificationBody.build());
            specification.add("return $1N.and($2N.toArray(new $3T[0]));\n",
                    builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN));
            specification.add("$<}");
        }

        if (specification.isEmpty()) {
            if (pageableParameter == null) {
                methodBuilder.addCode("$1L.findAll();\n", repository);
            } else {
                methodBuilder.addCode("$1L.findAll($2N);\n", repository, pageableParameter.getSimpleName());
            }
        } else {
            if (pageableParameter == null) {
                methodBuilder.addCode("$1L.findAll($2L);\n", executor, specification.build());
            } else {
                methodBuilder.addCode("$1L.findAll($2L, $3N);\n", executor, specification.build(), pageableParameter.getSimpleName());
            }
        }
    }

    private void collectFromParameter(CodeBlock.Builder predicates,
                                      String predicatesVar, String rootVar, String queryVar, String builderVar,
                                      Variable param) {
        if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
            predicates.add("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                    predicatesVar, builderVar, rootVar, param.getSimpleName()
            );
        }
    }

    private void collectFromParameterMethod(CodeBlock.Builder predicates,
                                            String predicatesVar, String rootVar, String queryVar, String builderVar,
                                            Variable param, Executable method) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return;
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
                return;
            }
        }
        predicates.add("$1N.add($2N.$3N($4L));\n",
                predicatesVar, param.getSimpleName(), method.getSimpleName(),
                CodeBlock.of(String.join(", ", formats), formatArgs.toArray(new Object[0]))
        );
    }

    private void collectFromParameterGetter(CodeBlock.Builder predicates,
                                            String predicatesVar, String rootVar, String queryVar, String builderVar,
                                            Variable param, GetAccessor getter) {
        if (entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
            predicates.beginControlFlow("if ($1N.$2N() != null)",
                    param.getSimpleName(), getter.getSimpleName()
            );
            predicates.add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                    predicatesVar, builderVar, rootVar,
                    getter.getAccessedName(), param.getSimpleName(), getter.getSimpleName()
            );
            predicates.endControlFlow();
        }
    }
}
