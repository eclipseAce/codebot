package io.cruder.apt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.util.ElementFilter;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.google.auto.service.AutoService;

import io.cruder.apt.wrap.WrapMapper;
import lombok.RequiredArgsConstructor;
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

				Map<String, List<ReplicaInfo>> replicas = new HashMap<>();
				for (TypeElement e : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(annotation))) {
					launcher.addInputResource(loadSourceFile(e));
					replicas.put(e.getQualifiedName().toString(), resolve(e.getAnnotation(Template.class)));
				}
				CtModel model = launcher.buildModel();

				for (CtType<?> type : model.getAllTypes()) {
					for (ReplicaInfo replica : replicas.get(type.getQualifiedName())) {
						ReplacaCloneHelper cloneHelper = new ReplacaCloneHelper(replica);

						CtType<?> clone = cloneHelper.clone(type);
						replica.rename(clone);

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

	private List<ReplicaInfo> resolve(Template annotation) {
		return accessClassValues(() -> annotation.value())
				.stream()
				.flatMap(it -> resolve(it).stream())
				.collect(Collectors.toList());
	}

	private List<ReplicaInfo> resolve(Element elem) {
		return Stream.of(elem.getAnnotationsByType(Replica.class))
				.map(replica -> {
					ReplicaInfo info = new ReplicaInfo();
					info.regex = Pattern.compile(replica.name().regex());
					info.replacement = replica.name().replacement();
					for (Replica.TypeRef r : replica.typeRefs()) {
						TypeRefReplacer replacer = new TypeRefReplacer();
						replacer.ref = getQualifiedName(accessClassValue(() -> r.target()));
						replacer.with = r.withName();
						if (replacer.with.isEmpty()) {
							replacer.with = getQualifiedName(accessClassValue(() -> r.withType()));
						}
						info.typeRefReplacers.add(replacer);
					}
					for (Replica.Literal r : replica.literals()) {
						LiteralReplacer replacer = new LiteralReplacer();
						replacer.regex = Pattern.compile(r.regex());
						replacer.replacement = r.replacement();
						info.literalReplacers.add(replacer);
					}
					return info;
				})
				.collect(Collectors.toList());
	}

	private List<TypeElement> accessClassValues(Supplier<Class<?>[]> access) {
		try {
			return Stream.of(access.get())
					.map(it -> processingEnv.getElementUtils().getTypeElement(it.getName()))
					.collect(Collectors.toList());
		} catch (MirroredTypesException e) {
			return e.getTypeMirrors().stream()
					.map(it -> (TypeElement) processingEnv.getTypeUtils().asElement(it))
					.collect(Collectors.toList());
		}
	}

	private TypeElement accessClassValue(Supplier<Class<?>> access) {
		return accessClassValues(() -> new Class<?>[] { access.get() }).get(0);
	}

	private String getQualifiedName(Element element) {
		List<String> names = new LinkedList<>();
		while (element.getKind() != ElementKind.PACKAGE) {
			names.add(0, element.getSimpleName().toString());
			element = element.getEnclosingElement();
		}
		return ((PackageElement) element).getQualifiedName().toString() + "." + String.join("$", names);
	}

	@RequiredArgsConstructor
	public class ReplacaCloneHelper extends CloneHelper {
		private final ReplicaInfo replica;

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
				if (TEMPLATE_ANNOTATION_NAME.equals(name)) {
					return;
				}
				else if (WrapMapper.class.getName().equals(name)) {
					super.visitCtAnnotation(annotation.getValue("value"));
				}
				else {
					super.visitCtAnnotation(annotation);
				}
			}

			@Override
			public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
				super.visitCtTypeReference(replica.replace(reference));
			}

			@Override
			public <T> void visitCtLiteral(CtLiteral<T> literal) {
				super.visitCtLiteral(replica.replace(literal));
			}
		}

	}

	@RequiredArgsConstructor
	private static class ReplicaInfo {
		private Pattern regex;
		private String replacement;
		private List<TypeRefReplacer> typeRefReplacers = new ArrayList<>();
		private List<LiteralReplacer> literalReplacers = new ArrayList<>();

		public void rename(CtType<?> type) {
			CtTypeReference<?> ref = type.getFactory().Type()
					.createReference(regex.matcher(type.getQualifiedName()).replaceAll(replacement));
			CtPackage pkg = type.getFactory().Package()
					.create(null, ref.getPackage().getQualifiedName());
			type.setSimpleName(ref.getSimpleName());
			pkg.addType(type);
		}

		public CtTypeReference<?> replace(CtTypeReference<?> origin) {
			String name = origin.getQualifiedName();
			for (TypeRefReplacer replace : typeRefReplacers) {
				if (name.equals(replace.ref)) {
					return origin.getFactory().Type().createReference(replace.with);
				}
			}
			return origin;
		}

		public CtLiteral<?> replace(CtLiteral<?> origin) {
			if ("java.lang.String".equals(origin.getType().getQualifiedName())) {
				String value = (String) origin.getValue();
				for (LiteralReplacer replace : literalReplacers) {
					value = replace.regex.matcher(value).replaceAll(replace.replacement);
				}
				return origin.getFactory().createLiteral(value);
			}
			return origin;
		}
	}

	private static class TypeRefReplacer {
		private String ref;
		private String with;
	}

	private static class LiteralReplacer {
		private Pattern regex;
		private String replacement;
	}
}
