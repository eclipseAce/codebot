package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import io.codebot.apt.crud.Entity;
import io.codebot.apt.crud.Service;
import io.codebot.apt.crud.ServiceGenerator;
import io.codebot.apt.type.Annotation;
import io.codebot.apt.type.Type;
import io.codebot.apt.type.TypeFactory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(CrudServiceProcessor.ANNOTATION_FQN)
public class CrudServiceProcessor extends AbstractProcessor {
    public static final String ANNOTATION_FQN = "io.codebot.CrudService";

    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.messager = processingEnv.getMessager();
        this.filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeFactory typeFactory = new TypeFactory(processingEnv);
        TypeElement annotation = elementUtils.getTypeElement(ANNOTATION_FQN);
        for (TypeElement serviceElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            Type serviceType = typeFactory.getType(serviceElement);
            try {
                Service service = new Service(serviceType);
                Entity entity = new Entity(
                        serviceType.findAnnotation(ANNOTATION_FQN)
                                .map(it -> typeFactory.getType(it.getValue("value")))
                                .get()
                );
                Type q = typeFactory.getType(
                        entity.getTypeName().packageName() + ".Q" + entity.getTypeName().simpleName()
                );
                new ServiceGenerator().generate(service, entity).writeTo(filer);
            } catch (Exception e) {
                messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        Throwables.getStackTraceAsString(e),
                        serviceElement,
                        serviceType.findAnnotation(ANNOTATION_FQN)
                                .map(Annotation::getAnnotationMirror)
                                .orElse(null)
                );
            }
        }
        return false;
    }
}
