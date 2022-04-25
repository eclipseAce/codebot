package io.cruder.apt;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
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
public class CodegenProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(Codegen.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Codegen.class))) {
                Codegen annotation = element.getAnnotation(Codegen.class);

                CompilerConfiguration config = new CompilerConfiguration();
                config.setScriptBaseClass(CodegenScript.class.getName());

                GroovyShell shell = new GroovyShell(config);

                FileObject file = getResource(annotation.value() + ".groovy");
                try (Reader r = file.openReader(true)) {
                    CodegenScript script = (CodegenScript) shell.parse(r);
                    script.setProcessingEnv(processingEnv);
                    script.setRoundEnv(roundEnv);
                    script.setElement(element);
                    script.run();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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
