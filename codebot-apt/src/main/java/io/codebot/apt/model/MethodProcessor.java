package io.codebot.apt.model;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeSpec;

public interface MethodProcessor {
    void process(Service service, TypeSpec.Builder serviceBuilder,
                 Method method, MethodSpec.Builder methodBuilder,
                 NameAllocator nameAlloc);
}
