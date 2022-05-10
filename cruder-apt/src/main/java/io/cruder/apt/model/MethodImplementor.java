package io.cruder.apt.model;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.TypeSpec;

public interface MethodImplementor {
    void implement(Service service, TypeSpec.Builder serviceBuilder,
                   Method method, MethodSpec.Builder methodBuilder,
                   NameAllocator nameAlloc);
}
