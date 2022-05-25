package io.codebot.apt.handler;

import io.codebot.apt.code.*;

import javax.annotation.processing.ProcessingEnvironment;

public abstract class AbstractTypeElementProcessor implements TypeElementProcessor {
    protected ProcessingEnvironment processingEnv;
    protected TypeOps typeOps;
    protected Annotations annotationUtils;
    protected Methods methodUtils;
    protected Fields fieldUtils;

    protected MethodCreators methodCreatorUtils;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.typeOps = TypeOps.instanceOf(processingEnv);
        this.annotationUtils = Annotations.instanceOf(processingEnv);
        this.methodUtils = Methods.instanceOf(processingEnv);
        this.fieldUtils = Fields.instanceOf(processingEnv);
        this.methodCreatorUtils = MethodCreators.instanceOf(processingEnv);
    }
}
