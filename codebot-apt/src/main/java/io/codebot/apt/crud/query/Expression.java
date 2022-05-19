package io.codebot.apt.crud.query;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public class Expression {
    private final CodeBlock expression;
    private final Type expressionType;

    public Expression(CodeBlock expression, Type expressionType) {
        this.expression = expression;
        this.expressionType = expressionType;
    }

    public CodeBlock getExpression() {
        return expression;
    }

    public Type getExpressionType() {
        return expressionType;
    }
}
