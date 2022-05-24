package io.codebot.apt.code;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import io.codebot.apt.type.Type;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.lang.model.element.Modifier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MethodCreators {

    public static MethodCreator create(String name) {
        return new MethodCreatorImpl(MethodSpec.methodBuilder(name), CodeWriters.create());
    }

    public static MethodCreator overriding(Method method) {
        MethodSpec.Builder builder = MethodSpec.overriding(
                method.getElement(),
                method.getContainingType().asDeclaredType(),
                method.getContainingType().getFactory().getTypeUtils()
        );
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
        public MethodCreator returns(Type type) {
            builder.returns(TypeName.get(type.getTypeMirror()));
            return this;
        }

        @Override
        public MethodSpec create() {
            return builder.build().toBuilder().addCode(codeWriter.getCode()).build();
        }
    }
}
