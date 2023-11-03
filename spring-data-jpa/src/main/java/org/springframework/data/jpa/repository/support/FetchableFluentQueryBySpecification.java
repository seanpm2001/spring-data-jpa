/*
 * Copyright 2021-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor.SpecificationFluentQuery;
import org.springframework.data.jpa.repository.query.ScrollDelegate;
import org.springframework.data.jpa.support.PageableUtils;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Immutable implementation of {@link FetchableFluentQuery} based on a {@link Specification}. All methods that return a
 * {@link FetchableFluentQuery} will return a new instance, not the original.
 *
 * @param <S> Domain type
 * @param <R> Result type
 * @author Greg Turnquist
 * @since 3.0
 */
class FetchableFluentQueryBySpecification<S, R> extends FluentQuerySupport<S, R>
		implements SpecificationFluentQuery<R> {

	private final Specification<S> spec;
	private final Function<Sort, TypedQuery<S>> finder;
	private final SpecificationScrollDelegate<S> scroll;
	private final Function<Specification<S>, Long> countOperation;
	private final Function<Specification<S>, Boolean> existsOperation;
	private final EntityManager entityManager;

	private final List<Object> indexedParams;

	private final MapSqlParameterSource namedParams;

	private final SqlParameterSource namedParamSource;

	public FetchableFluentQueryBySpecification(Specification<S> spec, Class<S> entityType,
			Function<Sort, TypedQuery<S>> finder, SpecificationScrollDelegate<S> scrollDelegate,
			Function<Specification<S>, Long> countOperation, Function<Specification<S>, Boolean> existsOperation,
			EntityManager entityManager) {
		this(spec, entityType, (Class<R>) entityType, Sort.unsorted(), 0, Collections.emptySet(), finder, scrollDelegate,
				countOperation, existsOperation, entityManager, Collections.emptyList(), new MapSqlParameterSource(), null);
	}

	private FetchableFluentQueryBySpecification(Specification<S> spec, Class<S> entityType, Class<R> resultType,
			Sort sort, int limit, Collection<String> properties, Function<Sort, TypedQuery<S>> finder,
			SpecificationScrollDelegate<S> scrollDelegate, Function<Specification<S>, Long> countOperation,
			Function<Specification<S>, Boolean> existsOperation, EntityManager entityManager, List<Object> indexedParams,
			MapSqlParameterSource namedParams, @Nullable SqlParameterSource namedParamSource) {

		super(resultType, sort, limit, properties, entityType);
		this.spec = spec;
		this.finder = finder;
		this.scroll = scrollDelegate;
		this.countOperation = countOperation;
		this.existsOperation = existsOperation;
		this.entityManager = entityManager;
		this.indexedParams = indexedParams;
		this.namedParams = namedParams;
		this.namedParamSource = namedParamSource != null ? namedParamSource : namedParams;
	}

	@Override
	public SpecificationFluentQuery<R> sortBy(Sort sort) {

		Assert.notNull(sort, "Sort must not be null");

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, this.sort.and(sort), limit,
				properties, finder, scroll, countOperation, existsOperation, entityManager, indexedParams, namedParams,
				namedParamSource);
	}

	@Override
	public SpecificationFluentQuery<R> limit(int limit) {

		Assert.isTrue(limit >= 0, "Limit must not be negative");

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, this.sort.and(sort), limit,
				properties, finder, scroll, countOperation, existsOperation, entityManager, indexedParams, namedParams,
				namedParamSource);
	}

	@Override
	public <NR> SpecificationFluentQuery<NR> as(Class<NR> resultType) {

		Assert.notNull(resultType, "Projection target type must not be null");
		if (!resultType.isInterface()) {
			throw new UnsupportedOperationException("Class-based DTOs are not yet supported.");
		}

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, indexedParams, namedParams, namedParamSource);
	}

	@Override
	public SpecificationFluentQuery<R> project(Collection<String> properties) {

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, indexedParams, namedParams, namedParamSource);
	}

	@Override
	public SpecificationFluentQuery<R> params(List<?> values) {

		Assert.notNull(values, "Indexed values must not be null");

		List<Object> indexedParams = new ArrayList<>(this.indexedParams);
		indexedParams.addAll(values);

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, indexedParams, namedParams, namedParamSource);
	}

	@Override
	public SpecificationFluentQuery<R> params(Map<String, ?> paramMap) {

		Assert.notNull(paramMap, "Parameter map must not be null");

		Map<String, Object> newParams = new LinkedHashMap<>(namedParams.getValues());
		newParams.putAll(paramMap);

		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource(newParams);
		SqlParameterSource namedParamSource = this.namedParamSource == this.namedParams ? mapSqlParameterSource
				: this.namedParamSource;

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, indexedParams, namedParams, namedParamSource);
	}

	@Override
	public SpecificationFluentQuery<R> params(SqlParameterSource namedParamSource) {

		Assert.notNull(namedParamSource, "SqlParameterSource must not be null");

		return new FetchableFluentQueryBySpecification<>(spec, entityType, resultType, sort, limit, properties, finder,
				scroll, countOperation, existsOperation, entityManager, indexedParams, namedParams, namedParamSource);
	}

	@Override
	public R oneValue() {

		List<?> results = createSortedAndProjectedQuery() //
				.setMaxResults(2) // Never need more than 2 values
				.getResultList();

		if (results.size() > 1) {
			throw new IncorrectResultSizeDataAccessException(1);
		}

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public R firstValue() {

		List<?> results = createSortedAndProjectedQuery() //
				.setMaxResults(1) // Never need more than 1 value
				.getResultList();

		return results.isEmpty() ? null : getConversionFunction().apply(results.get(0));
	}

	@Override
	public List<R> all() {
		return convert(createSortedAndProjectedQuery().getResultList());
	}

	@Override
	public Window<R> scroll(ScrollPosition scrollPosition) {

		Assert.notNull(scrollPosition, "ScrollPosition must not be null");

		return scroll.scroll(sort, limit, scrollPosition).map(getConversionFunction());
	}

	@Override
	public Page<R> page(Pageable pageable) {
		return pageable.isUnpaged() ? new PageImpl<>(all()) : readPage(pageable);
	}

	@Override
	public Stream<R> stream() {

		return createSortedAndProjectedQuery() //
				.getResultStream() //
				.map(getConversionFunction());
	}

	@Override
	public long count() {
		return countOperation.apply(spec);
	}

	@Override
	public boolean exists() {
		return existsOperation.apply(spec);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private TypedQuery<S> createSortedAndProjectedQuery() {

		TypedQuery<S> query = finder.apply(sort);

		if (!properties.isEmpty()) {
			query.setHint(EntityGraphFactory.HINT, EntityGraphFactory.create(entityManager, entityType, properties));
		}

		if (limit != 0) {
			query.setMaxResults(limit);
		}

		if (useNamedParams()) {

			String[] parameterNames = namedParamSource.getParameterNames();
			if (parameterNames != null) {
				for (String parameterName : parameterNames) {
					query = query.setParameter(parameterName, namedParamSource.getValue(parameterName));
				}
			}
		} else {

			Set<Parameter<?>> parameters = query.getParameters();
			Iterator<Parameter<?>> iterator = parameters.iterator();
			for (int i = 0; i < indexedParams.size(); i++) {

				if (!iterator.hasNext()) {
					break;
				}

				query = query.setParameter((Parameter) iterator.next(), indexedParams.get(i));
			}
		}

		return query;
	}

	private boolean useNamedParams() {

		boolean hasNamedParams = (this.namedParams.hasValues() || this.namedParamSource != this.namedParams);

		if (hasNamedParams && !this.indexedParams.isEmpty()) {
			throw new IllegalStateException("Configure either named or indexed parameters, not both");
		}

		if (this.namedParams.hasValues() && this.namedParamSource != this.namedParams) {
			throw new IllegalStateException("Configure either individual named parameters or a SqlParameterSource, not both");
		}
		return hasNamedParams;
	}

	private Page<R> readPage(Pageable pageable) {

		TypedQuery<S> pagedQuery = createSortedAndProjectedQuery();

		if (pageable.isPaged()) {
			pagedQuery.setFirstResult(PageableUtils.getOffsetAsInteger(pageable));
			pagedQuery.setMaxResults(pageable.getPageSize());
		}

		List<R> paginatedResults = convert(pagedQuery.getResultList());

		return PageableExecutionUtils.getPage(paginatedResults, pageable, () -> countOperation.apply(spec));
	}

	private List<R> convert(List<S> resultList) {

		Function<Object, R> conversionFunction = getConversionFunction();
		List<R> mapped = new ArrayList<>(resultList.size());

		for (S s : resultList) {
			mapped.add(conversionFunction.apply(s));
		}
		return mapped;
	}

	private Function<Object, R> getConversionFunction() {
		return getConversionFunction(entityType, resultType);
	}

	static class SpecificationScrollDelegate<T> extends ScrollDelegate<T> {

		private final ScrollQueryFactory scrollFunction;

		SpecificationScrollDelegate(ScrollQueryFactory scrollQueryFactory, JpaEntityInformation<T, ?> entity) {
			super(entity);
			this.scrollFunction = scrollQueryFactory;
		}

		public Window<T> scroll(Sort sort, int limit, ScrollPosition scrollPosition) {

			Query query = scrollFunction.createQuery(sort, scrollPosition);

			if (limit > 0) {
				query = query.setMaxResults(limit);
			}

			return scroll(query, sort, scrollPosition);
		}
	}
}
