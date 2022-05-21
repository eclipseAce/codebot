package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.SetAccessor;

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
    protected Expression doFindById(CodeBuilder codeBuilder, Expression id) {
        return Expressions.of(
                getEntity().getType(),
                CodeBlock.of("$1L.getById($2L)", getJpaRepository(), id.getCode())
        );
    }

    @Override
    protected void doUpdate(CodeBuilder codeBuilder, Expression target, Map<String, Expression> sources) {
        for (Map.Entry<String, Expression> source : sources.entrySet()) {
            target.getType().findSetter(source.getKey(), source.getValue().getType()).ifPresent(setter ->
                    codeBuilder.add("$1L.$2N($3L);\n",
                            target.getCode(), setter.getSimpleName(), source.getValue().getCode()
                    )
            );
        }
        codeBuilder.add("$1L.save($2L);\n", getJpaRepository(), target.getCode());
    }
}
