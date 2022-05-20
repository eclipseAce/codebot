package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public class FindResult {
    private final CodeBlock expression;
    private final Type resultType;

    FindResult(CodeBlock expression, Type resultType) {
        this.expression = expression;
        this.resultType = resultType;
    }

    public CodeBlock getExpression() {
        return expression;
    }

    public Type getResultType() {
        return resultType;
    }
}