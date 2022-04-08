package io.cruder.apt;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;
import spoon.support.visitor.clone.CloneVisitor;
import spoon.support.visitor.equals.CloneHelper;

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
        try {
            for (TypeElement annotation : annotations) {
                Launcher launcher = new Launcher();
                launcher.getEnvironment().setAutoImports(true);
                for (TypeElement e : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
                    launcher.addInputResource(getJavaSourceFile(e));
                }
                CtModel model = launcher.buildModel();

                ReplacingCloneHelper cloneHelper = new ReplacingCloneHelper();
                for (CtType<?> type : model.getAllTypes()) {
                    CtType<?> clone = cloneHelper.clone(type);
                    clone.setSimpleName("Generated" + type.getSimpleName());
                    clone.getFactory().Package()
                            .create(null, "generated")
                            .addType(clone);
                    System.out.println(clone.toStringWithImports());
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private VirtualFile getJavaSourceFile(TypeElement element) throws IOException {
        String pkg = ((PackageElement) element.getEnclosingElement()).getQualifiedName().toString();
        String file = element.getSimpleName() + ".java";

        FileObject source = processingEnv.getFiler()
                .getResource(StandardLocation.SOURCE_PATH, pkg, file);

        try (Reader reader = source.openReader(true)) {
            final StringBuilder builder = new StringBuilder();
            final char[] buf = new char[1024];
            int read;
            while ((read = reader.read(buf)) != -1) {
                builder.append(buf, 0, read);
            }
            return new VirtualFile(builder.toString(), pkg + "." + file);
        }
    }

    public class ReplacingCloneHelper extends CloneHelper {

        @Override
        public <T extends CtElement> T clone(T element) {
            CloneVisitor cloneVisitor = new TemplateCloneVisitor();
            cloneVisitor.scan(element);
            return cloneVisitor.getClone();
        }

        private class TemplateCloneVisitor extends CloneVisitor {
            public TemplateCloneVisitor() {
                super(ReplacingCloneHelper.this);
            }
        }

    }
}
