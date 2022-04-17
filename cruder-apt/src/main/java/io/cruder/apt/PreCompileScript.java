package io.cruder.apt;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.io.IOException;

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

    public void javaPoet(@DelegatesTo(JavaPoetBuilder.class) Closure<?> cl) throws IOException {
        JavaPoetBuilder builder = new JavaPoetBuilder();
        cl.rehydrate(builder, this, builder).call();
        builder.writeTo(getProcessingEnv().getFiler());
    }
}
