package io.cruder.apt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@Repeatable(Replicas.class)
public @interface Replica {
	Name name();

	TypeRef[] typeRefs() default {};

	Literal[] literals() default {};

	@interface Name {
		String regex();

		String replacement();
	}

	@interface TypeRef {
		Class<?> target();

		Class<?> withType() default Object.class;

		String withName() default "";
	}

	@interface Literal {
		String regex();

		String replacement();
	}
}
