package org.springframework.data.jpa.repository.query;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;

public class JpaStringBasedCountQueryCreator extends JpaStringBasedQueryCreator {

	private boolean distinct;

	public JpaStringBasedCountQueryCreator(PartTree tree, Iterable<? extends Parameter> parameters, ReturnedType type) {

		super(tree, parameters, type);

		this.distinct = tree.isDistinct();
	}

	@Override
	protected String complete(String criteria, Sort sort) {

		String simpleName = getReturnedType().getSimpleName();
		String simpleAlias = simpleName.substring(0, 1).toLowerCase();
		String query = String.format("select count(%s) from %s %s ", simpleAlias, simpleName, simpleAlias);

		if (criteria != null) {
			query += "where " + criteria;
		}

		return query;

	}
}
