package org.springframework.data.jpa.domain;

import org.springframework.lang.Nullable;

class StringSpecificationComposition {

	public static <T> StringSpecification<T> composed(StringSpecification lhs, StringSpecification rhs, String op) {

		return (domainClass, query) -> {

			String thisSide = toStringPredicate(lhs, domainClass, query);
			String otherSide = toStringPredicate(rhs, domainClass, query);

			if (thisSide == null || thisSide.equals("")) {
				return otherSide;
			}

			return otherSide == null || otherSide.equals("") //
					? thisSide //
					: thisSide + op + otherSide;
		};
	}

	private static <T> String toStringPredicate(@Nullable StringSpecification<T> specification, Class<?> domainClass,
			String query) {
		return specification == null ? null : specification.toStringPredicate(domainClass, query);
	}
}
