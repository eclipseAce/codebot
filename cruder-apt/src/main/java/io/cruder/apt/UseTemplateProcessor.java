package io.cruder.apt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
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
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.compiler.VirtualFile;
import spoon.support.visitor.clone.CloneVisitor;
import spoon.support.visitor.equals.CloneHelper;
import spoon.support.visitor.replace.ReplacementVisitor;

@AutoService(Processor.class)
@SupportedAnnotationTypes({ "io.cruder.apt.UseTemplate" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class UseTemplateProcessor extends AbstractProcessor {
	private Messager messager;
	private Filer filer;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		messager = processingEnv.getMessager();
		filer = processingEnv.getFiler();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (Element element : roundEnv.getElementsAnnotatedWith(UseTemplate.class)) {
			try {
				AnnotationMirror annotation = element.getAnnotationMirrors().stream()
						.filter(it -> UseTemplate.class.getName().equals(it.getAnnotationType().toString()))
						.findFirst().orElse(null);
				if (annotation == null) {
					continue;
				}
				TypeElement use = annotation.getElementValues().entrySet().stream()
						.filter(it -> it.getKey().getSimpleName().toString().equals("value"))
						.map(it -> (TypeElement) ((DeclaredType) it.getValue().getValue()).asElement())
						.findFirst().orElse(null);

				Launcher launcher = new Launcher();
				launcher.addInputResource(getJavaSourceFile(use));
				launcher.getEnvironment().setAutoImports(true);
				CtModel model = launcher.buildModel();

				Factory factory = launcher.getFactory();
				CtTypeReference<Placeholder> phAnnotationRef = factory.Type()
						.createReference(Placeholder.class.getName());

				CtType<?> wrapType = model.getAllTypes().iterator().next();
				for (CtType<?> defType : wrapType.getNestedTypes()) {
					if (defType.getAnnotation(phAnnotationRef) != null) {
//						if (defType.getQualifiedName().equals("io.cruder.example.template.Template.Id")) {
//							ReplacementVisitor.replace(defType, factory.Type().LONG);
//						}
						continue;
					}
					
					CloneVisitor cv = new CloneVisitor(CloneHelper.INSTANCE) {
						public <T> void visitCtMethod(CtMethod<T> m) {
							System.out.println(m);
							super.visitCtMethod(m);
						}
						
						public <T> void visitCtClass(CtClass<T> ctClass) {
							System.out.println(ctClass);
							super.visitCtClass(ctClass);
						}
						
						public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
							System.out.println(reference.getQualifiedName());
							if ("io.cruder.example.template.Template.Id".equals(reference.getQualifiedName())) {
								super.visitCtTypeReference(factory.Type().LONG);
								return;
							}
							super.visitCtTypeReference(reference);
						}
					};
					cv.scan(defType);
					CtType<?> copied = cv.getClone();

					String qname = "io.cruder.example.generated.User" + defType.getSimpleName();
//
//					CtType<?> copied;
//					if (defType.isClass()) {
//						copied = factory.Class().create(qname);
//					} else if (defType.isInterface()) {
//						copied = factory.Interface().create(qname);
//					} else {
//						continue;
//					}

//					copied.addModifier(ModifierKind.PUBLIC);
//					for (CtField<?> field : defType.getFields()) {
//						copied.addField(field);
//					}
//					for (CtMethod<?> method : defType.getMethods()) {
//						copied.addMethod(method);
//					}

					JavaFileObject jfo = filer.createSourceFile(qname, use);
					try (Writer w = jfo.openWriter()) {
						w.append(copied.toString());
					}
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
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
