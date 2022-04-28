package io.cruder.autoservice.matcher;

import io.cruder.autoservice.util.Models;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

@RequiredArgsConstructor
public class TypeMirrorMatcher extends BaseMatcher {
    private final Models models;
    private final TypeMirror typeMirror;

    public TypeMirrorMatcher assignable(TypeMirror type) {
        match(typeMirror, it -> models.types.isAssignable(it, type));
        return this;
    }

    public TypeMirrorMatcher assignable(TypeElement type) {
        return assignable(type.asType());
    }

    public TypeMirrorMatcher assignable(CharSequence typeFqn) {
        return assignable(models.elements.getTypeElement(typeFqn));
    }

    public TypeMirrorMatcher assignable(TypeElement type, TypeMirror... typeArgs) {
        return assignable(models.types.getDeclaredType(type, typeArgs));
    }

    public TypeMirrorMatcher assignable(CharSequence typeFqn, TypeMirror... typeArgs) {
        return assignable(models.elements.getTypeElement(typeFqn), typeArgs);
    }
}
