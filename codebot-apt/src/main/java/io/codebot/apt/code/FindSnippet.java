package io.codebot.apt.code;

import io.codebot.apt.type.Type;

import java.util.List;

public interface FindSnippet {
    void find(CodeBuilder codeBuilder, List<Variable> variables, Type returnType);
}