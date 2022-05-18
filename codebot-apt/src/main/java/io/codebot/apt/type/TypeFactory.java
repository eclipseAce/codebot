package io.codebot.apt.type;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TypeFactory {
    private final TypeElement iterableElement;
    private final TypeElement collectionElement;
    private final TypeElement listElement;
    private final TypeElement mapElement;

    private final Elements elementUtils;
    private final Types typeUtils;

    public TypeFactory(ProcessingEnvironment processingEnv) {
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        this.iterableElement = elementUtils.getTypeElement(Iterable.class.getName());
        this.collectionElement = elementUtils.getTypeElement(Collection.class.getName());
        this.listElement = elementUtils.getTypeElement(List.class.getName());
        this.mapElement = elementUtils.getTypeElement(Map.class.getName());
    }

    public Elements getElementUtils() {
        return elementUtils;
    }

    public Types getTypeUtils() {
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

    public Type getIterableType(TypeMirror elementType) {
        return getType(iterableElement, elementType);
    }

    public Type getListType(TypeMirror elementType) {
        return getType(listElement, elementType);
    }
}
