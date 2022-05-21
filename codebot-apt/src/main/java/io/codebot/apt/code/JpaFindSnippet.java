package io.codebot.apt.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JpaFindSnippet extends AbstractFindSnippet {
    protected static final String PAGE_FQN = "org.springframework.data.domain.Page";
    protected static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    protected static final String PREDICATE_FQN = "javax.persistence.criteria.Predicate";
    protected static final String ROOT_FQN = "javax.persistence.criteria.Root";
    protected static final String CRITERIA_QUERY_FQN = "javax.persistence.criteria.CriteriaQuery";
    protected static final String CRITERIA_BUILDER_FQN = "javax.persistence.criteria.CriteriaBuilder";

    private CodeBlock jpaRepository;
    private CodeBlock jpaSpecificationExecutor;

    private Variable pageable;

    public void setJpaRepository(CodeBlock jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void setJpaSpecificationExecutor(CodeBlock jpaSpecificationExecutor) {
        this.jpaSpecificationExecutor = jpaSpecificationExecutor;
    }

    protected CodeBlock getJpaRepository() {
        return jpaRepository;
    }

    protected Variable getPageable() {
        return pageable;
    }

    @Override
    public void find(CodeBuilder codeBuilder, List<Variable> variables, Type returnType) {
        List<Variable> queryVariables = Lists.newArrayList();
        for (Variable variable : variables) {
            if (variable.getType().isAssignableTo(PAGEABLE_FQN)) {
                if (pageable == null) {
                    pageable = variable;
                }
                continue;
            }
            queryVariables.add(variable);
        }
        super.find(codeBuilder, queryVariables, returnType);
    }

    @Override
    protected Expression doFindAll(CodeBuilder codeBuilder) {
        Type entityType = getEntity().getType();
        TypeFactory typeFactory = entityType.getFactory();
        if (getPageable() != null) {
            return Expressions.of(
                    typeFactory.getType(PAGE_FQN, entityType.getTypeMirror()),
                    CodeBlock.of("$1L.findAll($2N)", getJpaRepository(), getPageable().getName())
            );
        }
        return Expressions.of(
                typeFactory.getListType(entityType.getTypeMirror()),
                CodeBlock.of("$1L.findAll()", getJpaRepository())
        );
    }

    @Override
    protected Expression doFindById(CodeBuilder codeBuilder, Variable idVariable) {
        return Expressions.of(
                getEntity().getType(),
                CodeBlock.of("$1L.getById($2N)", jpaRepository, idVariable.getName())
        );
    }

    @Override
    protected Expression doFind(CodeBuilder codeBuilder, List<Variable> variables) {
        NameAllocator localNames = codeBuilder.names().clone();
        String rootVar = localNames.newName("root");
        String queryVar = localNames.newName("query");
        String builderVar = localNames.newName("builder");
        String predicatesVar = localNames.newName("predicates");

        Type entityType = getEntity().getType();
        TypeFactory typeFactory = entityType.getFactory();

        CodeBlock specificationBody = new VariableScanner() {
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
                for (io.codebot.apt.type.Variable arg : method.getParameters()) {
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
        }.scan(variables);

        if (!specificationBody.isEmpty()) {
            CodeBlock specification = CodeBlock.builder()
                    .add("($1N, $2N, $3N) -> {\n$>", rootVar, queryVar, builderVar)
                    .add("$1T<$2T> $3N = new $1T<>();\n",
                            ArrayList.class, ClassName.bestGuess(PREDICATE_FQN), predicatesVar)
                    .add(specificationBody)
                    .add("return $1N.and($2N.toArray(new $3T[0]));\n",
                            builderVar, predicatesVar, ClassName.bestGuess(PREDICATE_FQN))
                    .add("$<}")
                    .build();
            if (getPageable() != null) {
                return Expressions.of(
                        typeFactory.getType(PAGE_FQN, entityType.getTypeMirror()),
                        CodeBlock.of("$1L.findAll($2L, $3N)",
                                jpaSpecificationExecutor, specification, getPageable().getName())
                );
            }
            return Expressions.of(
                    typeFactory.getListType(entityType.getTypeMirror()),
                    CodeBlock.of("$1L.findAll($2L)", jpaSpecificationExecutor, specification)
            );
        }
        return doFindAll(codeBuilder);
    }

    @Override
    protected CodeBlock doMappings(CodeBuilder codeBuilder, Expression source, Type targetType) {
        Type sourceType = source.getType();

        if (targetType.erasure().isAssignableFrom(PAGE_FQN)
                && sourceType.erasure().isAssignableTo(PAGE_FQN)) {

            TypeFactory typeFactory = getEntity().getType().getFactory();
            TypeElement pageElement = typeFactory.getElementUtils().getTypeElement(PAGE_FQN);

            CodeBuilder mappingBuilder = CodeBuilders.create(codeBuilder.names());
            String itVar = mappingBuilder.names().newName("it");

            mappingBuilder.add("$1L.map($2N -> {\n$>", source.getCode(), itVar);
            mappingBuilder.add("return $L;\n", doMappings(
                    mappingBuilder,
                    Expressions.of(
                            typeFactory.getType(sourceType.asMember(pageElement.getTypeParameters().get(0))),
                            CodeBlock.of("$N", itVar)
                    ),
                    targetType.getTypeArguments().get(0)
            ));
            mappingBuilder.add("$<})");
            return mappingBuilder.toCode();
        }
        return super.doMappings(codeBuilder, source, targetType);
    }

    protected interface VariableScanner {
        default CodeBlock scan(List<Variable> variables) {
            CodeBlock.Builder builder = CodeBlock.builder();
            for (Variable variable : variables) {
                CodeBlock code = scanVariable(variable);
                if (code == null || code.isEmpty()) {
                    code = variable.getType().getMethods().stream()
                            .map(method -> scanVariableMethod(variable, method))
                            .filter(Objects::nonNull)
                            .collect(CodeBlock.joining(""));
                }
                if (code.isEmpty()) {
                    code = variable.getType().getGetters().stream()
                            .map(getter -> scanVariableGetter(variable, getter))
                            .filter(Objects::nonNull)
                            .collect(CodeBlock.joining(""));
                }
                builder.add(postProcessAfterVariableScan(variable, code));
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
