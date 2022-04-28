package io.cruder.autoservice.matcher;

import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class BaseMatcher {
    protected boolean matches = true;

    protected <T> boolean match(T target, Predicate<T> predicate) {
        if (matches) {
            matches = predicate.test(target);
        }
        return matches;
    }

    protected <M extends BaseMatcher> boolean match(M matcher, @Nullable List<M> matched, Consumer<M> closure) {
        if (matches) {
            closure.accept(matcher);
            matches = matcher.matches;
            if (matches && matched != null) {
                matched.add(matcher);
            }
        }
        return matches;
    }
}
