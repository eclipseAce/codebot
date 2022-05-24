package io.codebot.apt.code;

import com.squareup.javapoet.*;
import io.codebot.apt.type.Type;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.lang.model.element.Modifier;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeCreators {
    public static TypeCreator createClass(String packageName, String simpleName) {
        return new TypeCreatorImpl(packageName, TypeSpec.classBuilder(simpleName));
    }

    private static class TypeCreatorImpl implements TypeCreator {
        private final String packageName;
        private final TypeSpec.Builder builder;

        TypeCreatorImpl(String packageName, TypeSpec.Builder builder) {
            this.packageName = packageName;
            this.builder = builder;
        }

        @Override
        public TypeCreatorImpl addModifiers(Modifier... modifiers) {
            builder.addModifiers(modifiers);
            return this;
        }

        @Override
        public TypeCreatorImpl addAnnotation(AnnotationSpec annotation) {
            builder.addAnnotation(annotation);
            return this;
        }

        @Override
        public TypeCreatorImpl addMethod(MethodSpec method) {
            builder.addMethod(method);
            return this;
        }

        @Override
        public TypeCreator addField(FieldSpec field) {
            builder.addField(field);
            return this;
        }

        @Override
        public TypeCreator addFieldIfNameAbsent(String name, Supplier<FieldSpec> fieldSupplier) {
            for (FieldSpec existing : builder.fieldSpecs) {
                if (name.equals(existing.name)) {
                    return this;
                }
            }
            builder.addField(fieldSupplier.get());
            return this;
        }

        @Override
        public TypeCreator addSuperinterface(Type interfaceType) {
            builder.addSuperinterface(ClassName.get(interfaceType.asTypeElement()));
            return this;
        }

        @Override
        public TypeCreator superclass(Type classType) {
            builder.superclass(ClassName.get(classType.asTypeElement()));
            return this;
        }

        @Override
        public JavaFile create() {
            return JavaFile.builder(packageName, builder.build()).build();
        }
    }
}
