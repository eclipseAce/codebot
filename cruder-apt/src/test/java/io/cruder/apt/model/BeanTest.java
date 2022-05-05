package io.cruder.apt.model;

import com.google.common.collect.ImmutableSet;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import java.util.Set;

class BeanTest {

    @Test
    void of() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new AbstractProcessor() {
                    @Override
                    public Set<String> getSupportedAnnotationTypes() {
                        return ImmutableSet.of("test.Dummy");
                    }

                    @Override
                    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                        Bean bean = Bean.of(processingEnv, processingEnv.getElementUtils().getTypeElement("test.TestBean"));
                        return false;
                    }
                })
                .compile(
                        JavaFileObjects.forResource("Dummy.java"),
                        JavaFileObjects.forResource("TestBean.java")
                );

        CompilationSubject.assertThat(compilation).succeeded();
    }
}