package io.cruder.apt.builder;

import lombok.RequiredArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

@RequiredArgsConstructor
public class Builders {
    private final ProcessingEnvironment processingEnv;

    public JavaBeanBuilder javaBean(TypeElement element) {
        return new JavaBeanBuilder(processingEnv, element);
    }
}
