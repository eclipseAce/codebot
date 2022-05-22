package io.codebot.apt.code;

import io.codebot.apt.type.Type;

import java.util.List;

public interface CreateBuilder {
    void create(CodeWriter codeWriter, List<Variable> variables, Type returnType);
}
