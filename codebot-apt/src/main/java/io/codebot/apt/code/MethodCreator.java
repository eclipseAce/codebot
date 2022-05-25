package io.codebot.apt.code;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;

public interface MethodCreator {
    CodeWriter body();

    MethodCreator addModifiers(Modifier... modifiers);

    MethodCreator addAnnotation(AnnotationSpec annotation);

    MethodCreator addParameter(ParameterSpec parameter);

    MethodCreator returns(TypeMirror type);

    MethodSpec create();
}
