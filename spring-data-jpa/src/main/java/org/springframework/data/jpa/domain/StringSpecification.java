package org.springframework.data.jpa.domain;

import java.io.Serializable;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.lang.Nullable;

public interface StringSpecification<T> extends Serializable {

	long serialVersionUID = 1L;

	static <T> StringSpecification<T> not(@Nullable StringSpecification<T> spec) {

		return spec == null //
				? (domainClass, query) -> null //
				: (domainClass, query) -> "NOT " + spec.toStringPredicate(domainClass, query);
	}

	static <T> StringSpecification<T> where(@Nullable StringSpecification<T> spec) {
		return spec == null ? (domainClass, query) -> null : spec;
	}

	default StringSpecification<T> and(@Nullable StringSpecification<T> other) {
		return StringSpecificationComposition.composed(this, other, " and ");
	}

	default StringSpecification<T> or(@Nullable StringSpecification<T> other) {
		return StringSpecificationComposition.composed(this, other, " or ");
	}

	String toStringPredicate(Class<?> domainClass, String query);

	static <T> StringSpecification<T> allOf(Iterable<StringSpecification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(StringSpecification.where(null), StringSpecification::and);
	}

	@SafeVarargs
	static <T> StringSpecification<T> allOf(StringSpecification<T>... specifications) {
		return allOf(List.of(specifications));
	}

	static <T> StringSpecification<T> anyOf(Iterable<StringSpecification<T>> specifications) {

		return StreamSupport.stream(specifications.spliterator(), false) //
				.reduce(StringSpecification.where(null), StringSpecification::or);
	}

	@SafeVarargs
	static <T> StringSpecification<T> anyOf(StringSpecification<T>... specifications) {
		return anyOf(List.of(specifications));
	}

}
