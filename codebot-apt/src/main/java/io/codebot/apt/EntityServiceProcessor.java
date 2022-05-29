package io.codebot.apt;

import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.codebot.apt.coding.*;
import lombok.RequiredArgsConstructor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class EntityServiceProcessor extends AbstractProcessor {
    private TypeOps typeOps;
    private Annotations annotationUtils;
    private Methods methodUtils;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(EntityService.class.getName());
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeOps = TypeOps.instanceOf(processingEnv);
        this.annotationUtils = Annotations.instanceOf(processingEnv);
        this.methodUtils = Methods.instanceOf(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(EntityService.class))) {
            Annotation annotation = annotationUtils.find(element, EntityService.class);
            try {
                process(element, annotation);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        Throwables.getStackTraceAsString(e),
                        element, annotation.getMirror()
                );
            }
        }
        return false;
    }

    protected void process(TypeElement element, Annotation annotation) throws IOException {
        Entity entity = Entity.of(processingEnv, annotation.getType("value"));
        MethodCollection methods = methodUtils.allOf(typeOps.getDeclared(element));
        for (Method method : methods) {
            if (method.getSimpleName().startsWith("create")) {

            }
        }
    }


}
