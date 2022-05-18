package io.codebot.apt.crud.query;

import com.google.common.collect.Lists;
import com.squareup.javapoet.*;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Variable;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class QuerydslJpaQuery {
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    private static final String QUERYDSL_PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_FQN = "com.querydsl.core.types.EntityPath";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";

    public void appendTo(Entity entity,
                         Executable queryMethod,
                         MethodSpec.Builder methodBuilder,
                         NameAllocator nameAllocator) {
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
            methodBuilder.addCode("$1L.getById($2N);\n", repository, queryParams.get(0).getSimpleName());
            return;
        }

        String builderVar = nameAllocator.newName("builder");

        CodeBlock.Builder build = CodeBlock.builder();
        for (Variable param : queryParams) {
            CodeBlock.Builder predicates = CodeBlock.builder();
            collectFromParameter(predicates, builderVar, entity, param);
            if (predicates.isEmpty()) {
                for (Executable paramMethod : param.getType().getMethods()) {
                    collectFromParameterMethod(predicates, builderVar, entity, param, paramMethod);
                }
            }
            if (predicates.isEmpty()) {
                for (GetAccessor paramGetter : param.getType().getGetters()) {
                    collectFromParameterGetter(predicates, builderVar, entity, param, paramGetter);
                }
            }
            if (!predicates.isEmpty() && !param.getType().isPrimitive()) {
                build.beginControlFlow("if ($1N != null)", param.getSimpleName());
                build.add(predicates.build());
                build.endControlFlow();
            } else {
                build.add(predicates.build());
            }
        }

        if (build.isEmpty()) {
            if (pageableParam == null) {
                methodBuilder.addCode("$1L.findAll();\n", repository);
            } else {
                methodBuilder.addCode("$1L.findAll($2N);\n", repository, pageableParam.getSimpleName());
            }
        } else {
            methodBuilder.addCode("$1T $2N = new $1T();\n", ClassName.bestGuess(BOOLEAN_BUILDER_FQN), builderVar);
            methodBuilder.addCode(build.build());

            if (pageableParam == null) {
                methodBuilder.addCode("$1L.findAll($2L);\n", executor, builderVar);
            } else {
                methodBuilder.addCode("$1L.findAll($2L, $3N);\n", executor, builderVar, pageableParam.getSimpleName());
            }
        }
    }

    private CodeBlock getQueryVar(ClassName entityName) {
        return CodeBlock.of("$1T.$2N",
                ClassName.get(entityName.packageName(), "Q" + entityName.simpleName()),
                StringUtils.uncapitalize(entityName.simpleName())
        );
    }

    private void collectFromParameter(CodeBlock.Builder predicates, String builderVar,
                                      Entity entity, Variable param) {
        if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
            predicates.add("$1N.and($2L.$3N.eq($3N));\n",
                    builderVar, getQueryVar(entity.getTypeName()), param.getSimpleName()
            );
        }
    }

    private void collectFromParameterMethod(CodeBlock.Builder predicates, String builderVar,
                                            Entity entity, Variable param, Executable method) {
        if (!method.getReturnType().isAssignableTo(QUERYDSL_PREDICATE_FQN)) {
            return;
        }
        TypeElement entityPathElement = entity.getType().getFactory()
                .getElementUtils().getTypeElement(ENTITY_PATH_FQN);
        List<CodeBlock> args = Lists.newArrayList();
        for (Variable arg : method.getParameters()) {
            if (arg.getType().isAssignableTo(ENTITY_PATH_FQN)) {
                ClassName entityName = (ClassName) TypeName.get(
                        arg.getType().asMember(entityPathElement.getTypeParameters().get(0))
                );
                args.add(getQueryVar(entityName));
            } else {
                return;
            }
        }
        predicates.add("$1N.and($2N.$3N($4L));\n",
                builderVar, param.getSimpleName(), method.getSimpleName(),
                CodeBlock.join(args, ", ")
        );
    }

    private void collectFromParameterGetter(CodeBlock.Builder predicates, String builderVar,
                                            Entity entity, Variable param, GetAccessor getter) {
        if (entity.getType().findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
            predicates.beginControlFlow("if ($1N.$2N() != null)",
                    param.getSimpleName(), getter.getSimpleName());
            predicates.add("$1N.and($2L.$3N.eq($4N.$5N()));\n",
                    builderVar, getQueryVar(entity.getTypeName()), getter.getAccessedName(),
                    param.getSimpleName(), getter.getSimpleName());
            predicates.endControlFlow();
        }
    }
}
