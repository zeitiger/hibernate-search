/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.function.Supplier;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.mapper.pojo.processing.impl.PojoIndexingProcessor;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;

/**
 * @param <E> The entity type mapped to the index.
 * @param <D> The document type for the index.
 */
public final class PojoDocumentContributor<D extends DocumentElement, E> implements DocumentContributor<D> {

	private final PojoIndexingProcessor<E> processor;

	private final AbstractPojoBackendSessionContext sessionContext;

	private final Supplier<E> entitySupplier;

	public PojoDocumentContributor(PojoIndexingProcessor<E> processor, AbstractPojoBackendSessionContext sessionContext,
			Supplier<E> entitySupplier) {
		this.processor = processor;
		this.sessionContext = sessionContext;
		this.entitySupplier = entitySupplier;
	}

	@Override
	public void contribute(D state) {
		processor.process( state, entitySupplier.get(), sessionContext );
	}
}
