package io.codebot.apt.crud;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javapoet.*;
import io.codebot.apt.code.*;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class JpaCrudProcessor extends AbstractCrudProcessor {
    protected static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";

    protected static final String JPA_REPOSITORY_FQN = "org.springframework.data.jpa.repository.JpaRepository";
    protected static final String JPA_SPECIFICATION_EXECUTOR_FQN = "org.springframework.data.jpa.repository.JpaSpecificationExecutor";

    protected static final String PAGE_FQN = "org.springframework.data.domain.Page";
    protected static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    protected static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    protected static final String ROOT_FQN = "javax.persistence.criteria.Root";
    protected static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    protected static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private final Map<String, CodeBlock> injectedFields = Maps.newLinkedHashMap();

    protected CodeBlock getInjectedField(String fieldName, TypeName type) {
        return injectedFields.computeIfAbsent(fieldName, k -> {
            classCreator.addField(FieldSpec
                    .builder(type, fieldName, Modifier.PRIVATE)
                    .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                    .build()
            );
            return CodeBlock.of("this.$N", fieldName);
        });
    }

    protected CodeBlock getJpaRepository() {
        return getInjectedField("jpaRepository", ParameterizedTypeName.get(
                ClassName.bestGuess(JPA_REPOSITORY_FQN),
                entity.getTypeName(), entity.getIdTypeName().box()
        ));
    }

    protected CodeBlock getJpaSpecificationExecutor() {
        return getInjectedField("jpaSpecificationExecutor", ParameterizedTypeName.get(
                ClassName.bestGuess(JPA_SPECIFICATION_EXECUTOR_FQN),
                entity.getTypeName()
        ));
    }

    @Override
    protected Variable doCreateEntity(Method overridden, MethodCreator creator, Map<String, Expression> sources) {
        Type entityType = entity.getType();
        Variable entityVar = creator.body().declareVariable("entityVar", Expressions.of(
                entityType, CodeBlock.of("new $1T()", entityType.getTypeMirror())
        ));
        for (Map.Entry<String, Expression> source : sources.entrySet()) {
            entityType.findSetter(source.getKey(), source.getValue().getType()).ifPresent(setter ->
                    creator.body().add("$1N.$2N($3L);\n",
                            entityVar.getName(), setter.getSimpleName(), source.getValue().getCode()
                    )
            );
        }
        creator.body().add("$1L.save($2N);\n", getJpaRepository(), entityVar.getName());
        return entityVar;
    }

    @Override
    protected Variable doUpdateEntity(Method overridden, MethodCreator creator, Expression entityId, Map<String, Expression> sources) {
        Variable entityVar = creator.body().declareVariable("entity", Expressions.of(
                entity.getType(),
                CodeBlock.of("$1L.getById($2L)", getJpaRepository(), entityId.getCode())
        ));
        for (Map.Entry<String, Expression> source : sources.entrySet()) {
            entity.getType().findSetter(source.getKey(), source.getValue().getType()).ifPresent(setter ->
                    creator.body().add("$1N.$2N($3L);\n",
                            entityVar.getName(), setter.getSimpleName(), source.getValue().getCode()
                    )
            );
        }
        creator.body().add("$1L.save($2N);\n", getJpaRepository(), entityVar.getName());
        return entityVar;
    }

    @Override
    protected Variable doQueryEntities(Method overridden, MethodCreator creator) {
        List<Parameter> filters = Lists.newArrayList();
        Parameter pageable = null;
        for (Parameter param : overridden.getParameters()) {
            if (param.getType().isAssignableTo(PAGEABLE_FQN)) {
                if (pageable == null) {
                    pageable = param;
                }
                continue;
            }
            filters.add(param);
        }
        if (filters.isEmpty()) {
            return doFindAll(overridden, creator, pageable);
        }
        if (filters.size() == 1
                && entity.getIdName().equals(filters.get(0).getName())
                && entity.getIdType().isAssignableFrom(filters.get(0).getType())) {
            return doFindById(overridden, creator, filters.get(0).asExpression());
        }
        return doFindByFilters(overridden, creator, filters, pageable);
    }

    protected Variable doFindAll(Method overridden, MethodCreator creator,
                                 Parameter pageable) {
        if (pageable != null) {
            return creator.body().declareVariable("result", Expressions.of(
                    typeFactory.getType(PAGE_FQN, entity.getType().getTypeMirror()),
                    CodeBlock.of("$1L.findAll($2N)", getJpaRepository(), pageable.getName())
            ));
        }
        return creator.body().declareVariable("result", Expressions.of(
                typeFactory.getListType(entity.getType().getTypeMirror()),
                CodeBlock.of("$1L.findAll()", getJpaRepository())
        ));
    }

    protected Variable doFindById(Method overridden, MethodCreator creator,
                                  Expression asExpression) {
        return creator.body().declareVariable("result", Expressions.of(
                entity.getType(),
                CodeBlock.of("$1L.getById($2L)", getJpaRepository(), asExpression.getCode())
        ));
    }

    protected Variable doFindByFilters(Method overridden, MethodCreator creator,
                                       List<Parameter> filters, Parameter pageable) {
        CodeWriter specificationBody = creator.body().fork();
        String rootVar = specificationBody.allocateName("root");
        String queryVar = specificationBody.allocateName("query");
        String builderVar = specificationBody.allocateName("builder");
        String predicatesVar = specificationBody.allocateName("predicates");

        Type entityType = entity.getType();

        specificationBody.add(new ParameterScanner() {
            @Override
            public CodeBlock scanParameter(Variable variable) {
                if (entityType.findGetter(variable.getName(), variable.getType()).isPresent()) {
                    return CodeBlock.of("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                            predicatesVar, builderVar, rootVar, variable.getName());
                }
                return null;
            }

            @Override
            public CodeBlock scanParameterMethod(Variable variable, Executable method) {
                if (!method.getReturnType().isAssignableTo(PREDICATE_FQN)) {
                    return null;
                }
                List<String> formats = Lists.newArrayList();
                List<Object> formatArgs = Lists.newArrayList();
                for (io.codebot.apt.type.Parameter arg : method.getParameters()) {
                    if (arg.getType().isAssignableFrom(ROOT_FQN, entityType.getTypeMirror())) {
                        formats.add("$N");
                        formatArgs.add(rootVar);
                    } else if (arg.getType().isAssignableFrom(CRITERIA_QUERY_FQN)) {
                        formats.add("$N");
                        formatArgs.add(queryVar);
                    } else if (arg.getType().isAssignableFrom(CRITERIA_BUILDER_FQN)) {
                        formats.add("$N");
                        formatArgs.add(builderVar);
                    } else {
                        return null;
                    }
                }
                return CodeBlock.of("$1N.add($2N.$3N($4L));\n",
                        predicatesVar, variable.getName(), method.getSimpleName(),
                        CodeBlock.of(String.join(", ", formats), formatArgs.toArray(new Object[0])));
            }

            @Override
            public CodeBlock scanParameterGetter(Variable variable, GetAccessor getter) {
                if (entityType.findGetter(getter.getAccessedName(), getter.getAccessedType()).isPresent()) {
                    return CodeBlock.builder()
                            .beginControlFlow("if ($1N.$2N() != null)", variable.getName(), getter.getSimpleName())
                            .add("$1N.add($2N.equal($3N.get($4S), $5N.$6N()));\n",
                                    predicatesVar, builderVar, rootVar,
                                    getter.getAccessedName(), variable.getName(), getter.getSimpleName())
                            .endControlFlow()
                            .build();
                }
                return null;
            }
        }.scan(filters));

        if (specificationBody.isEmpty()) {
            return doFindAll(overridden, creator, pageable);
        }
        CodeBlock specification = CodeBlock.builder()
                .add("($1N, $2N, $3N) -> {\n$>", rootVar, queryVar, builderVar)
                .add("$1T<$2T> $3N = new $1T<>();\n",
                        ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar)
                .add(specificationBody.getCode())
                .add("return $1N.and($2N.toArray(new $3T[0]));\n",
                        builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN))
                .add("$<}")
                .build();
        if (!overridden.getReturnType().isIterable()) {
            return creator.body().declareVariable("result", Expressions.of(
                    entityType,
                    CodeBlock.of("$1L.findOne($2L).orElse(null)", getJpaSpecificationExecutor(), specification)
            ));
        }
        if (pageable != null) {
            return creator.body().declareVariable("result", Expressions.of(
                    typeFactory.getType(PAGE_FQN, entityType.getTypeMirror()),
                    CodeBlock.of("$1L.findAll($2L, $3N)",
                            getJpaSpecificationExecutor(), specification, pageable.getName())
            ));
        }
        return creator.body().declareVariable("result", Expressions.of(
                typeFactory.getListType(entityType.getTypeMirror()),
                CodeBlock.of("$1L.findAll($2L)", getJpaSpecificationExecutor(), specification)
        ));
    }

    @Override
    protected CodeBlock doTypeMapping(CodeWriter writer, Variable source, Type targetType) {
        Type sourceType = source.getType();
        if (targetType.erasure().isAssignableFrom(PAGE_FQN) && sourceType.erasure().isAssignableTo(PAGE_FQN)) {
            TypeElement pageElement = typeFactory.getElementUtils().getTypeElement(PAGE_FQN);

            CodeWriter mappings = writer.fork();
            Variable itVar = Variables.of(
                    typeFactory.getType(sourceType.asMember(pageElement.getTypeParameters().get(0))),
                    mappings.allocateName("it")
            );
            mappings.add("$1N.map($2N -> {\n$>", source.getName(), itVar.getName());
            mappings.add("return $L;\n", doTypeMapping(mappings, itVar, targetType.getTypeArguments().get(0)));
            mappings.add("$<})");
            return mappings.getCode();
        }
        return super.doTypeMapping(writer, source, targetType);
    }

    protected interface ParameterScanner {
        default CodeBlock scan(List<? extends Parameter> params) {
            CodeBlock.Builder builder = CodeBlock.builder();
            for (Parameter param : params) {
                CodeBlock code = scanParameter(param);
                if (code == null || code.isEmpty()) {
                    code = param.getType().getMethods().stream()
                            .map(method -> scanParameterMethod(param, method))
                            .filter(Objects::nonNull)
                            .collect(CodeBlock.joining(""));
                }
                if (code.isEmpty()) {
                    code = param.getType().getGetters().stream()
                            .map(getter -> scanParameterGetter(param, getter))
                            .filter(Objects::nonNull)
                            .collect(CodeBlock.joining(""));
                }
                builder.add(postProcessAfterVariableScan(param, code));
            }
            return builder.build();
        }

        default CodeBlock postProcessAfterVariableScan(Variable variable, CodeBlock code) {
            if (!code.isEmpty() && !variable.getType().isPrimitive()) {
                return CodeBlock.builder()
                        .beginControlFlow("if ($1N != null)", variable.getName())
                        .add(code)
                        .endControlFlow()
                        .build();
            }
            return code;
        }

        CodeBlock scanParameter(Variable variable);

        CodeBlock scanParameterMethod(Variable variable, Executable method);

        CodeBlock scanParameterGetter(Variable variable, GetAccessor getter);
    }
}
