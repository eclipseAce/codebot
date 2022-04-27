package io.cruder.apt;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@AutoService(Processor.class)
public class CodegenProcessor extends AbstractProcessor {
    private final ConcurrentMap<ClassName, TypeSpec.Builder> typeBuilders = Maps.newConcurrentMap();

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
        for (TypeElement element : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Codegen.class))) {
            Codegen annotation = element.getAnnotation(Codegen.class);
            try {
                CompilerConfiguration config = new CompilerConfiguration();
                config.setScriptBaseClass(CodegenScript.class.getName());

                GroovyShell shell = new GroovyShell(config);

                FileObject file = getResource(annotation.script() + ".groovy");
                try (Reader r = file.openReader(true)) {
                    CodegenScript script = (CodegenScript) shell.parse(r);
                    script.setProcessingEnv(processingEnv);
                    script.setRoundEnv(roundEnv);
                    script.setArgs(ImmutableList.copyOf(annotation.args()));
                    script.run();
                }
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("Error while executing codegen script '%s' for type %s, cause: %s\n%s",
                                annotation.script(), element.asType(), e.getMessage(), Throwables.getStackTraceAsString(e)),
                        element, MoreElements.getAnnotationMirror(element, Codegen.class).orNull());
                break;
            }
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
