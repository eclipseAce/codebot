package io.cruder.apt.model.processor;

import com.google.common.collect.Lists;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeSpec;
import io.cruder.apt.model.*;
import io.cruder.apt.type.Accessor;
import io.cruder.apt.type.Type;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadingMethodProcessor implements MethodProcessor {
    private static final String SPECIFICATION_FQN = "org.springframework.data.jpa.domain.Specification";
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    @Override
    public void process(Service service, TypeSpec.Builder serviceBuilder,
                        Method method, MethodSpec.Builder methodBuilder,
                        NameAllocator nameAlloc) {
        if (!method.getSimpleName().startsWith("find")) {
            return;
        }
        Entity entity = service.getEntity();

        List<QueryParameter> queries = Lists.newArrayList();
        List<MethodParameter> specifications = Lists.newArrayList();
        List<MethodParameter> pageables = Lists.newArrayList();

        for (MethodParameter param : method.getParameters()) {
            Optional<Accessor> directGetter = entity.getType()
                    .findReadAccessor(param.getName(), param.getType().asTypeMirror());
            if (directGetter.isPresent()) {
                queries.add(new QueryParameter(param.getName(), null));
                continue;
            }
            if (param.getType().isSubtype(SPECIFICATION_FQN, entity.getType().asTypeMirror())) {
                specifications.add(param);
                continue;
            }
            if (param.getType().isSubtype(PAGEABLE_FQN)) {
                pageables.add(param);
                continue;
            }
            for (Accessor getter : param.getType().findReadAccessors()) {
                Optional<Accessor> entityGetter = entity.getType()
                        .findReadAccessor(getter.getAccessedName(), getter.getAccessedType());
                if (entityGetter.isPresent()) {
                    queries.add(new QueryParameter(param.getName(), getter));
                }
            }
        }
        String resultVar = nameAlloc.newName("result");
        Type resultType = entity.getType();
        if (queries.size() == 1 && specifications.isEmpty() && queries.get(0).name.equals(entity.getIdName())) {
            methodBuilder.addStatement(
                    "$1T $2N = repository.findById($3L).orElse(null)",
                    entity.getTypeName(), resultVar, queries.get(0).getExpression()
            );
        } else {
            String specRootVar = nameAlloc.newName("root");
            String specQueryVar = nameAlloc.newName("query");
            String specBuilderVar = nameAlloc.newName("builder");

            CodeBlock.Builder specBuilder = CodeBlock.builder();
            specBuilder.add(
                    "($1N, $2N, $3N) -> $3N.and(\n$>",
                    specRootVar, specQueryVar, specBuilderVar
            );
            specBuilder.add(CodeBlock.join(Stream.of(
                    queries.stream().map(query -> CodeBlock.of(
                            "$1N.equal($2N.get($3S), $4L)",
                            specBuilderVar, specRootVar, query.name, query.getExpression()
                    )),
                    specifications.stream().map(spec -> CodeBlock.of(
                            "$1N.toPredicate($2N, $3N, $4N)",
                            spec.getName(), specRootVar, specQueryVar, specBuilderVar
                    ))
            ).flatMap(Function.identity()).collect(Collectors.toList()), ",\n"));
            specBuilder.add("\n$<)");

            if (!pageables.isEmpty() && method.getReturnType().erasure().isAssignableFrom(PAGE_FQN)) {
                resultType = entity.getType().getFactory()
                        .getType(PAGE_FQN, entity.getType().asTypeMirror());
                methodBuilder.addCode(
                        "$1T $2N = repository.findAll($3L, $4N);\n",
                        resultType.asTypeMirror(), resultVar, specBuilder.build(), pageables.get(0).getName()
                );
            } else {
                methodBuilder.addCode(
                        "$1T $2N = repository.findOne($3L).orElse(null);\n",
                        resultType.asTypeMirror(), resultVar, specBuilder.build()
                );
            }
        }

        CodeUtils.mapFromEntityAndReturn(
                resultType, resultVar, method.getReturnType(), entity, nameAlloc
        ).forEach(methodBuilder::addCode);
    }

    static class QueryParameter {
        final String name;
        final Accessor accessor;

        public QueryParameter(String name, Accessor accessor) {
            this.name = name;
            this.accessor = accessor;
        }

        public CodeBlock getExpression() {
            return accessor == null
                    ? CodeBlock.of("$N", name)
                    : CodeBlock.of("$1N.$2N()", name, accessor.getSimpleName());
        }
    }
}
