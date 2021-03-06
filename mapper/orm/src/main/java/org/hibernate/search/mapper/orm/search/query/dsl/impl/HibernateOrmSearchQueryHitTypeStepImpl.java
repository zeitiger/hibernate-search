/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.query.dsl.impl;

import org.hibernate.search.engine.search.query.dsl.SearchQueryHitTypeStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractDelegatingSearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.search.query.dsl.HibernateOrmSearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;
import org.hibernate.search.mapper.orm.search.loading.context.impl.HibernateOrmLoadingContext;
import org.hibernate.search.mapper.orm.common.EntityReference;

public class HibernateOrmSearchQueryHitTypeStepImpl<E>
		extends AbstractDelegatingSearchQueryHitTypeStep<EntityReference, E>
		implements HibernateOrmSearchQueryHitTypeStep<E> {
	private final HibernateOrmLoadingContext.Builder<E> loadingContextBuilder;

	public HibernateOrmSearchQueryHitTypeStepImpl(
			SearchQueryHitTypeStep<?, EntityReference, E, ?, ?> delegate,
			HibernateOrmLoadingContext.Builder<E> loadingContextBuilder) {
		super( delegate );
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public HibernateOrmSearchQueryHitTypeStep<E> fetchSize(int fetchSize) {
		loadingContextBuilder.fetchSize( fetchSize );
		return this;
	}

	@Override
	public HibernateOrmSearchQueryHitTypeStep<E> cacheLookupStrategy(EntityLoadingCacheLookupStrategy strategy) {
		loadingContextBuilder.cacheLookupStrategy( strategy );
		return this;
	}
}
