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
public class ExecutableElementMatcher extends BaseMatcher {
    private final Models models;
    private final ExecutableElement method;

    private final List<AnnotationMirror> matchedAnnotation = Lists.newArrayList();
    private final List<VariableElement> matchedParameters = Lists.newArrayList();

    public ExecutableElementMatcher returnType(Consumer<TypeMirrorMatcher> matcher) {
        match(new TypeMirrorMatcher(models, method.getReturnType()), null, matcher);
        return this;
    }

    public ExecutableElementMatcher hasParameter(Consumer<VariableElementMatcher> matcher) {
        for (VariableElement param : method.getParameters()) {
            match(new VariableElementMatcher(models, param), )
        }
    }
}
