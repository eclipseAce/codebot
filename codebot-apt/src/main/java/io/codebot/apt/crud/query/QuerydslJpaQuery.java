package io.codebot.apt.crud.query;

import com.google.common.collect.Lists;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.TypeFactory;
import io.codebot.apt.type.Variable;

import java.util.List;

public class QuerydslJpaQuery {
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    public Expression getQueryExpression(Entity entity,
                                         Executable queryMethod,
                                         NameAllocator nameAllocator,
                                         CodeBlock.Builder context) {
        CodeBlock repository = CodeBlock.of("this.repository");
        CodeBlock executor = CodeBlock.of("this.querydslPredicateExecutor");

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

        if (queryParams.size() == 1
                && queryParams.get(0).getSimpleName().equals(entity.getIdName())
                && queryParams.get(0).getType().isAssignableTo(entity.getIdType())) {
            return new Expression(
                    CodeBlock.of("$1L.getById($2N)", repository, queryParams.get(0).getSimpleName()),
                    entity.getType()
            );
        }

        Expression predicate = new QuerydslPredicateFactory()
                .getExpression(queryParams, entity, nameAllocator, context);

        TypeFactory typeFactory = entity.getType().getFactory();
        if (pageableParam != null) {
            return new Expression(
                    predicate != null
                            ? CodeBlock.of("$1L.findAll($2L, $3N)", executor, predicate.getExpression(), pageableParam.getSimpleName())
                            : CodeBlock.of("$1L.findAll($2N)", repository, pageableParam.getSimpleName()),
                    typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror())
            );
        } else {
            return new Expression(
                    predicate != null
                            ? CodeBlock.of("$1L.findAll($2L)", executor, predicate.getExpression())
                            : CodeBlock.of("$1L.findAll()", repository),
                    typeFactory.getIterableType(entity.getType().getTypeMirror())
            );
        }
    }
}
