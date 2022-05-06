package io.cruder.apt.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import java.util.Iterator;
import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TypeIterator implements Iterator<TypeIterator> {
    private final DeclaredType type;
    private final TypeElement element;
    private final TypeResolver typeResolver;

    public static TypeIterator from(DeclaredType type, TypeResolver typeResolver) {
        return new TypeIterator(
                type,
                (TypeElement) type.asElement(),
                new TypeResolver(type, typeResolver)
        );
    }

    public static TypeIterator from(DeclaredType type) {
        return from(type, null);
    }

    @Override
    public boolean hasNext() {
        return element.getSuperclass().getKind() != TypeKind.NONE;
    }

    @Override
    public TypeIterator next() {
        if (!hasNext()) {
            throw new IllegalStateException("End of superclass chain");
        }
        return from((DeclaredType) element.getSuperclass(), typeResolver);
    }

    @Override
    public void forEachRemaining(Consumer<? super TypeIterator> action) {
        for (TypeIterator i = this; i.hasNext(); i = i.next()) {
            action.accept(i);
        }
    }
}
