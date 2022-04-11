package io.cruder.apt.builder;

import com.squareup.javapoet.*;
import io.cruder.apt.Template;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.function.Function;

@Setter
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor
public class JavaBeanBuilder {
    private final ProcessingEnvironment processingEnv;
    private final TypeElement entity;

    private String packageName;
    private String className;
    private boolean forceBoxedType;

    public JavaFile build() {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);

        for (ExecutableElement method : ElementFilter.methodsIn(entity.getEnclosedElements())) {
            String methodName = method.getSimpleName().toString();
            if (methodName.length() > 3 && methodName.startsWith("set")) {
                TypeMirror fieldType = method.getParameters().get(0).asType();
                if (fieldType.getKind().isPrimitive() && forceBoxedType) {
                    fieldType = processingEnv.getTypeUtils()
                            .boxedClass((PrimitiveType) fieldType).asType();
                }

                String getterPrefix = "get";
                if (fieldType.getKind() == TypeKind.BOOLEAN) {
                    getterPrefix = "is";
                }

                String fieldName = transformFirstLetter(methodName.substring(3), Character::toLowerCase);
                String getterName = getterPrefix + transformFirstLetter(fieldName, Character::toUpperCase);
                String setterName = "set" + transformFirstLetter(fieldName, Character::toUpperCase);

                FieldSpec field = FieldSpec
                        .builder(TypeName.get(fieldType), fieldName, Modifier.PRIVATE)
                        .build();
                MethodSpec getter = MethodSpec
                        .methodBuilder(getterName)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.get(fieldType))
                        .addCode("return $N;", fieldName)
                        .build();
                MethodSpec setter = MethodSpec
                        .methodBuilder(setterName)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(ParameterSpec.builder(TypeName.get(fieldType), fieldName).build())
                        .addCode("this.$N = $N;", fieldName, fieldName)
                        .build();

                typeBuilder.addField(field).addMethod(getter).addMethod(setter);
            }
        }

        return JavaFile.builder(packageName, typeBuilder.build()).build();
    }

    private static String transformFirstLetter(String str, Function<Character, Character> transformer) {
        if (str == null || str.length() < 1) {
            return str;
        }
        return transformer.apply(str.charAt(0)) + str.substring(1);
    }

    private static class FieldDescriptor {
        private String fieldName;
        private TypeName typeName;
    }
}
