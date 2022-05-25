package io.codebot.apt.code;

import com.squareup.javapoet.CodeBlock;

import javax.lang.model.type.TypeMirror;

public interface Expression {
    CodeBlock getCode();

    TypeMirror getType();
}
