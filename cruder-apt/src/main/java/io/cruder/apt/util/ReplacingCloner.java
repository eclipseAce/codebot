package io.cruder.apt.util;

import java.util.Map;

import com.google.common.collect.Maps;

import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.visitor.clone.CloneVisitor;
import spoon.support.visitor.equals.CloneHelper;

public class ReplacingCloner {
	private final String regex;
	private final String replacement;
	private final Map<String, String> typeReplaces = Maps.newHashMap();

	public ReplacingCloner(String regex, String replacement) {
		this.regex = regex;
		this.replacement = replacement;
	}

	public ReplacingCloner replaceType(String target, String with) {
		typeReplaces.put(target, with);
		return this;
	}

	public ReplacingCloner replaceTypes(Map<String, String> replaces) {
		typeReplaces.putAll(replaces);
		return this;
	}

	public <T extends CtElement> T clone(CtElement element) {
		CloneVisitor cv = new ReplacingCloneVisitor(new ReplacingCloneHelper());
		cv.scan(element);
		return cv.getClone();
	}

	private class ReplacingCloneVisitor extends CloneVisitor {
		public ReplacingCloneVisitor(ReplacingCloneHelper cloneHelper) {
			super(cloneHelper);
		}

		@Override
		public <T> void visitCtTypeReference(CtTypeReference<T> reference) {
			String qname = reference.getQualifiedName();
			String qnameReplace = typeReplaces.get(qname);
			System.out.println("Found reference: " + qname);
			if (qnameReplace == null) {
				super.visitCtTypeReference(reference);
			} else {
				CtTypeReference<T> referenceReplace = reference.getFactory().Type().createReference(qnameReplace);
				super.visitCtTypeReference(referenceReplace);
			}
		}
	}

	private class ReplacingCloneHelper extends CloneHelper {

		@Override
		public <T extends CtElement> T clone(T element) {
			ReplacingCloneVisitor cloneVisitor = new ReplacingCloneVisitor(this);
			cloneVisitor.scan(element);
			return cloneVisitor.getClone();
		}

		@Override
		public void tailor(CtElement topLevelElement, CtElement topLevelClone) {
			if (topLevelClone instanceof CtType) {
				CtType<?> type = (CtType<?>) topLevelClone;
				if (type.isTopLevel()) {
					type.setSimpleName(type.getSimpleName().replaceAll(regex, replacement));
				}
			}
		}

	}
}
