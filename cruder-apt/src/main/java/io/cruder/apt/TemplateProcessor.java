package io.cruder.apt;

import com.google.auto.service.AutoService;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.tools.classfile.Type;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovyjarjarantlr4.v4.runtime.CharStreams;
import io.cruder.apt.builder.Builders;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Reader;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedAnnotationTypes({TemplateProcessor.TEMPLATE_ANNOTATION_NAME})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TemplateProcessor extends AbstractProcessor {
    public static final String TEMPLATE_ANNOTATION_NAME = "io.cruder.apt.Template";

    private ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Template.class))) {
                Template annotation = element.getAnnotation(Template.class);
                System.out.println(annotation.value());
                FileObject fo = processingEnv.getFiler()
                        .getResource(StandardLocation.CLASS_PATH, "", annotation.value());
                Binding binding = new Binding();
                binding.setVariable("__annotated", element);
                binding.setVariable("__roundEnv", roundEnv);
                binding.setVariable("__processingEnv", processingEnv);
                binding.setVariable("__types", processingEnv.getTypeUtils());
                binding.setVariable("__elements", processingEnv.getElementUtils());
                binding.setVariable("__messager", processingEnv.getMessager());
                binding.setVariable("__filer", processingEnv.getFiler());
                binding.setVariable("__builders", new Builders(processingEnv));
                GroovyShell shell = new GroovyShell(binding);
                try (Reader r = fo.openReader(true)) {
                    shell.evaluate(r);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
