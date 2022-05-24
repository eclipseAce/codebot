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
    public final TypeElement ITERABLE_ELEMENT;
    public final TypeElement COLLECTION_ELEMENT;
    public final TypeElement LIST_ELEMENT;
    public final TypeElement MAP_ELEMENT;

    private final Elements elementUtils;
    private final Types typeUtils;

    public TypeFactory(ProcessingEnvironment processingEnv) {
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();

        this.ITERABLE_ELEMENT = elementUtils.getTypeElement(Iterable.class.getName());
        this.COLLECTION_ELEMENT = elementUtils.getTypeElement(Collection.class.getName());
        this.LIST_ELEMENT = elementUtils.getTypeElement(List.class.getName());
        this.MAP_ELEMENT = elementUtils.getTypeElement(Map.class.getName());
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
        return getType(ITERABLE_ELEMENT, elementType);
    }

    public Type getListType(TypeMirror elementType) {
        return getType(LIST_ELEMENT, elementType);
    }
}
