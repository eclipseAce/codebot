package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;

import java.util.Map;

public class JpaUpdateBuilder extends AbstractUpdateBuilder {
    private CodeBlock jpaRepository;

    public void setJpaRepository(CodeBlock jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    protected CodeBlock getJpaRepository() {
        return jpaRepository;
    }

    @Override
    protected Variable doUpdate(CodeWriter codeWriter, Expression targetId, Map<String, Expression> sources) {
        Variable entity = codeWriter.newVariable("entity", Expressions.of(
                getEntity().getType(),
                CodeBlock.of("$1L.getById($2L)", getJpaRepository(), targetId.getCode())
        ));

        for (Map.Entry<String, Expression> source : sources.entrySet()) {
            entity.getType().findSetter(source.getKey(), source.getValue().getType()).ifPresent(setter ->
                    codeWriter.add("$1N.$2N($3L);\n",
                            entity.getName(), setter.getSimpleName(), source.getValue().getCode()
                    )
            );
        }
        codeWriter.add("$1L.save($2N);\n", getJpaRepository(), entity.getName());

        return entity;
    }
}
