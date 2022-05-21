package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.SetAccessor;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.lang.model.element.TypeElement;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class AbstractFindSnippet implements FindSnippet {
    private Entity entity;

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void find(CodeBuilder codeBuilder, List<Variable> variables, Type returnType) {
        Expression findExpr;
        if (variables.isEmpty()) {
            findExpr = doFindAll(codeBuilder);
        } else if (variables.size() == 1
                && getEntity().getIdName().equals(variables.get(0).getName())
                && getEntity().getIdType().isAssignableFrom(variables.get(0).getType())) {
            findExpr = doFindById(codeBuilder, variables.get(0));
        } else {
            findExpr = doFind(codeBuilder, variables);
        }

        String resultVar = codeBuilder.names().newName("result");
        codeBuilder.add("$1T $2N = $3L;\n",
                findExpr.getType().getTypeMirror(), resultVar, findExpr.getCode()
        );
        codeBuilder.add("return $L;\n", doMappings(
                codeBuilder, Expressions.of(findExpr.getType(), CodeBlock.of("$N", resultVar)), returnType
        ));
    }

    protected abstract Expression doFindAll(CodeBuilder codeBuilder);

    protected abstract Expression doFindById(CodeBuilder codeBuilder, Variable idVariable);

    protected abstract Expression doFind(CodeBuilder codeBuilder, List<Variable> variables);

    protected CodeBlock doMappings(CodeBuilder codeBuilder, Expression source, Type targetType) {
        Type sourceType = source.getType();

        if (targetType.erasure().isAssignableFrom(List.class.getName())
                && sourceType.erasure().isAssignableTo(Iterable.class.getName())) {

            TypeFactory typeFactory = getEntity().getType().getFactory();
            TypeElement iterableElement = typeFactory.getElementUtils().getTypeElement(Iterable.class.getName());

            CodeBuilder mappingBuilder = CodeBuilders.create(codeBuilder.names());
            if (sourceType.erasure().isAssignableTo(Collection.class.getName())) {
                mappingBuilder.add("$1L.stream()", source.getCode());
            } else {
                mappingBuilder.add("$1T.stream($2L.spliterator(), false)", StreamSupport.class, source.getCode());
            }

            String itVar = mappingBuilder.names().newName("it");
            mappingBuilder.add(".map($1N -> {\n$>", itVar);
            mappingBuilder.add("return $L;\n", doMappings(
                    mappingBuilder,
                    Expressions.of(
                            typeFactory.getType(sourceType.asMember(iterableElement.getTypeParameters().get(0))),
                            CodeBlock.of("$N", itVar)
                    ),
                    targetType.getTypeArguments().get(0)
            ));
            mappingBuilder.add("$<}).collect($T.toList())", Collectors.class);

            return mappingBuilder.toCode();
        }

        if (sourceType.equals(getEntity().getType())
                && targetType.isAssignableFrom(getEntity().getIdType())) {
            return CodeBlock.of("$1L.$2N()", source.getCode(), getEntity().getIdGetter().getSimpleName());
        }

        String tempVar = codeBuilder.names().newName("temp");
        codeBuilder.add("$1T $2N = new $1T();\n", targetType.getTypeMirror(), tempVar);
        for (SetAccessor setter : targetType.getSetters()) {
            sourceType.findGetter(setter.getAccessedName(), setter.getAccessedType()).ifPresent(it ->
                    codeBuilder.add("$1N.$2N($3L.$4N());\n",
                            tempVar, setter.getSimpleName(), source.getCode(), it.getSimpleName()
                    )
            );
        }
        return CodeBlock.of("$N", tempVar);
    }
}
