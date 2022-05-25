package io.codebot.apt.processor;

import io.codebot.apt.code.Annotations;
import io.codebot.apt.code.Fields;
import io.codebot.apt.code.Methods;
import io.codebot.apt.code.TypeOps;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class AbstractAnnotatedElementProcessor implements AnnotatedElementProcessor {
    protected ProcessingEnvironment processingEnv;
    protected TypeOps typeOps;
    protected Annotations annotationUtils;
    protected Methods methodUtils;
    protected Fields fieldUtils;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.typeOps = TypeOps.instanceOf(processingEnv);
        this.annotationUtils = Annotations.instanceOf(processingEnv);
        this.methodUtils = Methods.instanceOf(processingEnv);
        this.fieldUtils = Fields.instanceOf(processingEnv);
    }
}
