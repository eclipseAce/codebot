package io.cruder.apt.dsl;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import javax.lang.model.type.TypeMirror;

public abstract class DSLSupport {
    public ClassName type(String qualifiedName) {
        return ClassName.bestGuess(qualifiedName);
    }

    public TypeName type(TypeMirror typeMirror) {
        return TypeName.get(typeMirror);
    }

    public ParameterizedTypeName type(String qualifiedName, TypeName... typeParams) {
        return ParameterizedTypeName.get(type(qualifiedName), typeParams);
    }

    public ParameterizedTypeName type(ClassName typeName, TypeName... typeParams) {
        return ParameterizedTypeName.get(typeName, typeParams);
    }
}
