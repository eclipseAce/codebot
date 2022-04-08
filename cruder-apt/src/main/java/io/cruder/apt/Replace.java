package io.cruder.apt;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;

@Retention(SOURCE)
public @interface Replace {

	Type[] types() default {};

	Literal[] literals() default {};

	public @interface Type {
		Class<?> target();

		Class<?> type() default Object.class;

		String name() default "";
	}

	public @interface Literal {
		String regex();

		String replacement();
	}
}
