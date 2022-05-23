package io.codebot.apt.code;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

import javax.lang.model.element.Modifier;

public final class MethodWriters {
    private MethodWriters() {
    }

    public static MethodWriter overriding(Method method) {
        MethodSpec.Builder builder = MethodSpec.overriding(
                method.getElement(),
                method.getContainingType().asDeclaredType(),
                method.getContainingType().getFactory().getTypeUtils()
        );
        CodeWriter codeWriter = CodeWriters.create();
        method.getParameters().forEach(param -> codeWriter.allocateName(param.getName()));
        return new MethodWriterImpl(builder, codeWriter);
    }

    private static class MethodWriterImpl implements MethodWriter {
        private final MethodSpec.Builder builder;
        private final CodeWriter codeWriter;

        public MethodWriterImpl(MethodSpec.Builder builder, CodeWriter codeWriter) {
            this.builder = builder;
            this.codeWriter = codeWriter;
        }

        @Override
        public CodeWriter body() {
            return codeWriter;
        }

        @Override
        public MethodWriterImpl addModifiers(Modifier... modifiers) {
            builder.addModifiers(modifiers);
            return this;
        }

        @Override
        public MethodWriterImpl addAnnotation(AnnotationSpec annotation) {
            builder.addAnnotation(annotation);
            return this;
        }

        @Override
        public MethodWriter addParameter(ParameterSpec parameter) {
            builder.addParameter(parameter);
            return this;
        }

        @Override
        public MethodSpec getMethod() {
            return builder.build().toBuilder().addCode(codeWriter.getCode()).build();
        }
    }
}
