package io.cruder.apt;

import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

@Getter
@Setter
public abstract class CodegenScript extends Script {
    private final ConcurrentMap<ClassName, TypeSpec.Builder> typeBuilders = Maps.newConcurrentMap();

    private ProcessingEnvironment processingEnv;

    private RoundEnvironment roundEnv;

    private TypeElement element;

    public CodegenBuilder codegen(@DelegatesTo(CodegenBuilder.class) Closure<?> cl)
            throws IOException {
        CodegenBuilder builder = new CodegenBuilder();
        cl.rehydrate(builder, cl.getOwner(), builder).call();
        builder.writeTo(filer);
        return builder;
    }

    public Set<? extends Element> elementsAnnotatedWith(CharSequence qualifiedName) {
        return roundEnv.getElementsAnnotatedWith(processingEnv.getElementUtils().getTypeElement(qualifiedName));
    }
}
