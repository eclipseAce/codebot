package io.cruder.apt.dsl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.TypeMirror;

public abstract class DSLSupport {
    public ClassName type(String name) {
        return ClassName.bestGuess(name);
    }

    public TypeName type(TypeMirror typeMirror) {
        return TypeName.get(typeMirror);
    }

    public ParameterizedTypeName type(String name, TypeName... types) {
        return ParameterizedTypeName.get(type(name), types);
    }

    public ParameterizedTypeName type(ClassName type, TypeName... types) {
        return ParameterizedTypeName.get(type, types);
    }
}
