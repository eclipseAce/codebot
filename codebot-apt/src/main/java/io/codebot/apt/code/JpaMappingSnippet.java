package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.lang.model.element.TypeElement;

public class JpaMappingSnippet extends MappingSnippet {
    private static final String PAGE_FQN = "org.springframework.data.domain.Page";

    @Override
    protected Expression map(CodeBuffer codeBuffer, Entity entity, Expression sourceExpression, Type targetType) {
        Type sourceType = sourceExpression.getType();
        if (targetType.erasure().isAssignableFrom(PAGE_FQN)
                && sourceType.erasure().isAssignableTo(PAGE_FQN)) {
            TypeFactory typeFactory = entity.getType().getFactory();
            TypeElement pageElement = typeFactory.getElementUtils().getTypeElement(PAGE_FQN);

            SimpleCodeBuilder expression = new SimpleCodeBuilder(codeBuffer.nameAllocator());
            String itVar = expression.nameAllocator().newName("it");

            Expression source = new Expression(
                    CodeBlock.of("$N", itVar),
                    typeFactory.getType(sourceType.asMember(pageElement.getTypeParameters().get(0)))
            );
            expression.add("$1L.map($2N -> {\n$>", sourceExpression.getCode(), itVar);
            expression.add("return $L;\n", map(expression, entity, source, targetType.getTypeArguments().get(0)).getCode());
            expression.add("$<})");
            return new Expression(expression.toCode(), targetType);
        }
        return super.map(codeBuffer, entity, sourceExpression, targetType);
    }
}
