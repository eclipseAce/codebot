package io.codebot.apt.crud.query;

import com.google.common.collect.ImmutableMap;
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
import java.util.stream.Collectors;

public class JpaQuery {
    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private final Entity entity;
    private final Executable method;
    private final List<Filter> filters;

    public JpaQuery(Entity entity, Executable method) {
        this.entity = entity;
        this.method = method;
        this.filters = getFilters();
    }

    public CodeBlock getExpression(NameAllocator names) {
        if (filters.size() == 1 && filters.get(0) instanceof SimpleFilter) {
            SimpleFilter filter = (SimpleFilter) filters.get(0);
            if (filter.filterName.equals(entity.getIdName())
                    && filter.filterType.isAssignableTo(entity.getIdType())) {
                return CodeBlock.of("this.repository.getById($L)", filter.expression);
            }
        }

        CodeBlock.Builder specification = CodeBlock.builder();

        names = names.clone();
        String rootVar = names.newName("root");
        String queryVar = names.newName("query");
        String builderVar = names.newName("builder");
        String predicatesVar = names.newName("predicates");
        specification.add("($1N, $2N, $3N) -> {\n$>",
                rootVar, queryVar, builderVar
        );
        specification.add("$1T<$2T> $3N = new $1T<>();\n",
                ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar
        );
        for (Filter filter : filters) {
            if (filter instanceof SimpleFilter) {
                SimpleFilter simpleFilter = (SimpleFilter) filter;
            } //
            else if (filter instanceof PredicateFilter) {
                PredicateFilter predicateFilter = (PredicateFilter) filter;
            }
        }
        specification.add("return $1N.and($2N.toArray(new $3T[0]));\n",
                builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN)
        );
        specification.add("$<}");
    }

    private List<Filter> getFilters() {
        List<Filter> filters = Lists.newArrayList();
        for (Variable param : method.getParameters()) {
            if (entity.getType().findGetter(
                    param.getSimpleName(), param.getType()
            ).isPresent()) {
                filters.add(new SimpleFilter(param, null));
                continue;
            }
            for (Executable method : param.getType().getMethods()) {
                if (method instanceof GetAccessor) {
                    GetAccessor getter = (GetAccessor) method;
                    if (entity.getType().findGetter(
                            getter.getAccessedName(), getter.getAccessedType()
                    ).isPresent()) {
                        filters.add(new SimpleFilter(param, getter));
                        continue;
                    }
                }
                if (method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
                    List<String> argLabels = Lists.newArrayList();
                    boolean allArgsRecognized = true;
                    for (Variable p : method.getParameters()) {
                        if (p.getType().isAssignableFrom(ROOT_FQN, entity.getType().getTypeMirror())) {
                            argLabels.add("root");
                        } else if (p.getType().isAssignableFrom(CRITERIA_QUERY_FQN)) {
                            argLabels.add("query");
                        } else if (p.getType().isAssignableFrom(CRITERIA_BUILDER_FQN)) {
                            argLabels.add("builder");
                        } else {
                            allArgsRecognized = false;
                            break;
                        }
                    }
                    if (allArgsRecognized) {
                        filters.add(new Filter(FilterKind.PREDICATE, param, method));
                        continue;
                    }
                }
            }
        }
        return filters;
    }

    private interface Filter {
    }

    private static class SimpleFilter implements Filter {
        final Variable parameter;
        final GetAccessor getter;
        final String filterName;
        final Type filterType;
        final CodeBlock expression;

        SimpleFilter(Variable parameter, GetAccessor getter) {
            this.parameter = parameter;
            this.getter = getter;
            if (getter != null) {
                this.filterName = getter.getAccessedName();
                this.filterType = getter.getAccessedType();
                this.expression = CodeBlock.of("$1N.$2N()", parameter.getSimpleName(), getter.getSimpleName());
            } else {
                this.filterName = parameter.getSimpleName();
                this.filterType = parameter.getType();
                this.expression = CodeBlock.of("$N", parameter.getSimpleName());
            }
        }
    }

    private static class PredicateFilter implements Filter {
        final Variable parameter;
        final Executable method;
        final List<String> argLabels;

        PredicateFilter(Variable parameter, Executable method, List<String> argLabels) {
            this.parameter = parameter;
            this.method = method;
            this.argLabels = argLabels;
        }

        CodeBlock getExpression(String rootVar, String queryVar, String builderVar) {
            String argsFormat = argLabels.stream()
                    .map(it -> "$" + it + ":N")
                    .collect(Collectors.joining(", "));
            return CodeBlock.builder()
                    .add("$1N.$2N($3L)",
                            parameter.getSimpleName(),
                            method.getSimpleName(),
                            CodeBlock.of(argsFormat, ImmutableMap.of(
                                    "root", rootVar,
                                    "query", queryVar,
                                    "builder", builderVar
                            ))
                    )
                    .build();
        }
    }
}
