package io.cruder.apt;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(SOURCE)
@Target(TYPE)
@Repeatable(ReplaceStringLiterals.class)
public @interface ReplaceStringLiteral {
	String regex();

	String replacement();
}
