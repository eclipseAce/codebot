package io.cruder.apt.util;

import com.google.common.collect.Maps;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import java.util.Map;

public class TypeResolver {
    private final Map<TypeVariable, TypeMirror> typeVars = Maps.newHashMap();

    public TypeResolver(DeclaredType declaredType, TypeResolver parent) {
        TypeElement element = (TypeElement) declaredType.asElement();
        for (int i = 0; i < element.getTypeParameters().size(); i++) {
            TypeVariable typeVar = (TypeVariable) element.getTypeParameters().get(i).asType();
            TypeMirror type = declaredType.getTypeArguments().get(i);
            if (parent != null) {
                type = parent.resolve(type);
            }
            typeVars.put(typeVar, type);
        }
    }

    public TypeMirror resolve(TypeMirror type) {
        if (type.getKind() == TypeKind.TYPEVAR) {
            TypeVariable typeVar = (TypeVariable) type;
            if (typeVars.containsKey(typeVar)) {
                return typeVars.get(typeVar);
            }
            TypeMirror upper = typeVar.getUpperBound();
            if (upper.getKind() == TypeKind.INTERSECTION) {
                IntersectionType inter = (IntersectionType) upper;
                return inter.getBounds().get(0);
            }
            return upper;
        }
        return type;
    }
}