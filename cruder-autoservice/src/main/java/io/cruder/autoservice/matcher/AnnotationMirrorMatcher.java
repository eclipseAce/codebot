package io.cruder.autoservice.matcher;

import io.cruder.autoservice.util.Models;
import lombok.RequiredArgsConstructor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

@RequiredArgsConstructor
public class AnnotationMirrorMatcher extends BaseMatcher {
    private final Models models;
    private final TypeMirror typeMirror;

}
