package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import io.codebot.apt.annotation.Exposed;
import io.codebot.apt.handler.TypeElementProcessor;
import io.codebot.apt.handler.ExposedTypeElementProcessor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CodebotProcessor extends AbstractProcessor {
    private final Map<String, TypeElementProcessor> elementProcessors = Maps.newLinkedHashMap();

    public CodebotProcessor() {
        elementProcessors.put(Exposed.class.getName(), new ExposedTypeElementProcessor());
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementProcessors.values().forEach(it -> it.init(processingEnv));
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return elementProcessors.keySet();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
                TypeElementProcessor processor = elementProcessors.get(annotation.getQualifiedName().toString());
                if (processor != null) {
                    AnnotationMirror annotationMirror = element.getAnnotationMirrors().stream()
                            .filter(it -> annotation.equals(it.getAnnotationType().asElement()))
                            .findFirst().orElse(null);
                    try {
                        processor.process(element);
                    } catch (Exception e) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                Throwables.getStackTraceAsString(e),
                                element, annotationMirror
                        );
                    }
                }
            }
        }
        return false;
    }
}
