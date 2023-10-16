package org.springframework.data.jpa.domain.sample;

import org.springframework.data.jpa.domain.StringSpecification;

public class UserStringSpecifications {

	public static StringSpecification<User> userHasFirstname(final String firstname) {
		return simplePropertySpec("firstname", firstname);
	}

	public static StringSpecification<User> userHasLastname(final String lastname) {
		return simplePropertySpec("lastname", lastname);
	}

	public static StringSpecification<User> userHasFirstnameLike(final String expression) {
		return (domainClass, query) -> domainWithProperty(domainClass, "firstname") + " like "
				+ String.format("%%%s%%", expression);
	}

	public static StringSpecification<User> userHasAgeLess(final Integer age) {
		return (domainClass, query) -> domainWithProperty(domainClass, "age") + " < " + age;
	}

	public static StringSpecification<User> userHasLastnameLikeWithSort(final String expression) {

		return (domainClass, query) -> {
			// TODO: apply ORDER BY via side effect

			return domainWithProperty(domainClass, "lastname") + " like " + String.format("%%%s%%", expression);
		};
	}

	private static <T> StringSpecification<T> simplePropertySpec(final String property, final Object value) {

		if (String.class.equals(value.getClass())) {
			return (domainClass, query) -> domainWithProperty(domainClass, property) + " = '" + value + "'";
		}

		return (domainClass, query) -> domainWithProperty(domainClass, property) + " = " + value;
	}

	private static String domainWithProperty(Class<?> domainClass, String property) {

		String simpleAlias = domainClass.getSimpleName().substring(0, 1).toLowerCase();
		return simpleAlias + "." + property;
	}
}
