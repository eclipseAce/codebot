package io.codebot.apt.crud.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public class Snippet {
    private final CodeBlock statements;
    private final CodeBlock expression;
    private final Type expressionType;

    public Snippet(CodeBlock statements, CodeBlock expression, Type expressionType) {
        this.statements = statements;
        this.expression = expression;
        this.expressionType = expressionType;
    }

    public CodeBlock getStatements() {
        return statements;
    }

    public CodeBlock getExpression() {
        return expression;
    }

    public Type getExpressionType() {
        return expressionType;
    }
}
