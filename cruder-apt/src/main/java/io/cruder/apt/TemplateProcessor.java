package io.cruder.apt;

import com.google.auto.service.AutoService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.cruder.apt.bean.BeanInfo;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Reader;
import java.util.Set;

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
                PackageElement pkg = getPackageElement(element);
                Template annotation = element.getAnnotation(Template.class);
                FileObject fo = processingEnv.getFiler()
                        .getResource(StandardLocation.SOURCE_PATH, pkg.getQualifiedName(), annotation.value());
                Binding binding = new Binding();
                binding.setProperty("__roundEnv", roundEnv);
                binding.setProperty("__processingEnv", processingEnv);
                binding.setProperty("__beanInfo", BeanInfo.introspect(element));
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

    private PackageElement getPackageElement(Element element) {
        while (element != null && element.getKind() != ElementKind.PACKAGE) {
            element = element.getEnclosingElement();
        }
        return (PackageElement) element;
    }
}
