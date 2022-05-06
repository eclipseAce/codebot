package io.cruder.apt;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import io.cruder.JpaService;
import io.cruder.apt.model.JpaServiceModel;
import io.cruder.apt.model.ModelContext;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.Set;

@AutoService(Processor.class)
public class EntityServiceProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(JpaService.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ModelContext ctx = new ModelContext(processingEnv);

        for (TypeElement serviceElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(JpaService.class))) {
            JpaServiceModel service = JpaServiceModel.serviceOf(ctx, ctx.typeUtils.getDeclaredType(serviceElement));
            System.out.println(service);
        }
        return false;
    }
}
