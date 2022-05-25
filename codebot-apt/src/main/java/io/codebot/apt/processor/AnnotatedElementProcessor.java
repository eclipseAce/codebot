package io.codebot.apt.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

public interface AnnotatedElementProcessor {
    void init(ProcessingEnvironment processingEnv);

    void process(Element element, AnnotationMirror annotationMirror) throws Exception;
}
