package io.codebot.apt.crud.query;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Variable;

import java.util.ArrayList;
import java.util.List;

public class JpaSpecificationFactory {
    private static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";

    private static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    private static final String ROOT_FQN = "javax.persistence.criteria.Root";
    private static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    private static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private Entity entity;
    private String rootVar;
    private String queryVar;
    private String builderVar;
    private String predicatesVar;

    public Expression getExpression(List<Variable> params, Entity entity, NameAllocator nameAllocator) {
        NameAllocator localNameAllocator = nameAllocator.clone();
        this.entity = entity;
        this.rootVar = localNameAllocator.newName("root");
        this.queryVar = localNameAllocator.newName("query");
        this.builderVar = localNameAllocator.newName("builder");
        this.predicatesVar = localNameAllocator.newName("predicates");

        CodeBlock.Builder specBody = CodeBlock.builder();
        for (Variable param : params) {
            CodeBlock.Builder builder = CodeBlock.builder();

            build(builder, param);
            if (builder.isEmpty()) {
                param.getType().getMethods().forEach(method -> build(builder, param, method));
            }
            if (builder.isEmpty()) {
                param.getType().getGetters().forEach(getter -> build(builder, param, getter));
            }
            if (!builder.isEmpty() && !param.getType().isPrimitive()) {
                specBody.beginControlFlow("if ($1N != null)", param.getSimpleName());
                specBody.add(builder.build());
                specBody.endControlFlow();
            } else {
                specBody.add(builder.build());
            }
        }
        if (specBody.isEmpty()) {
            return null;
        }
        return new Expression(
                CodeBlock.builder()
                        .add("($1N, $2N, $3N) -> {\n$>", rootVar, queryVar, builderVar)
                        .add("$1T<$2T> $3N = new $1T<>();\n",
                                ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar)
                        .add(specBody.build())
                        .add("return $1N.and($2N.toArray(new $3T[0]));\n",
                                builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN))
                        .add("$<}")
                        .build(),
                entity.getType().getFactory().getType(SPECIFICATION_FQN, entity.getType().getTypeMirror())
        );
    }

    private void build(CodeBlock.Builder builder, Variable param) {
        if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
            builder.add("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                    predicatesVar, builderVar, rootVar, param.getSimpleName());
        }
    }

    private void build(CodeBlock.Builder builder, Variable param, Executable method) {
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
        builder.add("$1N.add($2N.$3N($4L));\n",
                predicatesVar, param.getSimpleName(), method.getSimpleName(),
                CodeBlock.of(String.join(", ", formats), formatArgs.toArray(new Object[0])));
    }

    private void build(CodeBlock.Builder builder, Variable param, GetAccessor getter) {
        if (entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
            builder.beginControlFlow("if ($1N.$2N() != null)",
                    param.getSimpleName(), getter.getSimpleName());
            builder.add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                    predicatesVar, builderVar, rootVar,
                    getter.getAccessedName(), param.getSimpleName(), getter.getSimpleName());
            builder.endControlFlow();
        }
    }
}
