package io.codebot.apt.code;

import io.codebot.apt.type.Type;

import java.util.List;

public interface QueryBuilder {
    void find(CodeWriter codeWriter, List<Variable> variables, Type returnType);
}