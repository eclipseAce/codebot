package io.codebot.apt.code;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;

import javax.lang.model.element.Modifier;
import javax.lang.model.type.DeclaredType;
import java.util.function.Supplier;

public interface TypeCreator {
    TypeCreator addModifiers(Modifier... modifiers);

    TypeCreator addAnnotation(AnnotationSpec annotation);

    TypeCreator addMethod(MethodSpec method);

    TypeCreator addField(FieldSpec field);

    TypeCreator addFieldIfNameAbsent(String name, Supplier<FieldSpec> fieldSupplier);

    TypeCreator addSuperinterface(DeclaredType interfaceType);

    TypeCreator superclass(DeclaredType classType);

    JavaFile create();
}
