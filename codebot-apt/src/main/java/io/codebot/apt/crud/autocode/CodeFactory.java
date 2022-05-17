package io.codebot.apt.crud.autocode;

public interface CodeFactory<T extends Code> {
    T getCode();
}
