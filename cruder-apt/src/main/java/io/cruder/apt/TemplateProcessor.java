package io.cruder.apt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.compiler.VirtualFile;
import spoon.support.visitor.clone.CloneVisitor;
import spoon.support.visitor.equals.CloneHelper;

@AutoService(Processor.class)
@SupportedAnnotationTypes({ TemplateProcessor.TEMPLATE_ANNOTATION_NAME })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TemplateProcessor extends AbstractProcessor {
    public static final String TEMPLATE_ANNOTATION_NAME = "io.cruder.apt.Template";
    public static final String REPLICA_ANNOTATION_NAME = "io.cruder.apt.Replica";
    public static final String REPLICAS_ANNOTATION_NAME = "io.cruder.apt.Replicas";

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

                Map<String, Replica[]> replicas = new HashMap<>();
                for (TypeElement e : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
                    launcher.addInputResource(loadSourceFile(e));
                    replicas.put(e.getQualifiedName().toString(), e.getAnnotationsByType(Replica.class));
                }
                CtModel model = launcher.buildModel();

                for (CtType<?> type : model.getAllTypes()) {
                    for (Replica replica : replicas.get(type.getQualifiedName())) {
                        ReplacaCloneHelper cloneHelper = new ReplacaCloneHelper(replica.replaces());

                        CtType<?> clone = cloneHelper.clone(type);
                        CtTypeReference<?> ref = clone.getFactory().Type()
                                .createReference(replica.name());
                        CtPackage pkg = clone.getFactory().Package()
                                .create(null, ref.getPackage().getQualifiedName());
                        clone.setSimpleName(ref.getSimpleName());
                        pkg.addType(clone);

                        saveSourceFile(clone);
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private VirtualFile loadSourceFile(TypeElement element) throws IOException {
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

    private void saveSourceFile(CtType<?> type) throws IOException {
        JavaFileObject source = processingEnv.getFiler()
                .createSourceFile(type.getQualifiedName());
        try (Writer w = source.openWriter()) {
            w.append(type.toStringWithImports());
        }
    }

    public class ReplacaCloneHelper extends CloneHelper {
        private final List<Replace> typeReplaces = new ArrayList<>();
        private final List<Replace> literalReplaces = new ArrayList<>();

        public ReplacaCloneHelper(Replace[] replaces) {
            for (Replace replace : replaces) {
                if (replace.type().equals("type") && replace.args().length == 2) {
                    typeReplaces.add(replace);
                } else if (replace.type().equals("literal") && replace.args().length == 2) {
                    literalReplaces.add(replace);
                }
            }
        }

        @Override
        public <T extends CtElement> T clone(T element) {
            CloneVisitor cloneVisitor = new ReplicaCloneVisitor();
            cloneVisitor.scan(element);
            return cloneVisitor.getClone();
        }

        private class ReplicaCloneVisitor extends CloneVisitor {
            public ReplicaCloneVisitor() {
                super(ReplacaCloneHelper.this);
            }

            @Override
            public <A extends Annotation> void visitCtAnnotation(CtAnnotation<A> annotation) {
                String name = annotation.getAnnotationType().getQualifiedName();
                if (TEMPLATE_ANNOTATION_NAME.equals(name)
                        || REPLICA_ANNOTATION_NAME.equals(name)
                        || REPLICAS_ANNOTATION_NAME.equals(name)) {
                    return;
                }
                super.visitCtAnnotation(annotation);
            }

            @Override
            public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
                String name = reference.getQualifiedName();
                for (Replace replace : typeReplaces) {
                    if (name.matches(replace.args()[0])) {
                        reference = reference.getFactory().Type()
                                .createReference(name.replaceAll(replace.args()[0], replace.args()[1]));
                        break;
                    }
                }
                super.visitCtTypeReference(reference);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> void visitCtLiteral(CtLiteral<T> literal) {
                if ("java.lang.String".equals(literal.getType().getQualifiedName())) {
                    String value = (String) literal.getValue();
                    for (Replace replace : literalReplaces) {
                        value = value.replaceAll(replace.args()[0], replace.args()[1]);
                    }
                    literal.setValue((T) value);
                }
                super.visitCtLiteral(literal);
            }
        }

    }
}
