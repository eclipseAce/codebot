package io.codebot.apt.handler;

import com.squareup.javapoet.*;
import io.codebot.apt.annotation.AutoExpose;
import io.codebot.apt.annotation.Exposed;
import io.codebot.apt.code.*;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.stream.Collectors;

public class AutoExposeHandler implements AnnotationHandler {
    private static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";
    private static final String SERVICE_FQN = "org.springframework.stereotype.Service";

    private static final String TRANSACTIONAL_FQN = "org.springframework.transaction.annotation.Transactional";
    private static final String REST_CONTROLLER_FQN = "org.springframework.web.bind.annotation.RestController";
    private static final String REQUEST_METHOD_FQN = "org.springframework.web.bind.annotation.RequestMethod";
    private static final String REQUEST_MAPPING_FQN = "org.springframework.web.bind.annotation.RequestMapping";
    private static final String REQUEST_PARAM_FQN = "org.springframework.web.bind.annotation.RequestParam";
    private static final String REQUEST_BODY_FQN = "org.springframework.web.bind.annotation.RequestBody";

    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    private static final String TAG_FQN = "io.swagger.v3.oas.annotations.tags.Tag";
    private static final String OPERATION_FQN = "io.swagger.v3.oas.annotations.Operation";
    private static final String PAGEABLE_AS_QUERY_PARAM_FQN = "org.springdoc.core.converters.models.PageableAsQueryParam";

    @Override
    public void handle(ProcessingEnvironment processingEnv, Element element) throws Exception {
        Annotations annotationUtils = Annotations.instance(processingEnv);

        TypeFactory typeFactory = new TypeFactory(processingEnv);
        Type exposedType = typeFactory.getType((TypeElement) element);
        Annotation autoExpose = annotationUtils.find(element, AutoExpose.class);
        if (autoExpose == null) {
            return;
        }
        String exposeTitle = autoExpose.getString("title");
        String exposePath = autoExpose.getString("path");

        ClassName exposedName = ClassName.get((TypeElement) element);
        ClassName controllerName = ClassName.get(
                exposedName.packageName().replaceAll("[^.]+$", "controller"),
                exposedName.simpleName().replaceAll("Service$", "Controller")
        );
        TypeCreator controllerCreator = TypeCreators
                .createClass(controllerName.packageName(), controllerName.simpleName())
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.bestGuess(REST_CONTROLLER_FQN))
                        .build()
                )
                .addAnnotation(AnnotationSpec
                        .builder(ClassName.bestGuess(REQUEST_MAPPING_FQN))
                        .addMember("path", "$S", exposePath)
                        .build()
                )
                .addField(FieldSpec
                        .builder(exposedName, "target", Modifier.PRIVATE)
                        .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                        .build()
                );
        if (!exposeTitle.isEmpty()) {
            controllerCreator.addAnnotation(AnnotationSpec
                    .builder(ClassName.bestGuess(TAG_FQN))
                    .addMember("name", "$S", exposeTitle)
                    .build()
            );
        }
        for (Method method : Methods.allOf(exposedType)) {
            Annotation exposed = annotationUtils.find(method.getElement(), Exposed.class);
            if (exposed == null || !exposed.getBoolean("value")) {
                continue;
            }
            String title = exposed.getString("title");
            String path = exposed.getString("path");
            if (path.isEmpty()) {
                path = "/" + method.getSimpleName();
            }

            MethodCreator methodCreator = MethodCreators
                    .create(method.getSimpleName())
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(REQUEST_MAPPING_FQN))
                            .addMember("method", "$T.POST", ClassName.bestGuess(REQUEST_METHOD_FQN))
                            .addMember("path", "$S", path)
                            .build()
                    )
                    .returns(method.getReturnType());

            if (!title.isEmpty()) {
                methodCreator.addAnnotation(AnnotationSpec
                        .builder(ClassName.bestGuess(OPERATION_FQN))
                        .addMember("summary", "$S", title)
                        .build()
                );
            }
            addParameters(method, methodCreator, annotationUtils);

            if (!method.getReturnType().isVoid()) {
                methodCreator.body().add("return ");
            }
            methodCreator.body().add("this.target.$N($L);\n",
                    method.getSimpleName(),
                    method.getParameters().stream()
                            .map(it -> it.asExpression().getCode())
                            .collect(CodeBlock.joining(", "))
            );

            controllerCreator.addMethod(methodCreator.create());
        }
        controllerCreator.create().writeTo(processingEnv.getFiler());
    }

    private void addParameters(Method method, MethodCreator creator, Annotations annotationUtils) {
        for (Parameter param : method.getParameters()) {
            if (annotationUtils.isPresent(param.getElement(), Exposed.Body.class)) {
                creator.addParameter(ParameterSpec
                        .builder(TypeName.get(param.getType().getTypeMirror()), param.getName())
                        .addAnnotation(AnnotationSpec
                                .builder(ClassName.bestGuess(REQUEST_BODY_FQN))
                                .build()
                        )
                        .build()
                );
            } //
            else if (annotationUtils.isPresent(param.getElement(), Exposed.Param.class)) {
                creator.addParameter(ParameterSpec
                        .builder(TypeName.get(param.getType().getTypeMirror()), param.getName())
                        .addAnnotation(AnnotationSpec
                                .builder(ClassName.bestGuess(REQUEST_PARAM_FQN))
                                .addMember("name", "$S", param.getName())
                                .build()
                        )
                        .build()
                );
            } //
            else {
                creator.addParameter(ParameterSpec
                        .builder(TypeName.get(param.getType().getTypeMirror()), param.getName())
                        .build()
                );
                if (param.getType().isAssignableTo(PAGEABLE_FQN)) {
                    creator.addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(PAGEABLE_AS_QUERY_PARAM_FQN))
                            .build()
                    );
                }
            }
        }
    }
}
