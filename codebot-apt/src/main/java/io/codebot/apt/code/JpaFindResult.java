package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;
import io.codebot.apt.type.Type;

public class JpaFindResult {
    private final CodeBlock findExpression;
    private final Type resultType;

    JpaFindResult(CodeBlock findExpression, Type resultType) {
        this.findExpression = findExpression;
        this.resultType = resultType;
    }

    public CodeBlock getFindExpression() {
        return findExpression;
    }

    public Type getResultType() {
        return resultType;
    }
}