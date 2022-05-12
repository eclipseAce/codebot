package io.codebot.apt.crud;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.Variable;

import javax.lang.model.element.Modifier;

public class ServiceGenerator {
    public JavaFile generate(Service service, Entity entity) {
        TypeSpec.Builder serviceBuilder = TypeSpec.classBuilder(service.getImplTypeName());
        serviceBuilder.addModifiers(Modifier.PUBLIC);
        serviceBuilder.addAnnotation(ClassName.bestGuess("org.springframework.stereotype.Service"));
        if (service.getType().isInterface()) {
            serviceBuilder.addSuperinterface(service.getTypeName());
        } else {
            serviceBuilder.superclass(service.getTypeName());
        }
        for (Executable method : service.getType().getMethods()) {
            MethodSpec.Builder methodBuilder = MethodSpec.overriding(method.getElement());
            if (method.getSimpleName().startsWith("create")) {
                CodeWriter writer = new CodeWriter(method);
                String entityVar = writer.newInstance("entity", entity.getType());
                for (Variable param : method.getParameters()) {
                    if ()
                }
            }
        }
    }
}
