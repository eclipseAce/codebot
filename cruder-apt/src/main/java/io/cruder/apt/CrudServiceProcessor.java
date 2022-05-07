package io.cruder.apt;

import com.google.auto.service.AutoService;
import io.cruder.apt.model.Type;
import io.cruder.apt.model.TypeFactory;
import io.cruder.apt.util.AnnotationUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes(CrudServiceProcessor.CRUD_SERVICE_ANNOTATION)
public class CrudServiceProcessor extends AbstractProcessor {
    public static final String CRUD_SERVICE_ANNOTATION = "io.cruder.CrudService";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Elements elementUtils = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();
        TypeFactory typeFactory = new TypeFactory(processingEnv);

        TypeElement annotation = elementUtils.getTypeElement(CRUD_SERVICE_ANNOTATION);
        for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
            Type serviceType = typeFactory.getType(typeElement.asType());
            Type entityType = typeFactory.getType((TypeMirror) AnnotationUtils
                    .findAnnotationValue(typeElement, CRUD_SERVICE_ANNOTATION).get().getValue());

            System.out.println();
        }
        return false;
    }
}
