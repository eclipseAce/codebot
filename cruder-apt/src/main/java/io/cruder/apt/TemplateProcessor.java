package io.cruder.apt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;

import io.cruder.apt.util.AnnotationUtils;
import io.cruder.apt.util.ReplacingCloner;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.VirtualFolder;

@AutoService(Processor.class)
@SupportedAnnotationTypes({ "io.cruder.apt.Template" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TemplateProcessor extends AbstractProcessor {
    private Elements elements;
    private Types types;
    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elements = processingEnv.getElementUtils();
        types = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Template.class)) {

            Map<String, String> replaceTypes = Stream.of(element.getAnnotationsByType(ReplaceType.class))
                    .map(it -> new AbstractMap.SimpleEntry<>(
                            ((TypeElement) types.asElement(AnnotationUtils.getClassValue(it, ReplaceType::target)))
                                    .getQualifiedName().toString(),
                            ((TypeElement) types.asElement(AnnotationUtils.getClassValue(it, ReplaceType::with)))
                                    .getQualifiedName().toString()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            Template ann = element.getAnnotation(Template.class);

            List<TypeElement> templateTypes = AnnotationUtils.getClassValues(ann, Template::uses).stream()
                    .map(it -> (TypeElement) it)
                    .collect(Collectors.toList());

            try {
                VirtualFolder vf = new VirtualFolder();
                for (TypeElement te : templateTypes) {
                    vf.addFile(getJavaSourceFile(te));
                }

                Launcher launcher = new Launcher();
                launcher.getEnvironment().setAutoImports(true);
                launcher.addInputResource(vf);
                CtModel model = launcher.buildModel();

                CtPackage pkg = launcher.getFactory().Package()
                        .create(null, "io.cruder.example.generated");

                ReplacingCloner cloner = new ReplacingCloner(ann.regex(), ann.replacement(), replaceTypes,
                        templateTypes);

                for (CtType<?> type : model.getAllTypes()) {
                    CtType<?> clone = cloner.clone(type);
                    pkg.addType(clone);

                    JavaFileObject jfo = filer.createSourceFile(clone.getQualifiedName());
                    try (Writer w = jfo.openWriter()) {
                        w.append(clone.toStringWithImports());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private VirtualFile getJavaSourceFile(TypeElement typeElement) throws IOException {
        FileObject source = filer.getResource(
                StandardLocation.SOURCE_PATH,
                ((PackageElement) typeElement.getEnclosingElement()).getQualifiedName(),
                typeElement.getSimpleName() + ".java");

        try (Reader reader = source.openReader(true)) {
            final StringBuilder builder = new StringBuilder();
            final char[] buf = new char[1024];
            int read;
            while ((read = reader.read(buf)) != -1) {
                builder.append(buf, 0, read);
            }
            return new VirtualFile(builder.toString(), typeElement.getSimpleName() + ".java");
        }
    }

}
