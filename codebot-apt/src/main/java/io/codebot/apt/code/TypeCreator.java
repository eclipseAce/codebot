package io.codebot.apt.code;

import com.squareup.javapoet.*;
import io.codebot.apt.type.Type;

import javax.lang.model.element.Modifier;
import java.util.function.Supplier;

public interface TypeCreator {
    TypeCreator addModifiers(Modifier... modifiers);

    TypeCreator addAnnotation(AnnotationSpec annotation);

    TypeCreator addMethod(MethodSpec method);

    TypeCreator addField(FieldSpec field);

    TypeCreator addFieldIfNameAbsent(String name, Supplier<FieldSpec> fieldSupplier);

    TypeCreator addSuperinterface(Type interfaceType);

    TypeCreator superclass(Type classType);

    JavaFile create();
}
