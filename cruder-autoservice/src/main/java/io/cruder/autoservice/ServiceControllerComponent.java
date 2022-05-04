package io.cruder.autoservice;

import com.squareup.javapoet.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Modifier;

@RequiredArgsConstructor
public class ServiceControllerComponent implements Component {
    private final @Getter ClassName name;
    private final ServiceDescriptor service;

    @Override
    public void init(ProcessingContext ctx) {
    }

    @Override
    public JavaFile createJavaFile() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassNames.SpringWeb.RestController)
                .addField(FieldSpec
                        .builder(service.getName(), "service", Modifier.PRIVATE)
                        .addAnnotation(ClassNames.Spring.Autowired)
                        .build());

        for (MethodDescriptor method : service.getMethods()) {
            MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getName())
                    .addModifiers(Modifier.PUBLIC);

            boolean bodyDetected = false;
            for (ParameterDescriptor param : method.getParameters().values()) {
                if (!bodyDetected && !param.isPageable() && param.isDeclaredType()) {
                    methodBuilder.addParameter(ParameterSpec
                            .builder(TypeName.get(param.getType()), param.getName())
                            .addAnnotation(ClassNames.SpringWeb.RequestBody)
                            .addAnnotation(ClassNames.JavaxValidation.Valid)
                            .build());
                    bodyDetected = true;
                } else if (param.isPageable()) {
                    methodBuilder.addParameter(TypeName.get(param.getType()), param.getName());
                } else {
                    methodBuilder.addParameter(ParameterSpec
                            .builder(TypeName.get(param.getType()), param.getName())
                            .addAnnotation(AnnotationSpec
                                    .builder(ClassNames.SpringWeb.RequestParam)
                                    .addMember("name", "$S", param.getName())
                                    .build())
                            .build());
                }
            }

            String path = String.format("/api/%s/%s",
                    StringUtils.uncapitalize(service.getEntity().getClassName().simpleName()),
                    method.getName());
            methodBuilder.addAnnotation(AnnotationSpec
                    .builder(ClassNames.SpringWeb.RequestMapping)
                    .addMember("method", "$T.$L", ClassNames.SpringWeb.RequestMethod, bodyDetected ? "POST" : "GET")
                    .addMember("path", "$S", path)
                    .build());
            methodBuilder.returns(ParameterizedTypeName.get(
                    ClassNames.SpringWeb.ResponseEntity,
                    TypeName.get(method.getReturnType()).box()));

            CodeBlock serviceCall = CodeBlock.of("service.$1N($2L)",
                    method.getName(),
                    String.join(", ", method.getParameters().keySet()));
            if (method.getResultKind() != MethodDescriptor.ResultKind.NONE) {
                methodBuilder.addStatement("return $1T.ok($2L)", ClassNames.SpringWeb.ResponseEntity, serviceCall);
            } else {
                methodBuilder.addStatement(serviceCall);
                methodBuilder.addStatement("return $1T.ok(null)", ClassNames.SpringWeb.ResponseEntity);
            }

            builder.addMethod(methodBuilder.build());
        }

        return JavaFile.builder(name.packageName(), builder.build()).build();
    }
}
