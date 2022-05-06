package io.cruder.apt.model;

import javax.lang.model.element.TypeElement;
import java.util.List;

public class Bean {
    private final TypeElement element;
    private final List<Accessor> accessors;

    Bean(TypeElement element,
         List<Accessor> accessors) {
        this.element = element;
        this.accessors = accessors;
    }

    public TypeElement getElement() {
        return element;
    }

    public List<Accessor> getAccessors() {
        return accessors;
    }
}
