package io.cruder.apt;

import com.squareup.javapoet.JavaFile;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.Script;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.io.IOException;
import java.util.List;

@Getter
@Setter
public abstract class CodegenScript extends Script {
    private ProcessingEnvironment processingEnv;

    private RoundEnvironment roundEnv;

    private List<String> args;

    public void codegen(@DelegatesTo(CodegenBuilder.class) Closure<?> cl)
            throws IOException {
        CodegenBuilder builder = new CodegenBuilder();
        cl.rehydrate(builder, cl.getOwner(), builder).call();
        for (JavaFile file : builder.build()) {
            file.writeTo(processingEnv.getFiler());
        }
    }

    public Elements getElementUtils() {
        return processingEnv.getElementUtils();
    }

    public Types getTypeUtils() {
        return processingEnv.getTypeUtils();
    }

    public TypeElement typeElementOf(CharSequence qualifiedName) {
        return getElementUtils().getTypeElement(qualifiedName);
    }
}
