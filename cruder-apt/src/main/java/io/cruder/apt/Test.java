package io.cruder.apt;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.support.compiler.VirtualFile;

public class Test {

	public static void main(String[] args) {
		String source = ""
				+ "package io.cruder.apt;"
				+ "import test.ArrayList;\r\n"
				+ "import java.util.stream.Collectors;"
				+ "public class Test1 {\r\n"
				+ "	public ArrayList<String> get() {\r\n"
				+ "		return new ArrayList<String>().stream()"
				+ "             .collect(Collectors.collectingAndThen(Collectors.toList(), list -> new ArrayList<>()));\r\n"
				+ "	}\r\n"
				+ "}";
		Launcher launcher = new Launcher();
		launcher.addInputResource(new VirtualFile(source));
		launcher.getEnvironment().setNoClasspath(true);
		CtModel model = launcher.buildModel();
		
		CtType<?> wrapType = model.getAllTypes().iterator().next();
		
		for (CtMethod<?> method : wrapType.getMethods()) {
			System.out.println(method);
		}
	}

}

