/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.session.impl;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeContainedTypeContextProvider;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeIndexedTypeContextProvider;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexerImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoIndexingPlanImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public final class PojoSearchSessionDelegateImpl implements PojoSearchSessionDelegate {

	private final PojoScopeIndexedTypeContextProvider indexedTypeContextProvider;
	private final PojoScopeContainedTypeContextProvider containedTypeContextProvider;
	private final AbstractPojoBackendSessionContext backendSessionContext;

	public PojoSearchSessionDelegateImpl(PojoScopeIndexedTypeContextProvider indexedTypeContextProvider,
			PojoScopeContainedTypeContextProvider containedTypeContextProvider,
			AbstractPojoBackendSessionContext backendSessionContext) {
		this.indexedTypeContextProvider = indexedTypeContextProvider;
		this.containedTypeContextProvider = containedTypeContextProvider;
		this.backendSessionContext = backendSessionContext;
	}

	@Override
	public AbstractPojoBackendSessionContext getBackendSessionContext() {
		return backendSessionContext;
	}

	@Override
	public PojoIndexingPlan createIndexingPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoIndexingPlanImpl(
				indexedTypeContextProvider, containedTypeContextProvider, backendSessionContext,
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public PojoIndexer createIndexer(DocumentCommitStrategy commitStrategy) {
		return new PojoIndexerImpl( indexedTypeContextProvider, backendSessionContext, commitStrategy );
	}

}
