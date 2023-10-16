package org.springframework.data.jpa.repository.query;

import static org.springframework.data.repository.query.parser.Part.Type.*;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Query creator to create a query from a {@link org.springframework.data.repository.query.parser.PartTree}.
 */
public class JpaStringBasedQueryCreator extends AbstractQueryCreator<String, String> {

	private final String query;
	private final ReturnedType returnedType;
	private final PartTree tree;
	private final Iterator<? extends Parameter> parameters;

	public JpaStringBasedQueryCreator(PartTree tree, Iterable<? extends Parameter> parameters, ReturnedType type) {

		super(tree);
		this.tree = tree;
		this.parameters = parameters.iterator();

		String stringQuery = createStringBasedQuery(tree, type);

		this.query = stringQuery;
		this.returnedType = type;
	}

	protected String createStringBasedQuery(PartTree tree, ReturnedType type) {

		Class<?> typeToRead = tree.isDelete() ? type.getDomainType() : type.getTypeToRead();

		return (typeToRead == null) || tree.isExistsProjection() //
				? createTupleQuery() //
				: createQuery(typeToRead);
	}

	private String createTupleQuery() {
		return null;
	}

	private String createQuery(Class<?> typeToRead) {
		return null;
	}

	public Class<?> getReturnedType() {
		return returnedType.getReturnedType();
	}

	@Override
	protected String create(Part part, Iterator<Object> iterator) {
		return toStringPredicate(part);
	}

	@Override
	protected String and(Part part, String base, Iterator<Object> iterator) {
		return base + " AND " + toStringPredicate(part);
	}

	@Override
	protected String or(String base, String predicate) {
		return base + " OR " + predicate;
	}

	@Override
	protected String complete(String criteria, @Nullable Sort sort) {

		String simpleName = returnedType.getDomainType().getSimpleName();
		String simpleAlias = simpleName.substring(0, 1).toLowerCase();
		String query = String.format("select %s from %s %s ", simpleAlias, simpleName, simpleAlias);

		if (criteria != null) {
			query += "where " + criteria;
		}

		if (sort != null && sort.isSorted()) {
			query += "order by " + String.join(",", QueryUtils.toOrders(sort, returnedType.getDomainType()));
		}

		return query;
	}

	Collection<String> getRequiredSelection(Sort sort, ReturnedType returnedType) {
		return returnedType.getInputProperties();
	}

	private String toStringPredicate(Part part) {
		return new StringBasedPredicateBuilder(part).build();
	}

	private class StringBasedPredicateBuilder {

		private final Part part;

		public StringBasedPredicateBuilder(Part part) {

			Assert.notNull(part, "Part must not be null");
			this.part = part;
		}

		public String build() {

			PropertyPath property = part.getProperty();
			Part.Type type = part.getType();

			String simpleName = property.getOwningType().getType().getSimpleName();
			String simpleAlias = simpleName.substring(0, 1).toLowerCase();

			switch (type) {
				case BETWEEN:
					Parameter first = parameters.next();
					Parameter second = parameters.next();
					return getComparablePath(part) + " between " + first.toString() + " and " + second.toString();
				case AFTER:
				case GREATER_THAN:
					return getComparablePath(part) + " > " + nextParameter();
				case GREATER_THAN_EQUAL:
					return getComparablePath(part) + " >= " + nextParameter();
				case BEFORE:
				case LESS_THAN:
					return getComparablePath(part) + " < " + nextParameter();
				case LESS_THAN_EQUAL:
					return getComparablePath(part) + " <= " + nextParameter();
				case IS_NULL:
					return getTypedPath(simpleAlias, part) + " IS NULL";
				case IS_NOT_NULL:
					return getTypedPath(simpleAlias, part) + " IS NOT NULL";
				case NOT_IN:
					return upperIfIgnoreCase(getTypedPath(simpleAlias, part)) + " NOT IN " + nextParameter();
				case IN:
					String typedPath = getTypedPath(simpleAlias, part);
					String s = upperIfIgnoreCase(typedPath) + " IN " + nextParameter();
					return s;
				case STARTING_WITH:
				case ENDING_WITH:
				case CONTAINING:
				case NOT_CONTAINING:

					if (property.getLeafProperty().isCollection()) {

						String propertyExpression = traversePath(simpleAlias, property);
						String parameterExpression = nextParameter();

						// Can't just call .not() in case of negation as EclipseLink chokes on that.
						return type.equals(NOT_CONTAINING) //
								? isNotMember(parameterExpression, propertyExpression) //
								: isMember(parameterExpression, propertyExpression);
					}

				case LIKE:
				case NOT_LIKE:
					String propertyExpression = upperIfIgnoreCase(getTypedPath(part));
					String parameterExpression = upperIfIgnoreCase(nextParameter());
					return type.equals(LIKE) //
							? propertyExpression + " LIKE " + parameterExpression //
							: propertyExpression + " NOT LIKE " + parameterExpression;
				case TRUE:
					return getTypedPath(simpleAlias, part) + " IS TRUE";
				case FALSE:
					return getTypedPath(simpleAlias, part) + " IS FALSE";
				case SIMPLE_PROPERTY:
					return upperIfIgnoreCase(getTypedPath(simpleAlias, part)) + " = " + upperIfIgnoreCase(nextParameter());
				case NEGATING_SIMPLE_PROPERTY:
					return upperIfIgnoreCase(getTypedPath(simpleAlias, part)) + " <> " + upperIfIgnoreCase(nextParameter());
				case IS_EMPTY:
				case IS_NOT_EMPTY:

					if (!property.getLeafProperty().isCollection()) {
						throw new IllegalArgumentException("IsEmpty / IsNotEmpty can only be used on collection properties");
					}

					String collectionPath = traversePath("", property);

					return type.equals(IS_NOT_EMPTY) //
							? isNotEmpty(collectionPath) //
							: isEmpty(collectionPath);
				default:
					throw new IllegalArgumentException("Unsupported keyword " + type);
			}
		}

		private String isNotMember(String parameterExpression, String propertyExpression) {
			return parameterExpression + " NOT MEMBER OF " + propertyExpression;
		}

		private String isMember(String parameterExpression, String propertyExpression) {
			return parameterExpression + " MEMBER OF " + propertyExpression;
		}

		private String isNotEmpty(String collectionPath) {
			return collectionPath + " is not empty";
		}

		private String isEmpty(String collectionPath) {
			return collectionPath + " is empty";
		}

		private String upperIfIgnoreCase(String typedPath) {

			switch (part.shouldIgnoreCase()) {
				case ALWAYS:

					Assert.state(canUpperCase(part.getProperty()),
							"Unable to ignore case of " + part.getProperty().getType().getName() + " types, the property '"
									+ part.getProperty().getSegment() + "' must reference a String");
					return "upper(" + typedPath + ")";
				case WHEN_POSSIBLE:

					if (canUpperCase(part.getProperty())) {
						return "upper(" + typedPath + ")";
					}
				case NEVER:
				default:
					return typedPath;
			}
		}

		private String nextParameter() {
			return ":" + parameters.next().getName().get();
		}

		private boolean canUpperCase(PropertyPath property) {
			return String.class.equals(property.getType());
		}

		private String getComparablePath(Part part) {
			return getTypedPath(part);
		}

		private String getTypedPath(@Nullable String prefix, Part part) {

			return prefix == null || prefix.equals("") //
					? part.getProperty().getSegment() //
					: prefix + "." + part.getProperty().getSegment();
		}

		private String getTypedPath(Part part) {
			return getTypedPath("", part);
		}

		private String traversePath(PropertyPath path) {
			return traversePath("", path);
		}

		private String traversePath(String totalPath, PropertyPath path) {

			String result = totalPath.equals("") ? path.getSegment() : totalPath + "." + path.getSegment();

			return path.hasNext() //
					? traversePath(result, path.next()) //
					: result;
		}

	}
}
