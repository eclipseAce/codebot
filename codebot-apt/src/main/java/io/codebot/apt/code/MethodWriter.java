package io.codebot.apt.code;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

import javax.lang.model.element.Modifier;

public interface MethodWriter {
    CodeWriter body();

    MethodWriter addModifiers(Modifier... modifiers);

    MethodWriter addAnnotation(AnnotationSpec annotation);

    MethodWriter addParameter(ParameterSpec parameter);

    MethodSpec getMethod();
}
