package io.codebot.apt.code;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import io.codebot.apt.type.Type;

import javax.lang.model.element.Modifier;

public interface MethodCreator {
    CodeWriter body();

    MethodCreator addModifiers(Modifier... modifiers);

    MethodCreator addAnnotation(AnnotationSpec annotation);

    MethodCreator addParameter(ParameterSpec parameter);

    MethodCreator returns(Type type);

    MethodSpec create();
}
