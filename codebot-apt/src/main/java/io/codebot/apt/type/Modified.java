package io.codebot.apt.type;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Set;

public interface Modified {
    Set<Modifier> getModifiers();

    default boolean hasModifiers(Modifier... modifiers) {
        return getModifiers().containsAll(Arrays.asList(modifiers));
    }
}
