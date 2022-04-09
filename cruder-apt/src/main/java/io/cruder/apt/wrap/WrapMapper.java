package io.cruder.apt.wrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Mapper;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface WrapMapper {
	Mapper value();
}
