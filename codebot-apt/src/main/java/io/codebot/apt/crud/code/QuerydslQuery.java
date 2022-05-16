package io.codebot.apt.crud.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.crud.Service;
import io.codebot.apt.type.*;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class QuerydslQuery implements Query {
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_FQN = "com.querydsl.core.types.EntityPath";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";

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

        TypeElement entityPathBaseElement = typeFactory.getElementUtils()
                .getTypeElement(ENTITY_PATH_FQN);

        String builderVar = names.newName("builder");
        CodeBlock query = CodeBlock.of("$1T.$2N",
                ClassName.get(
                        entity.getTypeName().packageName(),
                        "Q" + entity.getTypeName().simpleName()
                ),
                StringUtils.uncapitalize(entity.getTypeName().simpleName())
        );
        CodeBlock.Builder predicate = CodeBlock.builder();
        predicate.add("$1T $2N = new $1T();\n",
                ClassName.bestGuess(BOOLEAN_BUILDER_FQN), builderVar
        );
        Variable pageable = null;
        for (Variable param : method.getParameters()) {
            if (param.getType().isAssignableTo(PAGEABLE_FQN)) {
                pageable = param;
                continue;
            }
            if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
                predicate.beginControlFlow("if ($1N != null)", param.getSimpleName());
                predicate.add("$1N.and($2L.$3N.eq($3N));\n",
                        builderVar, query, param.getSimpleName()
                );
                predicate.endControlFlow();
                continue;
            }
            boolean hasPredicateMethod = false;
            for (Executable paramMethod : param.getType().getMethods()) {
                if (!paramMethod.getReturnType().isAssignableTo(PREDICATE_FQN)) {
                    continue;
                }
                List<CodeBlock> args = Lists.newArrayList();
                boolean allArgsRecognized = true;
                for (Variable arg : paramMethod.getParameters()) {
                    if (arg.getType().isAssignableTo(entityPathBaseElement)) {
                        Type typeArg = typeFactory.getType(
                                arg.getType().asMember(entityPathBaseElement.getTypeParameters().get(0))
                        );
                        ClassName typeArgName = ClassName.get(typeArg.asTypeElement());
                        ClassName typeArgQueryName = ClassName.get(
                                typeArgName.packageName(), "Q" + typeArgName.simpleName()
                        );
                        args.add(CodeBlock.of("$1T.$2N",
                                typeArgQueryName, StringUtils.uncapitalize(typeArgName.simpleName()))
                        );
                    } else {
                        allArgsRecognized = false;
                        break;
                    }
                }
                if (!allArgsRecognized) {
                    continue;
                }
                predicate.beginControlFlow("if ($1N != null)", param.getSimpleName());
                predicate.add("$1N.and($2N.$3N($4L));\n",
                        builderVar, param.getSimpleName(), paramMethod.getSimpleName(),
                        CodeBlock.join(args, ", ")
                );
                predicate.endControlFlow();
                hasPredicateMethod = true;
            }
            if (hasPredicateMethod) {
                continue;
            }
            for (GetAccessor getter : param.getType().getGetters()) {
                if (entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
                    predicate.beginControlFlow("if ($1N != null && $1N.$2N() != null)",
                            param.getSimpleName(), getter.getSimpleName()
                    );
                    predicate.add("$1N.and($2L.$3N.eq($4N.$5N()));\n",
                            builderVar, query, getter.getAccessedName(), param.getSimpleName(), getter.getSimpleName()
                    );
                    predicate.endControlFlow();
                }
            }
        }
        if (pageable != null) {
            return new Snippet(
                    predicate.build(),
                    CodeBlock.of("this.querydslPredicateExecutor.findAll($1N, $2N)",
                            builderVar, pageable.getSimpleName()
                    ),
                    typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror())
            );
        }
        return new Snippet(
                predicate.build(),
                CodeBlock.of("this.querydslPredicateExecutor.findAll($1N)",
                        builderVar
                ),
                typeFactory.getType(Iterable.class.getName(), entity.getType().getTypeMirror())
        );
    }
}
