package io.cruder.autoservice.processor;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import io.cruder.autoservice.ServiceImplementor;
import io.cruder.autoservice.annotation.AutoService;
import io.cruder.autoservice.ServiceDescriptor;
import io.cruder.autoservice.Context;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.Set;

@com.google.auto.service.AutoService(Processor.class)
public class AutoServiceProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(AutoService.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Context ctx = new Context(processingEnv);
        for (TypeElement serviceElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(AutoService.class))) {
            try {
                ServiceDescriptor service = ServiceDescriptor.of(ctx, serviceElement);
                System.out.println(service);
                ServiceImplementor implementor = new ServiceImplementor(ctx, service);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("Error while processing AutoService for type %s, cause: %s\n%s",
                                serviceElement.asType(), e.getMessage(), Throwables.getStackTraceAsString(e)),
                        serviceElement, ctx.findAnnotation(serviceElement, AutoService.class.getName()).orElse(null));
            }

        }
        return false;
    }
}
