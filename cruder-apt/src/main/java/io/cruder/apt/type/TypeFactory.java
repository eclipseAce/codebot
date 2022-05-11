package io.cruder.apt.type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
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

    public Elements elementUtils() {
        return elementUtils;
    }

    public Types typeUtils() {
        return typeUtils;
    }

    public Type getType(String qualifiedName, TypeMirror... typeArgs) {
        return getType(elementUtils.getTypeElement(qualifiedName), typeArgs);
    }

    public Type getType(TypeElement typeElement, TypeMirror... typeArgs) {
        return getType(typeUtils.getDeclaredType(typeElement, typeArgs));
    }

    public Type getType(TypeMirror typeMirror) {
        return new Type(this, typeMirror);
    }
}
