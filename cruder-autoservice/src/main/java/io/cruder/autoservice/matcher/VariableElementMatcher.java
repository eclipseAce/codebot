package io.cruder.autoservice.matcher;

import com.google.common.collect.Lists;
import io.cruder.autoservice.util.Models;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class VariableElementMatcher extends BaseMatcher {
    private final Models models;
    private final VariableElement variable;

    public VariableElementMatcher type(Consumer<TypeMirrorMatcher> matcher) {
        match(new TypeMirrorMatcher(models, variable.asType()), null, matcher);
        return this;
    }
}
