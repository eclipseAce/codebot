package io.codebot.apt.code;

import com.google.common.collect.Lists;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.GetAccessor;
import io.codebot.apt.type.Type;

import java.util.List;
import java.util.Objects;

public abstract class JpaFindSnippet<T extends JpaFindSnippet<?>> implements CodeSnippet<JpaFindResult> {
    protected static final String PAGE_FQN = "org.springframework.data.domain.Page";

    private Entity entity;
    private CodeBlock jpaRepository;
    private String pageableVariableName;
    private final List<QueryVariable> queryVariables = Lists.newArrayList();

    public T setEntity(Entity entity) {
        this.entity = entity;
        return getThis();
    }

    public T setJpaRepository(CodeBlock jpaRepository) {
        this.jpaRepository = jpaRepository;
        return getThis();
    }

    public T setPageableVariableName(String pageableVariableName) {
        this.pageableVariableName = pageableVariableName;
        return getThis();
    }

    public T addQueryVariable(String name, Type type) {
        queryVariables.add(new QueryVariable(name, type));
        return getThis();
    }

    @SuppressWarnings("unchecked")
    private T getThis() {
        return (T) this;
    }

    protected Entity getEntity() {
        return entity;
    }

    protected CodeBlock getJpaRepository() {
        return jpaRepository;
    }

    protected String getPageableVariableName() {
        return pageableVariableName;
    }

    protected List<QueryVariable> getQueryVariables() {
        return queryVariables;
    }

    @Override
    public JpaFindResult write(CodeBlock.Builder code, NameAllocator names) {
        if (queryVariables.size() == 1
                && queryVariables.get(0).getName().equals(entity.getIdName())
                && queryVariables.get(0).getType().isAssignableTo(entity.getIdType())) {
            return findBySingleIdVariable(code, names, queryVariables.get(0));
        }
        return find(code, names);
    }

    protected JpaFindResult findBySingleIdVariable(CodeBlock.Builder code, NameAllocator names,
                                                   QueryVariable idVariable) {
        return new JpaFindResult(
                CodeBlock.of("$1L.getById($2N)", jpaRepository, idVariable.getName()),
                entity.getType()
        );
    }

    protected abstract JpaFindResult find(CodeBlock.Builder code, NameAllocator names);

    protected static class QueryVariable {
        private final String name;
        private final Type type;

        QueryVariable(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }
    }

    protected interface QueryVariableScanner {
        default CodeBlock scan(List<QueryVariable> variables) {
            CodeBlock.Builder builder = CodeBlock.builder();
            for (QueryVariable variable : variables) {
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
                if (!code.isEmpty() && !variable.getType().isPrimitive()) {
                    builder.beginControlFlow("if ($1N != null)", variable.getName());
                    builder.add(code);
                    builder.endControlFlow();
                } else {
                    builder.add(code);
                }
            }
            return builder.build();
        }

        CodeBlock scanVariable(QueryVariable variable);

        CodeBlock scanVariableMethod(QueryVariable variable, Executable method);

        CodeBlock scanVariableGetter(QueryVariable variable, GetAccessor getter);
    }
}
