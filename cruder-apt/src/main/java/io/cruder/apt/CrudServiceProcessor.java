package io.cruder.apt;

import com.google.auto.service.AutoService;
import io.cruder.CrudService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("io.cruder.CrudService")
public class CrudServiceProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement typeElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(CrudService.class))) {
        }
        return false;
    }
}
