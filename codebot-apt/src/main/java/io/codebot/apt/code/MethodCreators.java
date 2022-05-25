package io.codebot.apt.code;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class MethodCreators {
    private final Types typeUtils;

    public static MethodCreators instanceOf(ProcessingEnvironment processingEnv) {
        return new MethodCreators(processingEnv.getTypeUtils());
    }

    public MethodCreator create(String name) {
        return new MethodCreatorImpl(MethodSpec.methodBuilder(name), CodeWriters.create());
    }

    public MethodCreator overriding(Method method) {
        MethodSpec.Builder builder = MethodSpec
                .overriding(method.getElement(), method.getContainingType(), typeUtils);
        CodeWriter codeWriter = CodeWriters.create();
        method.getParameters().forEach(param -> codeWriter.allocateName(param.getName()));
        return new MethodCreatorImpl(builder, codeWriter);
    }

    private static class MethodCreatorImpl implements MethodCreator {
        private final MethodSpec.Builder builder;
        private final CodeWriter codeWriter;

        public MethodCreatorImpl(MethodSpec.Builder builder, CodeWriter codeWriter) {
            this.builder = builder;
            this.codeWriter = codeWriter;
        }

        @Override
        public CodeWriter body() {
            return codeWriter;
        }

        @Override
        public MethodCreatorImpl addModifiers(Modifier... modifiers) {
            builder.addModifiers(modifiers);
            return this;
        }

        @Override
        public MethodCreatorImpl addAnnotation(AnnotationSpec annotation) {
            builder.addAnnotation(annotation);
            return this;
        }

        @Override
        public MethodCreator addParameter(ParameterSpec parameter) {
            builder.addParameter(parameter);
            return this;
        }

        @Override
        public MethodCreator returns(TypeMirror type) {
            builder.returns(TypeName.get(type));
            return this;
        }

        @Override
        public MethodSpec create() {
            return builder.build().toBuilder().addCode(codeWriter.getCode()).build();
        }
    }
}
