package io.codebot.apt.handler;

import com.squareup.javapoet.*;
import io.codebot.apt.annotation.Exposed;
import io.codebot.apt.code.*;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.util.Optional;

public class AutoExposeHandler implements AnnotationHandler {
    private static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";

    private static final String REST_CONTROLLER_FQN = "org.springframework.web.bind.annotation.RestController";
    private static final String REQUEST_METHOD_FQN = "org.springframework.web.bind.annotation.RequestMethod";
    private static final String REQUEST_MAPPING_FQN = "org.springframework.web.bind.annotation.RequestMapping";
    private static final String REQUEST_PARAM_FQN = "org.springframework.web.bind.annotation.RequestParam";
    private static final String REQUEST_BODY_FQN = "org.springframework.web.bind.annotation.RequestBody";
    private static final String PATH_VARIABLE_FQN = "org.springframework.web.bind.annotation.PathVariable";

    private static final String PAGEABLE_FQN = "org.springframework.data.domain.Pageable";

    private static final String TAG_FQN = "io.swagger.v3.oas.annotations.tags.Tag";
    private static final String OPERATION_FQN = "io.swagger.v3.oas.annotations.Operation";
    private static final String PAGEABLE_AS_QUERY_PARAM_FQN = "org.springdoc.core.converters.models.PageableAsQueryParam";

    @Override
    public void handle(ProcessingEnvironment processingEnv, Element element) throws Exception {
        Annotations annotationUtils = Annotations.instanceOf(processingEnv);
        Methods methodUtils = Methods.instanceOf(processingEnv);
        MethodCreators methodCreatorUtils = MethodCreators.instanceOf(processingEnv);

        DeclaredType exposedType = processingEnv.getTypeUtils().getDeclaredType((TypeElement) element));
        ClassName exposedName = ClassName.get((TypeElement) element);
        ClassName controllerName = ClassName.get(
                exposedName.packageName().replaceAll("[^.]+$", "controller"),
                exposedName.simpleName().replaceAll("Service$", "Controller")
        );

        Annotation exposedOnType = annotationUtils.find(element, Exposed.class);

        TypeCreator controllerCreator = TypeCreators
                .createClass(controllerName.packageName(), controllerName.simpleName())
                .addModifiers(Modifier.PUBLIC);
        controllerCreator.addAnnotation(AnnotationSpec
                .builder(ClassName.bestGuess(REST_CONTROLLER_FQN))
                .build()
        );
        String exposedPath = Optional.ofNullable(exposedOnType)
                .map(it -> StringUtils.trimToNull(it.getString("path")))
                .orElse(exposedName.simpleName());
        controllerCreator.addAnnotation(AnnotationSpec
                .builder(ClassName.bestGuess(REQUEST_MAPPING_FQN))
                .addMember("path", "$S", exposedPath)
                .build()
        );
        controllerCreator.addField(FieldSpec
                .builder(exposedName, "target", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build()
        );
        Optional.ofNullable(exposedOnType)
                .map(it -> StringUtils.trimToNull(it.getString("title")))
                .ifPresent(title -> controllerCreator.addAnnotation(AnnotationSpec
                        .builder(ClassName.bestGuess(TAG_FQN))
                        .addMember("name", "$S", title)
                        .build()
                ));
        for (Method method : methodUtils.allOf(exposedType)) {
            Annotation exposed = annotationUtils.find(method.getElement(), Exposed.class);
            if (exposed == null || !exposed.getBoolean("value")) {
                continue;
            }
            String title = exposed.getString("title");
            StringBuilder path = new StringBuilder(exposed.getString("path"));
            if (path.length() == 0) {
                path.append("/").append(method.getSimpleName());
            }

            MethodCreator methodCreator = methodCreatorUtils
                    .create(method.getSimpleName())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(method.getReturnType());

            if (!title.isEmpty()) {
                methodCreator.addAnnotation(AnnotationSpec
                        .builder(ClassName.bestGuess(OPERATION_FQN))
                        .addMember("summary", "$S", title)
                        .build()
                );
            }

            for (Parameter param : method.getParameters()) {
                ParameterSpec.Builder paramBuilder = ParameterSpec
                        .builder(TypeName.get(param.getType()), param.getName());
                if (annotationUtils.isPresent(param.getElement(), Exposed.Body.class)) {
                    paramBuilder.addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(REQUEST_BODY_FQN))
                            .build()
                    );
                } //
                else if (annotationUtils.isPresent(param.getElement(), Exposed.Param.class)) {
                    paramBuilder.addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(REQUEST_PARAM_FQN))
                            .addMember("name", "$S", param.getName())
                            .build()
                    );
                } //
                else if (annotationUtils.isPresent(param.getElement(), Exposed.Path.class)) {
                    paramBuilder.addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(PATH_VARIABLE_FQN))
                            .addMember("name", "$S", param.getName())
                            .build()
                    );
                    path.append("/{").append(param.getName()).append("}");
                } //
                else if (processingEnv.getTypeUtils().isAssignable(
                        param.getType(),
                        processingEnv.getTypeUtils().
                        )) {
                    methodCreator.addAnnotation(AnnotationSpec
                            .builder(ClassName.bestGuess(PAGEABLE_AS_QUERY_PARAM_FQN))
                            .build()
                    );
                }
                methodCreator.addParameter(paramBuilder.build());
            }

            methodCreator.addAnnotation(AnnotationSpec
                    .builder(ClassName.bestGuess(REQUEST_MAPPING_FQN))
                    .addMember("method", "$T.POST", ClassName.bestGuess(REQUEST_METHOD_FQN))
                    .addMember("path", "$S", path)
                    .build()
            );

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
}
