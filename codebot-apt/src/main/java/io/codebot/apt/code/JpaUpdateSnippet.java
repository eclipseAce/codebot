package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;

import java.util.Map;

public class JpaUpdateSnippet extends AbstractUpdateSnippet {
    private CodeBlock jpaRepository;

    public void setJpaRepository(CodeBlock jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    protected CodeBlock getJpaRepository() {
        return jpaRepository;
    }

    @Override
    protected Variable doUpdate(CodeBuilder codeBuilder, Expression targetId, Map<String, Expression> sources) {
        Variable entity = Expressions.of(
                getEntity().getType(),
                CodeBlock.of("$1L.getById($2L)", getJpaRepository(), targetId.getCode())
        ).asVariable(codeBuilder, "entity");

        for (Map.Entry<String, Expression> source : sources.entrySet()) {
            entity.getType().findSetter(source.getKey(), source.getValue().getType()).ifPresent(setter ->
                    codeBuilder.add("$1N.$2N($3L);\n",
                            entity.getName(), setter.getSimpleName(), source.getValue().getCode()
                    )
            );
        }
        codeBuilder.add("$1L.save($2N);\n", getJpaRepository(), entity.getName());

        return entity;
    }
}
