package io.cruder.autoservice.processor;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import io.cruder.autoservice.Component;
import io.cruder.autoservice.ProcessingContext;
import io.cruder.autoservice.ServiceDescriptor;
import io.cruder.autoservice.annotation.AutoService;

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
        ProcessingContext ctx = new ProcessingContext(processingEnv);
        for (TypeElement serviceElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(AutoService.class))) {
            try {
                ctx.getServiceImplComponent(new ServiceDescriptor(ctx, serviceElement));
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("Error while processing AutoService for type %s, cause: %s\n%s",
                                serviceElement.asType(), e.getMessage(), Throwables.getStackTraceAsString(e)),
                        serviceElement,
                        ctx.utils.findAnnotation(serviceElement, AutoService.class.getName()).orElse(null));
            }
        }
        try {
            for (Component c : ctx.getComponents()) {
                c.createJavaFile().writeTo(ctx.processingEnv.getFiler());
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    String.format("Error while generating component, cause: %s\n%s",
                            e.getMessage(), Throwables.getStackTraceAsString(e)));
        }
        return false;
    }
}
