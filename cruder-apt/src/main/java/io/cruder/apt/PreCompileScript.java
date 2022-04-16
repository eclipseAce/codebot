package io.cruder.apt;

import groovy.lang.Script;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

public abstract class PreCompileScript extends Script {
    public static final String PROCESSING_ENV_KEY = "__processingEnv";
    public static final String ROUND_ENV_KEY = "__roundEnv";
    public static final String TARGET_ELEMENT_KEY = "__targetElement";

    public ProcessingEnvironment getProcessingEnv() {
        return (ProcessingEnvironment) getBinding().getVariable(PROCESSING_ENV_KEY);
    }

    public RoundEnvironment getRoundEnv() {
        return (RoundEnvironment) getBinding().getVariable(ROUND_ENV_KEY);
    }

    public TypeElement getTargetElement() {
        return (TypeElement) getBinding().getVariable(TARGET_ELEMENT_KEY);
    }
}
