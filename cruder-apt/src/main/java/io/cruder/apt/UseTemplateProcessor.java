package io.cruder.apt;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;

import spoon.Launcher;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;

@AutoService(Processor.class)
@SupportedAnnotationTypes({ "io.cruder.apt.UseTemplate" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class UseTemplateProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Types typeUtils;
    private Trees treeUtils;
    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        treeUtils = Trees.instance(processingEnv);
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(UseTemplate.class)) {
            AnnotationMirror annotation = element.getAnnotationMirrors().stream()
                    .filter(it -> UseTemplate.class.getName().equals(it.getAnnotationType().toString()))
                    .findFirst().orElse(null);
            if (annotation != null) {
                TypeElement use = annotation.getElementValues().entrySet().stream()
                        .filter(it -> it.getKey().getSimpleName().toString().equals("value"))
                        .map(it -> (TypeElement) ((DeclaredType) it.getValue().getValue()).asElement())
                        .findFirst().orElse(null);
                Launcher launcher = new Launcher();
                try {
                    launcher.addInputResource(getJavaSourceFile(use));
                } catch (IOException e) {
                    messager.printMessage(Kind.ERROR, "Failed to read java source file: " + use.getQualifiedName());
                    return true;
                }
                
                for (CtType<?> t : launcher.buildModel().getAllTypes()) {
                    System.out.println(t);
                }
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
            return new VirtualFile(builder.toString());
        }
    }
}
