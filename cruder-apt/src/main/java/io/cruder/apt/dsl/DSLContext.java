package io.cruder.apt.dsl;

import groovy.lang.Closure;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@RequiredArgsConstructor
public class DSLContext {
    private final ProcessingEnvironment processingEnv;

    public void javaBean(Closure<?> cl) {
        JavaBeanDSL javaBean = new JavaBeanDSL(this);
        cl.setDelegate(javaBean);
        cl.call();
    }

    public Messager getMessager() {
        return processingEnv.getMessager();
    }

    public Filer getFiler() {
        return processingEnv.getFiler();
    }

    public Elements getElementUtils() {
        return processingEnv.getElementUtils();
    }

    public Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }
}
