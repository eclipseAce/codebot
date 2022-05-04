package io.cruder.autoservice;

import lombok.Getter;

import javax.lang.model.element.TypeElement;

public class EntityDescriptor extends BeanDescriptor {
    private final @Getter PropertyDescriptor idProperty;

    public EntityDescriptor(ProcessingContext ctx, TypeElement entity) {
        super(ctx, entity);
        this.idProperty = getProperties().values().stream()
                .filter(it -> it.isAnnotationPresent(ClassNames.JavaxPersistence.Id.canonicalName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't determin id field of entity " + entity));
    }
}
