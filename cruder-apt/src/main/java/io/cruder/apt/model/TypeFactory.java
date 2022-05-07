package io.cruder.apt.model;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class TypeFactory {
    private final Elements elementUtils;
    private final Types typeUtils;

    public TypeFactory(ProcessingEnvironment processingEnv) {
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
    }

    public Elements getElementUtils() {
        return elementUtils;
    }

    public Types getTypeUtils() {
        return typeUtils;
    }

    public Type getType(String qualifiedName) {
        return getType(elementUtils.getTypeElement(qualifiedName).asType());
    }

    public Type getType(TypeMirror typeMirror) {
        return new Type(this, typeMirror);
    }


}
