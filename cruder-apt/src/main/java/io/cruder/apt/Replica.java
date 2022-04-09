package io.cruder.apt;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target(TYPE)
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
