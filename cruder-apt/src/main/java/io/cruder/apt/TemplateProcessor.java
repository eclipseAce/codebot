package io.cruder.apt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;

import io.cruder.apt.util.AnnotationUtils;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;
import spoon.support.compiler.VirtualFolder;

@AutoService(Processor.class)
@SupportedAnnotationTypes({ "io.cruder.apt.Template" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TemplateProcessor extends AbstractProcessor {
    private ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(Template.class)) {
            try {
                Template template = element.getAnnotation(Template.class);

                TemplateCloner cloner = new TemplateCloner(
                        processingEnv, template,
                        element.getAnnotation(ReplaceTypeName.class),
                        element.getAnnotationsByType(ReplaceStringLiteral.class),
                        element.getAnnotationsByType(ReplaceType.class));

                VirtualFolder vf = new VirtualFolder();
                for (TypeMirror use : AnnotationUtils.getClassValues(template, Template::uses)) {
                    vf.addFile(getJavaSourceFile((TypeElement) processingEnv.getTypeUtils().asElement(use)));
                }

                Launcher launcher = new Launcher();
                launcher.getEnvironment().setAutoImports(true);
                launcher.addInputResource(vf);
                CtModel model = launcher.buildModel();

                for (CtType<?> type : model.getAllTypes()) {
                    CtType<?> clone = cloner.clone(type);

                    JavaFileObject jfo = processingEnv.getFiler()
                            .createSourceFile(clone.getQualifiedName());
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
        FileObject source = processingEnv.getFiler().getResource(
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
