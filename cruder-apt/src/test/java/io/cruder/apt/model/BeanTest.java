package io.cruder.apt.model;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import java.util.Set;

class BeanTest {

    @Test
    void of() {
        Compilation compilation = Compiler.javac()
                .withProcessors(new TestProcessor())
                .compile(
                        JavaFileObjects.forResource("Dummy.java"),
                        JavaFileObjects.forResource("EntityL1.java"),
                        JavaFileObjects.forResource("EntityL2.java"),
                        JavaFileObjects.forResource("EntityL3.java")
                );

        CompilationSubject.assertThat(compilation).succeeded();
    }

    @SupportedAnnotationTypes("test.Dummy")
    static class TestProcessor extends AbstractProcessor {
        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            return false;
        }
    }
}