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

public class MappingSnippet implements CodeSnippet<Expression> {
    private Entity entity;
    private Expression sourceExpression;
    private Type targetType;

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void setSourceExpression(Expression sourceExpression) {
        this.sourceExpression = sourceExpression;
    }

    public void setTargetType(Type targetType) {
        this.targetType = targetType;
    }

    @Override
    public Expression writeTo(CodeBuffer context) {
        return map(context, entity, sourceExpression, targetType);
    }

    protected Expression map(CodeBuffer codeBuffer, Entity entity, Expression sourceExpression, Type targetType) {
        Type sourceType = sourceExpression.getType();
        if (targetType.erasure().isAssignableFrom(List.class.getName())
                && sourceType.erasure().isAssignableTo(Iterable.class.getName())) {
            TypeFactory typeFactory = entity.getType().getFactory();
            TypeElement iterableElement = typeFactory.getElementUtils().getTypeElement(Iterable.class.getName());

            SimpleCodeBuilder expression = new SimpleCodeBuilder(codeBuffer.nameAllocator());
            if (sourceType.erasure().isAssignableTo(Collection.class.getName())) {
                expression.add("$1L.stream()", sourceExpression.getCode());
            } else {
                expression.add("$1T.stream($2L.spliterator(), false)", StreamSupport.class, sourceExpression.getCode());
            }

            String itVar = expression.nameAllocator().newName("it");
            expression.add(".map($1N -> {\n$>", itVar);
            Expression source = new Expression(
                    CodeBlock.of("$N", itVar),
                    typeFactory.getType(sourceType.asMember(iterableElement.getTypeParameters().get(0)))
            );
            expression.add("return $L;\n", map(expression, entity, source, targetType.getTypeArguments().get(0)).getCode());
            expression.add("$<}).collect($T.toList())", Collectors.class);
            return new Expression(expression.toCode(), targetType);
        }

        if (sourceType.equals(entity.getType())
                && targetType.isAssignableFrom(entity.getIdType())) {
            return new Expression(
                    CodeBlock.of("$1L.$2N()", sourceExpression.getCode(), entity.getIdGetter().getSimpleName()),
                    targetType
            );
        }

        String tempVar = codeBuffer.nameAllocator().newName("temp");
        codeBuffer.add("$1T $2N = new $1T();\n", targetType.getTypeMirror(), tempVar);
        for (SetAccessor setter : targetType.getSetters()) {
            sourceType.findGetter(setter.getAccessedName(), setter.getAccessedType()).ifPresent(it ->
                    codeBuffer.add("$1N.$2N($3L.$4N());\n",
                            tempVar, setter.getSimpleName(), sourceExpression.getCode(), it.getSimpleName()
                    )
            );
        }
        return new Expression(CodeBlock.of("$N", tempVar), targetType);
    }
}
