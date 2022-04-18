package io.cruder.apt;

import com.google.auto.service.AutoService;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.cruder.apt.script.ProcessingScript;
import org.codehaus.groovy.control.CompilerConfiguration;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes({PreCompileProcessor.TEMPLATE_ANNOTATION_NAME})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PreCompileProcessor extends AbstractProcessor {
    public static final String TEMPLATE_ANNOTATION_NAME = "io.cruder.apt.PreCompile";

    private ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(PreCompile.class))) {
                PackageElement pkg = getPackageElement(element);
                PreCompile annotation = element.getAnnotation(PreCompile.class);

                CompilerConfiguration config = new CompilerConfiguration();
                config.setScriptBaseClass(ProcessingScript.class.getName());

                Binding binding = new Binding();
                binding.setVariable(ProcessingScript.ROUND_ENV_KEY, roundEnv);
                binding.setVariable(ProcessingScript.PROCESSING_ENV_KEY, processingEnv);
                binding.setVariable(ProcessingScript.TARGET_ELEMENT_KEY, element);
                GroovyShell shell = new GroovyShell(binding, config);

                FileObject file = getResource(annotation.script() + ".groovy");
                try (Reader r = file.openReader(true)) {
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

    private FileObject getResource(CharSequence relativeName) throws IOException {
        for (JavaFileManager.Location location : new JavaFileManager.Location[]{
                StandardLocation.SOURCE_PATH,
                StandardLocation.CLASS_PATH,
                StandardLocation.PLATFORM_CLASS_PATH
        }) {
            try {
                return processingEnv.getFiler().getResource(location, "", relativeName);
            } catch (FileNotFoundException e) {
            }
        }
        throw new FileNotFoundException(relativeName.toString());
    }
}
