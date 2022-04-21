package io.cruder.apt.script;

import groovy.lang.Script;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

@Getter
@Setter
public abstract class ProcessingScript extends Script {
    private ProcessingEnvironment processingEnv;
    private RoundEnvironment roundEnv;
    private TypeElement element;
}
