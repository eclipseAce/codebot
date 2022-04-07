package io.cruder.apt.mapstruct;

import static org.mapstruct.NullValueCheckStrategy.ON_IMPLICIT_CONVERSION;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mapstruct.Builder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.MappingInheritanceStrategy;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.control.MappingControl;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Mapper {

	Class<?>[] uses() default {};

	Class<?>[] imports() default {};

	ReportingPolicy unmappedSourcePolicy() default ReportingPolicy.IGNORE;

	ReportingPolicy unmappedTargetPolicy() default ReportingPolicy.WARN;

	ReportingPolicy typeConversionPolicy() default ReportingPolicy.IGNORE;

	String componentModel() default "default";

	String implementationName() default "<CLASS_NAME>Impl";

	String implementationPackage() default "<PACKAGE_NAME>";

	Class<?> config() default void.class;

	CollectionMappingStrategy collectionMappingStrategy() default CollectionMappingStrategy.ACCESSOR_ONLY;

	NullValueMappingStrategy nullValueMappingStrategy() default NullValueMappingStrategy.RETURN_NULL;

	NullValuePropertyMappingStrategy nullValuePropertyMappingStrategy() default NullValuePropertyMappingStrategy.SET_TO_NULL;

	MappingInheritanceStrategy mappingInheritanceStrategy() default MappingInheritanceStrategy.EXPLICIT;

	NullValueCheckStrategy nullValueCheckStrategy() default ON_IMPLICIT_CONVERSION;

	InjectionStrategy injectionStrategy() default InjectionStrategy.FIELD;

	boolean disableSubMappingMethodsGeneration() default false;

	Builder builder() default @Builder;

	Class<? extends Annotation> mappingControl() default MappingControl.class;

	Class<? extends Exception> unexpectedValueMappingException() default IllegalArgumentException.class;

}
