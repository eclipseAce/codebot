package io.cruder.apt.model;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
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
            Elements elementUtils = processingEnv.getElementUtils();
            Types typeUtils = processingEnv.getTypeUtils();
            ModelFactory factory = new ModelFactory(processingEnv);

            DeclaredType type = typeUtils.getDeclaredType(elementUtils.getTypeElement("test.EntityL3"));
            Bean bean = factory.getBean(type);

            System.out.println(ToStringBuilder.reflectionToString(bean, ToStringStyle.MULTI_LINE_STYLE));

            return false;
        }
    }
}