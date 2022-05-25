package io.codebot.apt.processor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

public interface TypeElementProcessor {
    void init(ProcessingEnvironment processingEnv);

    void process(TypeElement element) throws Exception;
}
