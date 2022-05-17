package io.codebot.apt.crud.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeName;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.crud.Service;
import io.codebot.apt.type.*;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class QuerydslJpaQuery implements Query {
    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    private static final String PREDICATE_FQN = "com.querydsl.core.types.Predicate";
    private static final String ENTITY_PATH_FQN = "com.querydsl.core.types.EntityPath";
    private static final String BOOLEAN_BUILDER_FQN = "com.querydsl.core.BooleanBuilder";

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
        CodeBlock executor = CodeBlock.of("this.querydslPredicateExecutor");

        CodeBlock.Builder statements = CodeBlock.builder();
        CodeBlock.Builder expression = CodeBlock.builder();
        Type expressionType;

        if (params.isEmpty() && !pageables.isEmpty() && method.getReturnType().isAssignableFrom(PAGE_FQN)) {
            expression.add("$1L.findAll($2N)", repository, pageables.get(0).getSimpleName());
            expressionType = typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror());
        } //
        else if (params.size() == 1
                && params.get(0).getSimpleName().equals(entity.getIdName())
                && params.get(0).getType().isAssignableTo(entity.getIdType())) {
            expression.add("$1L.getById($2N)", repository, params.get(0).getSimpleName());
            expressionType = entity.getType();
        } //
        else {
            CodeBlock.Builder predicateStatements = CodeBlock.builder();
            String builderVar = names.newName("builder");

            for (Variable param : params) {
                CodeBlock.Builder build = CodeBlock.builder();

                if (entity.getType().findGetter(param.getSimpleName(), param.getType()).isPresent()) {
                    analyzeParameter(entity, param, builderVar, build);
                }

                if (build.isEmpty()) {
                    for (Executable paramMethod : param.getType().getMethods()) {
                        analyzeMethods(entity, param, paramMethod, builderVar, build);
                    }
                }

                if (build.isEmpty()) {
                    for (GetAccessor getter : param.getType().getGetters()) {
                        if (entity.getType().findGetter(
                                getter.getAccessedName(), getter.getAccessedType()
                        ).isPresent()) {
                            analyzeGetters(entity, param, getter, builderVar, build);
                        }
                    }
                }

                if (!build.isEmpty()) {
                    predicateStatements
                            .beginControlFlow("if ($1N != null)", param.getSimpleName())
                            .add(build.build())
                            .endControlFlow();
                }
            }
            if (!predicateStatements.isEmpty()) {
                statements.add("$1T $2N = new $1T();\n", ClassName.bestGuess(BOOLEAN_BUILDER_FQN), builderVar);
                statements.add(predicateStatements.build());

                if (pageables.isEmpty()) {
                    expression.add("$1L.findAll($2N)", executor, builderVar);
                    expressionType = typeFactory.getType(Iterable.class.getName(), entity.getType().getTypeMirror());
                } else {
                    expression.add("$1L.findAll($2N, $3N)", executor, builderVar, pageables.get(0).getSimpleName());
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
                                  String builderVar,
                                  CodeBlock.Builder code) {
        code.beginControlFlow("if ($1N != null)", param.getSimpleName());
        code.add("$1N.and($2L.$3N.eq($3N));\n",
                builderVar, getQueryVar(entity.getTypeName()), param.getSimpleName()
        );
        code.endControlFlow();
    }

    private void analyzeMethods(Entity entity,
                                Variable param,
                                Executable method,
                                String builderVar,
                                CodeBlock.Builder code) {
        if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
            return;
        }
        TypeElement entityPathElement = entity.getType().getFactory().getElementUtils()
                .getTypeElement(ENTITY_PATH_FQN);

        List<CodeBlock> args = Lists.newArrayList();
        for (Variable arg : method.getParameters()) {
            if (arg.getType().isAssignableTo(ENTITY_PATH_FQN)) {
                args.add(getQueryVar((ClassName) TypeName.get(
                        arg.getType().asMember(entityPathElement.getTypeParameters().get(0))
                )));
            } else {
                return;
            }
        }
        code.add("$1N.and($2N.$3N($4L));\n",
                builderVar, param.getSimpleName(), method.getSimpleName(), CodeBlock.join(args, ", ")
        );
    }

    private void analyzeGetters(Entity entity,
                                Variable param,
                                GetAccessor getter,
                                String builderVar,
                                CodeBlock.Builder code) {
        code.beginControlFlow("if ($1N.$2N() != null)",
                param.getSimpleName(), getter.getSimpleName()
        );
        code.add("$1N.and($2L.$3N.eq($4N.$5N()));\n",
                builderVar, getQueryVar(entity.getTypeName()), getter.getAccessedName(),
                param.getSimpleName(), getter.getSimpleName()
        );
        code.endControlFlow();
    }

    private CodeBlock getQueryVar(ClassName entityName) {
        return CodeBlock.of("$1T.$2N",
                ClassName.get(entityName.packageName(), "Q" + entityName.simpleName()),
                StringUtils.uncapitalize(entityName.simpleName())
        );
    }
}
