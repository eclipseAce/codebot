package io.codebot.apt.crud.query;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.*;

import java.util.ArrayList;
import java.util.List;

public class SimpleJpaQuery {
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    public void appendTo(Entity entity,
                         Executable queryMethod,
                         MethodSpec.Builder methodBuilder,
                         NameAllocator nameAllocator) {
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

        TypeFactory typeFactory = entity.getType().getFactory();
        CodeBlock repository = CodeBlock.of("this.repository");
        CodeBlock executor = CodeBlock.of("this.jpaSpecificationExecutor");

        CodeBlock result;
        Type resultType;
        if (queryParams.size() == 1
                && queryParams.get(0).getSimpleName().equals(entity.getIdName())
                && queryParams.get(0).getType().isAssignableTo(entity.getIdType())) {
            result = CodeBlock.of("$1L.getById($2N)", repository, queryParams.get(0).getSimpleName());
            resultType = entity.getType();
            return;
        }

        NameAllocator localNameAllocator = nameAllocator.clone();
        String rootVar = localNameAllocator.newName("root");
        String queryVar = localNameAllocator.newName("query");
        String builderVar = localNameAllocator.newName("builder");
        String predicatesVar = localNameAllocator.newName("predicates");

        CodeBlock.Builder specificationBody = CodeBlock.builder();
        for (Variable param : queryParams) {
            CodeBlock.Builder predicates = CodeBlock.builder();
            collectFromParameter(predicates, predicatesVar, rootVar, queryVar, builderVar, entity, param);
            if (predicates.isEmpty()) {
                for (Executable paramMethod : param.getType().getMethods()) {
                    collectFromParameterMethod(predicates, predicatesVar, rootVar, queryVar, builderVar, entity, param, paramMethod);
                }
            }
            if (predicates.isEmpty()) {
                for (GetAccessor paramGetter : param.getType().getGetters()) {
                    collectFromParameterGetter(predicates, predicatesVar, rootVar, queryVar, builderVar, entity, param, paramGetter);
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

        if (!specificationBody.isEmpty()) {
            CodeBlock specification = CodeBlock.builder()
                    .add("($1N, $2N, $3N) -> {\n$>",
                            rootVar, queryVar, builderVar)
                    .add("$1T<$2T> $3N = new $1T<>();\n",
                            ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar)
                    .add(specificationBody.build())
                    .add("return $1N.and($2N.toArray(new $3T[0]));\n",
                            builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN))
                    .add("$<}")
                    .build();
            if (pageableParam == null) {
                result = CodeBlock.of("$1L.findAll($2L)", executor, specification);
                resultType = typeFactory.getListType(entity.getType().getTypeMirror());
            } else {
                result = CodeBlock.of("$1L.findAll($2L, $3N)", executor, specification, pageableParam.getSimpleName());
                resultType = typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror());
            }
        } else {
            if (pageableParam == null) {
                result = CodeBlock.of("$1L.findAll()", repository);
                resultType = typeFactory.getListType(entity.getType().getTypeMirror());
            } else {
                result = CodeBlock.of("$1L.findAll($2N)", repository, pageableParam.getSimpleName());
                resultType = typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror());
            }
        }
    }

    private void collectFromParameter(CodeBlock.Builder predicates,
                                      String predicatesVar, String rootVar, String queryVar, String builderVar,
                                      Entity entity, Variable param) {
        if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
            predicates.add("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                    predicatesVar, builderVar, rootVar, param.getSimpleName()
            );
        }
    }

    private void collectFromParameterMethod(CodeBlock.Builder predicates,
                                            String predicatesVar, String rootVar, String queryVar, String builderVar,
                                            Entity entity, Variable param, Executable method) {
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
                                            Entity entity, Variable param, GetAccessor getter) {
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
