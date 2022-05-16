package io.codebot.apt.crud.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.crud.Service;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.TypeFactory;
import io.codebot.apt.type.Variable;

import java.util.ArrayList;
import java.util.List;

public class JpaQuery implements Query {
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    @Override
    public Snippet query(Entity entity, Service service, Executable method, NameAllocator names) {
        TypeFactory typeFactory = entity.getType().getFactory();
        if (method.getParameters().size() == 1) {
            Variable theParam = method.getParameters().get(0);
            if (theParam.getSimpleName().equals(entity.getIdName())
                    && theParam.getType().isAssignableTo(entity.getIdType())) {
                return new Snippet(
                        CodeBlock.of(""),
                        CodeBlock.of("this.repository.getById($1N)", theParam.getSimpleName()),
                        entity.getType()
                );
            }
            if (theParam.getType().isAssignableTo(PAGEABLE_FQN)
                    && method.getReturnType().isAssignableFrom(PAGE_FQN)) {
                return new Snippet(
                        CodeBlock.of(""),
                        CodeBlock.of("this.repository.findAll($1N)", theParam.getSimpleName()),
                        typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror())
                );
            }
        }

        NameAllocator localNames = names.clone();
        String rootVar = localNames.newName("root");
        String queryVar = localNames.newName("query");
        String builderVar = localNames.newName("builder");
        String predicatesVar = localNames.newName("predicates");

        CodeBlock.Builder specification = CodeBlock.builder();
        specification.add("($1N, $2N, $3N) -> {\n$>",
                rootVar, queryVar, builderVar
        );
        specification.add("$1T<$2T> $3N = new $1T<>();\n",
                ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar
        );
        Variable pageable = null;
        for (Variable param : method.getParameters()) {
            if (param.getType().isAssignableTo(PAGEABLE_FQN)) {
                pageable = param;
                continue;
            }
            if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
                specification.beginControlFlow("if ($1N != null)", param.getSimpleName());
                specification.add("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                        predicatesVar, builderVar, rootVar, param.getSimpleName()
                );
                specification.endControlFlow();
                continue;
            }
            boolean hasPredicateMethod = false;
            for (Executable paramMethod : param.getType().getMethods()) {
                if (!paramMethod.getReturnType().isAssignableTo(PREDICATE_FQN)) {
                    continue;
                }
                List<String> formats = Lists.newArrayList();
                List<Object> formatArgs = Lists.newArrayList();
                boolean allArgsRecognized = true;
                for (Variable arg : paramMethod.getParameters()) {
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
                        allArgsRecognized = false;
                        break;
                    }
                }
                if (!allArgsRecognized) {
                    continue;
                }
                specification.beginControlFlow("if ($1N != null)", param.getSimpleName());
                specification.add("$1N.add($2N.$3N($4L));\n",
                        predicatesVar, param.getSimpleName(), paramMethod.getSimpleName(),
                        CodeBlock.of(String.join(", ", formats), formatArgs.toArray(new Object[0]))
                );
                specification.endControlFlow();
                hasPredicateMethod = true;
            }
            if (hasPredicateMethod) {
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                if (entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
                    specification.beginControlFlow("if ($1N != null && $1N.$2N() != null)",
                            param.getSimpleName(), getter.getSimpleName()
                    );
                    specification.add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                            predicatesVar, builderVar, rootVar, getter.getAccessedName(),
                            param.getSimpleName(), getter.getSimpleName()
                    );
                    specification.endControlFlow();
                }
            }
        }
        specification.add("return $1N.and($2N.toArray(new $3T[0]));\n",
                builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN)
        );
        specification.add("$<}");

        if (pageable != null) {
            return new Snippet(
                    CodeBlock.of(""),
                    CodeBlock.of("this.specificationExecutor.findAll($1L, $2N)",
                            specification.build(), pageable.getSimpleName()
                    ),
                    typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror())
            );
        }
        return new Snippet(
                CodeBlock.of(""),
                CodeBlock.of("this.specificationExecutor.findAll($1L)",
                        specification.build()
                ),
                typeFactory.getType(List.class.getName(), entity.getType().getTypeMirror())
        );
    }
}
