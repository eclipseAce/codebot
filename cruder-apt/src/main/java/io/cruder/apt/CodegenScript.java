package io.cruder.apt;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.io.IOException;

@Getter
@Setter
public abstract class CodegenScript extends Script {
    private ProcessingEnvironment processingEnv;

    private RoundEnvironment roundEnv;

    private TypeElement element;

    public CodegenBuilder codegen(@DelegatesTo(CodegenBuilder.class) Closure<?> cl)
            throws IOException {
        return CodegenBuilder.codegen(processingEnv.getFiler(), cl);
    }
}
