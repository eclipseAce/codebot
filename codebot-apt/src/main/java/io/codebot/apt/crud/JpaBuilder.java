package io.codebot.apt.crud;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.code.*;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JpaBuilder extends AbstractBuilder {
    protected static final String PAGE_FQN = "org.springframework.data.domain.Page";
    protected static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    protected static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    protected static final String ROOT_FQN = "javax.persistence.criteria.Root";
    protected static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    protected static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private CodeBlock jpaRepository;
    private CodeBlock jpaSpecificationExecutor;

    public void setJpaRepository(CodeBlock jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void setJpaSpecificationExecutor(CodeBlock jpaSpecificationExecutor) {
        this.jpaSpecificationExecutor = jpaSpecificationExecutor;
    }

    protected CodeBlock getJpaRepository() {
        return jpaRepository;
    }

    protected CodeBlock getJpaSpecificationExecutor() {
        return jpaSpecificationExecutor;
    }

    @Override
    protected Variable doCreate(MethodWriter methodWriter, Map<String, Expression> sources) {
        Type entityType = getEntity().getType();

        Variable entity = methodWriter.body().declareVariable("entity", Expressions.of(
                entityType, CodeBlock.of("new $1T()", entityType.getTypeMirror())
        ));

        for (Map.Entry<String, Expression> source : sources.entrySet()) {
            entityType.findSetter(source.getKey(), source.getValue().getType()).ifPresent(setter ->
                    methodWriter.body().add("$1N.$2N($3L);\n",
                            entity.getName(), setter.getSimpleName(), source.getValue().getCode()
                    )
            );
        }
        methodWriter.body().add("$1L.save($2N);\n", getJpaRepository(), entity.getName());

        return entity;
    }

    @Override
    protected Variable doUpdate(MethodWriter methodWriter, Expression targetId, Map<String, Expression> sources) {
        Variable entity = methodWriter.body().declareVariable("entity", Expressions.of(
                getEntity().getType(),
                CodeBlock.of("$1L.getById($2L)", getJpaRepository(), targetId.getCode())
        ));

        for (Map.Entry<String, Expression> source : sources.entrySet()) {
            entity.getType().findSetter(source.getKey(), source.getValue().getType()).ifPresent(setter ->
                    methodWriter.body().add("$1N.$2N($3L);\n",
                            entity.getName(), setter.getSimpleName(), source.getValue().getCode()
                    )
            );
        }
        methodWriter.body().add("$1L.save($2N);\n", getJpaRepository(), entity.getName());

        return entity;
    }

    @Override
    protected List<Parameter> getQueryParameters(Method overridden) {
        return overridden.getParameters().stream()
                .filter(it -> !it.getType().isAssignableTo(PAGEABLE_FQN))
                .collect(Collectors.toList());
    }

    protected Parameter getPageableParameter(Method overridden) {
        return overridden.getParameters().stream()
                .filter(it -> it.getType().isAssignableTo(PAGEABLE_FQN))
                .findFirst().orElse(null);
    }

    @Override
    protected Variable doQuery(Method overridden, MethodWriter methodWriter) {
        Type entityType = getEntity().getType();
        TypeFactory typeFactory = entityType.getFactory();
        Parameter pageable = getPageableParameter(overridden);
        if (pageable != null) {
            return methodWriter.body().declareVariable("result", Expressions.of(
                    typeFactory.getType(PAGE_FQN, entityType.getTypeMirror()),
                    CodeBlock.of("$1L.findAll($2N)", getJpaRepository(), pageable.getName())
            ));
        }
        return methodWriter.body().declareVariable("result", Expressions.of(
                typeFactory.getListType(entityType.getTypeMirror()),
                CodeBlock.of("$1L.findAll()", getJpaRepository())
        ));
    }

    @Override
    protected Variable doQuery(Method overridden, MethodWriter methodWriter, Parameter idParam) {
        return methodWriter.body().declareVariable("result", Expressions.of(
                getEntity().getType(),
                CodeBlock.of("$1L.getById($2N)", getJpaRepository(), idParam.getName())
        ));
    }

    @Override
    protected Variable doQuery(Method overridden, MethodWriter methodWriter, List<Parameter> params) {
        CodeWriter specificationBody = methodWriter.body().fork();
        String rootVar = specificationBody.allocateName("root");
        String queryVar = specificationBody.allocateName("query");
        String builderVar = specificationBody.allocateName("builder");
        String predicatesVar = specificationBody.allocateName("predicates");

        Type entityType = getEntity().getType();
        TypeFactory typeFactory = entityType.getFactory();

        specificationBody.add(new ParameterScanner() {
            @Override
            public CodeBlock scanVariable(Variable variable) {
                if (entityType.findGetter(variable.getName(), variable.getType()).isPresent()) {
                    return CodeBlock.of("$1N.add($2N.equal($3N.get($4S), $4N));\n",
                            predicatesVar, builderVar, rootVar, variable.getName());
                }
                return null;
            }

            @Override
            public CodeBlock scanVariableMethod(Variable variable, Executable method) {
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
            public CodeBlock scanVariableGetter(Variable variable, GetAccessor getter) {
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
        }.scan(params));

        if (!specificationBody.isEmpty()) {
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
                return methodWriter.body().declareVariable("result", Expressions.of(
                        entityType,
                        CodeBlock.of("$1L.findOne($2L).orElse(null)", jpaSpecificationExecutor, specification)
                ));
            }
            Parameter pageable = getPageableParameter(overridden);
            if (pageable != null) {
                return methodWriter.body().declareVariable("result", Expressions.of(
                        typeFactory.getType(PAGE_FQN, entityType.getTypeMirror()),
                        CodeBlock.of("$1L.findAll($2L, $3N)",
                                jpaSpecificationExecutor, specification, pageable.getName())
                ));
            }
            return methodWriter.body().declareVariable("result", Expressions.of(
                    typeFactory.getListType(entityType.getTypeMirror()),
                    CodeBlock.of("$1L.findAll($2L)", jpaSpecificationExecutor, specification)
            ));
        }
        return doQuery(overridden, methodWriter);
    }

    @Override
    protected CodeBlock doMappings(CodeWriter codeWriter, Variable source, Type targetType) {
        Type sourceType = source.getType();

        if (targetType.erasure().isAssignableFrom(PAGE_FQN)
                && sourceType.erasure().isAssignableTo(PAGE_FQN)) {

            TypeFactory typeFactory = getEntity().getType().getFactory();
            TypeElement pageElement = typeFactory.getElementUtils().getTypeElement(PAGE_FQN);

            CodeWriter mappingBuilder = codeWriter.fork();
            Variable itVar = Variables.of(
                    typeFactory.getType(sourceType.asMember(pageElement.getTypeParameters().get(0))),
                    mappingBuilder.allocateName("it")
            );

            mappingBuilder.add("$1N.map($2N -> {\n$>", source.getName(), itVar.getName());
            mappingBuilder.add("return $L;\n", doMappings(
                    mappingBuilder, itVar, targetType.getTypeArguments().get(0)
            ));
            mappingBuilder.add("$<})");
            return mappingBuilder.getCode();
        }
        return super.doMappings(codeWriter, source, targetType);
    }

    protected interface ParameterScanner {
        default CodeBlock scan(List<Parameter> params) {
            CodeBlock.Builder builder = CodeBlock.builder();
            for (Parameter param : params) {
                CodeBlock code = scanVariable(param);
                if (code == null || code.isEmpty()) {
                    code = param.getType().getMethods().stream()
                            .map(method -> scanVariableMethod(param, method))
                            .filter(Objects::nonNull)
                            .collect(CodeBlock.joining(""));
                }
                if (code.isEmpty()) {
                    code = param.getType().getGetters().stream()
                            .map(getter -> scanVariableGetter(param, getter))
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

        CodeBlock scanVariable(Variable variable);

        CodeBlock scanVariableMethod(Variable variable, Executable method);

        CodeBlock scanVariableGetter(Variable variable, GetAccessor getter);
    }
}
