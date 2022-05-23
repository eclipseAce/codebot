package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.squareup.javapoet.*;
import io.codebot.apt.code.Methods;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.crud.JpaBuilder;
import io.codebot.apt.type.Annotation;
import io.codebot.apt.type.Executable;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(CrudServiceProcessor.CRUD_SERVICE_FQN)
public class CrudServiceProcessor extends AbstractProcessor {
    public static final String CRUD_SERVICE_FQN = "io.codebot.CrudService";

    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;
    private Filer filer;

    private TypeFactory typeFactory;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
        this.typeFactory = new TypeFactory(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement annotation = elementUtils.getTypeElement(CRUD_SERVICE_FQN);
        for (TypeElement serviceElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            Type serviceType = typeFactory.getType(serviceElement);
            try {
                generateServiceImpl(serviceType).writeTo(filer);
                generateController(serviceType).writeTo(filer);
            } catch (Exception e) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        Throwables.getStackTraceAsString(e),
                        serviceElement,
                        serviceType.findAnnotation(CRUD_SERVICE_FQN)
                                .map(Annotation::getAnnotationMirror)
                                .orElse(null)
                );
            }
        }
        return false;
    }


    private static final String JPA_REPOSITORY_FQN = "org.springframework.data.jpa.repository.JpaRepository";
    private static final String JPA_SPECIFICATION_EXECUTOR_FQN = "org.springframework.data.jpa.repository.JpaSpecificationExecutor";
    private static final String QUERYDSL_PREDICATE_EXECUTOR_FQN = "org.springframework.data.querydsl.QuerydslPredicateExecutor";

    private static final String SERVICE_FQN = "org.springframework.stereotype.Service";
    private static final String AUTOWIRED_FQN = "org.springframework.beans.factory.annotation.Autowired";

    private JavaFile generateServiceImpl(Type serviceType) {
        Entity entity = new Entity(
                serviceType.findAnnotation(CRUD_SERVICE_FQN)
                        .map(it -> typeFactory.getType(it.getValue("value")))
                        .get()
        );

        ClassName serviceName = ClassName.get(serviceType.asTypeElement());
        ClassName serviceImplName = ClassName.get(serviceName.packageName(), serviceName.simpleName() + "Impl");

        TypeSpec.Builder serviceBuilder = TypeSpec.classBuilder(serviceImplName);
        serviceBuilder.addModifiers(Modifier.PUBLIC);
        serviceBuilder.addAnnotation(ClassName.bestGuess(SERVICE_FQN));
        if (serviceType.isInterface()) {
            serviceBuilder.addSuperinterface(serviceName);
        } else {
            serviceBuilder.superclass(serviceName);
        }
        serviceBuilder.addField(FieldSpec
                .builder(ParameterizedTypeName.get(
                        ClassName.bestGuess(JPA_REPOSITORY_FQN),
                        entity.getTypeName(), entity.getIdTypeName().box()
                ), "repository", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build()
        );
        serviceBuilder.addField(FieldSpec
                .builder(ParameterizedTypeName.get(
                        ClassName.bestGuess(JPA_SPECIFICATION_EXECUTOR_FQN),
                        entity.getTypeName()
                ), "jpaSpecificationExecutor", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build()
        );
        serviceBuilder.addField(FieldSpec
                .builder(ParameterizedTypeName.get(
                        ClassName.bestGuess(QUERYDSL_PREDICATE_EXECUTOR_FQN),
                        entity.getTypeName()
                ), "querydslPredicateExecutor", Modifier.PRIVATE)
                .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                .build()
        );

        JpaBuilder builder = new JpaBuilder();
        builder.setEntity(entity);
        builder.setJpaRepository(CodeBlock.of("this.repository"));
        builder.setJpaSpecificationExecutor(CodeBlock.of("this.jpaSpecificationExecutor"));

        for (Executable method : serviceType.getMethods()) {
            if (method.getSimpleName().startsWith("create")) {
                serviceBuilder.addMethod(builder.create(Methods.of(serviceType, method.getElement())));
            } //
            else if (method.getSimpleName().startsWith("update")) {
                serviceBuilder.addMethod(builder.update(Methods.of(serviceType, method.getElement())));
            } //
            else if (method.getSimpleName().startsWith("find")) {
                serviceBuilder.addMethod(builder.query(Methods.of(serviceType, method.getElement())));
            }
        }
        return JavaFile.builder(serviceImplName.packageName(), serviceBuilder.build()).build();
    }

    private static final String REST_CONTROLLER_FQN = "org.springframework.web.bind.annotation.RestController";

    private JavaFile generateController(Type serviceType) {
        ClassName serviceName = ClassName.get(serviceType.asTypeElement());
        ClassName controllerName = ClassName.get(
                serviceName.packageName().replaceAll("[^.]+$", "controller"),
                serviceName.simpleName().replaceAll("Service$", "Controller")
        );
        TypeSpec.Builder controllerBuilder = TypeSpec.classBuilder(controllerName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.bestGuess(REST_CONTROLLER_FQN))
                .addField(FieldSpec
                        .builder(TypeName.get(serviceType.getTypeMirror()), "service")
                        .addModifiers(Modifier.PRIVATE)
                        .addAnnotation(ClassName.bestGuess(AUTOWIRED_FQN))
                        .build());
        return JavaFile.builder(controllerName.packageName(), controllerBuilder.build()).build();
    }
}
