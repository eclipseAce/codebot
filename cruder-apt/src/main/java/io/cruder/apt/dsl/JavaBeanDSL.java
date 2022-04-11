package io.cruder.apt.dsl;

import com.squareup.javapoet.*;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Setter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class JavaBeanDSL {
    private final DSLContext dslContext;

    private String qualifiedName;
    private final Map<String, FieldSpec> fields = new LinkedHashMap<>();

    public void settersOf(TypeElement element) {
        for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
            if (isSetterMethod(method)) {
                String fieldName = lowerFirstLetter(method.getSimpleName().toString().substring(3));
                TypeMirror fieldType = method.getParameters().get(0).asType();
                if (fieldType.getKind().isPrimitive()) {
                    fieldType = dslContext.getTypeUtils()
                            .boxedClass((PrimitiveType) fieldType).asType();
                }
                fields.put(fieldName, FieldSpec
                        .builder(TypeName.get(fieldType), fieldName, Modifier.PRIVATE)
                        .build());
            }
        }
    }

    public void writeToFiler() throws IOException {
        String packageName = "";
        String className = qualifiedName;
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot > -1) {
            packageName = qualifiedName.substring(0, lastDot);
            className = qualifiedName.substring(lastDot + 1, qualifiedName.length());
        }
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);
        for (FieldSpec field : fields.values()) {
            typeBuilder.addField(field);

            String getterName = (field.type.equals(TypeName.BOOLEAN) ? "is" : "get")
                    + upperFirstLetter(field.name);
            typeBuilder.addMethod(MethodSpec
                    .methodBuilder(getterName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(field.type)
                    .addCode("return this.$N;", field.name)
                    .build());

            String setterName = "set" + upperFirstLetter(field.name);
            typeBuilder.addMethod(MethodSpec
                    .methodBuilder(setterName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(field.type, field.name).build())
                    .addCode("this.$N = $N;", field.name, field.name)
                    .build());
        }
        JavaFile.builder(packageName, typeBuilder.build())
                .build()
                .writeTo(dslContext.getFiler());
    }

    private boolean isSetterMethod(ExecutableElement method) {
        String methodName = method.getSimpleName().toString();
        return methodName.length() > 3
                && methodName.startsWith("set")
                && method.getParameters().size() == 1;
    }

    private String lowerFirstLetter(String text) {
        return text.isEmpty() ? text : Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private String upperFirstLetter(String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
