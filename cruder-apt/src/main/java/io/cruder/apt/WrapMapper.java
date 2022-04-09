package io.cruder.apt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Mapper;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface WrapMapper {
	Mapper value();
}
