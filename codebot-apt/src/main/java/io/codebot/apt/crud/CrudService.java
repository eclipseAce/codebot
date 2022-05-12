package io.codebot.apt.crud;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import io.codebot.apt.type.Type;

public class CrudService {
    public final Type type;
    public final ClassName typeName;
    public final ClassName implementationTypeName;
    public final TypeSpec.Builder builder;
}
