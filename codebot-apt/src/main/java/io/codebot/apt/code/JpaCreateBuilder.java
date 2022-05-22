package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

import java.util.Map;

public class JpaCreateBuilder extends AbstractCreateBuilder {
    private CodeBlock jpaRepository;

    public void setJpaRepository(CodeBlock jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    protected CodeBlock getJpaRepository() {
        return jpaRepository;
    }

    @Override
    protected Variable doCreate(CodeWriter codeWriter, Map<String, Expression> sources) {
        Type entityType = getEntity().getType();

        Variable entity = codeWriter.newVariable("entity", Expressions.of(
                entityType, CodeBlock.of("new $1T()", entityType.getTypeMirror())
        ));

        for (Map.Entry<String, Expression> source : sources.entrySet()) {
            entityType.findSetter(source.getKey(), source.getValue().getType()).ifPresent(setter ->
                    codeWriter.add("$1N.$2N($3L);\n",
                            entity.getName(), setter.getSimpleName(), source.getValue().getCode()
                    )
            );
        }
        codeWriter.add("$1L.save($2N);\n", getJpaRepository(), entity.getName());

        return entity;
    }
}
