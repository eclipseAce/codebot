package io.cruder.apt.util;

import java.util.LinkedList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

public abstract class TypeNameUtils {
	private TypeNameUtils() {
	}

	public static String getSimpleName(TypeElement te) {
		List<String> names = new LinkedList<>();
		for (Element i = te; i.getKind() != ElementKind.PACKAGE; i = i.getEnclosingElement()) {
			names.add(0, i.getSimpleName().toString());
		}
		return String.join("$", names);
	}

	public static String getQualifiedName(TypeElement te) {
		String sname = getSimpleName(te);
		String qname = te.getQualifiedName().toString();
		return qname.substring(0, qname.length() - sname.length()) + sname;
	}
}
