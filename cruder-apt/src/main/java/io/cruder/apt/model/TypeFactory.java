package io.cruder.apt.model;

import com.google.common.collect.ImmutableList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@RequiredArgsConstructor
public class TypeFactory {
    private final @Getter Elements elementUtils;
    private final @Getter Types typeUtils;

    public Type getType(TypeMirror typeMirror) {
        if (typeMirror.getKind().isPrimitive()) {
            return new Type(this, (PrimitiveType) typeMirror);
        }
        if (typeMirror.getKind() == TypeKind.DECLARED) {

        }
    }
}
