package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractJpaFindSnippet extends AbstractFindSnippet  {
    protected static final String PAGE_FQN = "org.springframework.data.domain.Page";
    protected static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    private CodeBlock jpaRepository;
    private String pageableVariableName;

    public void setJpaRepository(CodeBlock jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void addContextVariable(String name, Type type) {
        if (type.isAssignableTo(PAGEABLE_FQN)) {
            if (pageableVariableName == null) {
                pageableVariableName = name;
            }
        }
        super.addContextVariable(name, type);
    }

    protected CodeBlock getJpaRepository() {
        return jpaRepository;
    }

    protected List<ContextVariable> getQueryVariables() {
        return getContextVariables().stream()
                .filter(it -> !it.getName().equals(getPageableVariableName()))
                .collect(Collectors.toList());
    }

    protected String getPageableVariableName() {
        return pageableVariableName;
    }

    @Override
    protected FindExpression findById(CodeBuffer codeBuffer, ContextVariable idVariable) {
        return new FindExpression(
                CodeBlock.of("$1L.getById($2N)", jpaRepository, idVariable.getName()),
                getEntity().getType()
        );
    }

    @Override
    protected FindExpression findAll(CodeBuffer codeBuffer) {
        TypeFactory typeFactory = getEntity().getType().getFactory();
        if (getPageableVariableName() != null) {
            return new FindExpression(
                    CodeBlock.of("$1L.findAll($2N)", getJpaRepository(), getPageableVariableName()),
                    typeFactory.getType(PAGE_FQN, getEntity().getType().getTypeMirror())
            );
        }
        return new FindExpression(
                CodeBlock.of("$1L.findAll()", getJpaRepository()),
                typeFactory.getListType(getEntity().getType().getTypeMirror())
        );
    }
}
