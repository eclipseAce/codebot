package io.codebot.apt.type;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.Set;

public interface Modified {
    Set<Modifier> modifiers();

    default boolean hasModifiers(Modifier... modifiers) {
        return modifiers().containsAll(Arrays.asList(modifiers));
    }
}
