package io.codebot.apt.handler;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

public interface AnnotationHandler {
    void handle(ProcessingEnvironment processingEnv, Element element) throws Exception;
}
