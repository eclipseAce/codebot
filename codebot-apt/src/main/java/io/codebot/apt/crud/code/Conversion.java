package io.codebot.apt.crud.code;

import com.squareup.javapoet.NameAllocator;
import io.codebot.apt.type.Type;

public interface Conversion {
    Snippet convert(String sourceVar, Type sourceType, Type targetType, NameAllocator names);
}
