package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import io.codebot.apt.annotation.AutoCrud;
import io.codebot.apt.annotation.AutoExpose;
import io.codebot.apt.handler.AnnotationHandler;
import io.codebot.apt.handler.AutoExposeHandler;
import io.codebot.apt.handler.JpaAutoCrudHandler;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
        "io.codebot.apt.annotation.AutoExpose",
        "io.codebot.apt.annotation.AutoCrud"
})
public class CodebotProcessor extends AbstractProcessor {
    private Map<String, Class<? extends AnnotationHandler>> annotationHandlers;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        annotationHandlers = Maps.newLinkedHashMap();
        annotationHandlers.put(AutoExpose.class.getName(), AutoExposeHandler.class);
        annotationHandlers.put(AutoCrud.class.getName(), JpaAutoCrudHandler.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element annotated : roundEnv.getElementsAnnotatedWith(annotation)) {
                Class<? extends AnnotationHandler> handlerClass = annotationHandlers
                        .get(annotation.getQualifiedName().toString());
                if (handlerClass != null) {
                    AnnotationMirror annotationMirror = annotated.getAnnotationMirrors().stream()
                            .filter(it -> annotation.equals(it.getAnnotationType().asElement()))
                            .findFirst().orElse(null);
                    try {
                        AnnotationHandler handler = handlerClass.getConstructor().newInstance();
                        handler.handle(processingEnv, annotated);
                    } catch (Exception e) {
                        processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                Throwables.getStackTraceAsString(e),
                                annotated, annotationMirror
                        );
                    }
                }
            }
        }
        return false;
    }
}
