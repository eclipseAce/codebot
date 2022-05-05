package io.cruder.apt;

import com.google.common.collect.ImmutableSet;
import io.cruder.EntityService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Set;

@com.google.auto.service.AutoService(Processor.class)
public class EntityServiceProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(EntityService.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement serviceElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(EntityService.class))) {

        }
        return false;
    }
}
