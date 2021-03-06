/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.loading.context.impl;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.mapper.javabean.search.loading.impl.JavaBeanProjectionHitMapper;
import org.hibernate.search.mapper.javabean.common.EntityReference;

public final class JavaBeanLoadingContext implements LoadingContext<EntityReference, Void> {

	private final ProjectionHitMapper<EntityReference, Void> projectionHitMapper;

	private JavaBeanLoadingContext(ProjectionHitMapper<EntityReference, Void> projectionHitMapper) {
		this.projectionHitMapper = projectionHitMapper;
	}

	@Override
	public ProjectionHitMapper<EntityReference, Void> getProjectionHitMapper() {
		return projectionHitMapper;
	}

	public static final class Builder implements LoadingContextBuilder<EntityReference, Void> {
		private final DocumentReferenceConverter<EntityReference> documentReferenceConverter;

		public Builder(DocumentReferenceConverter<EntityReference> documentReferenceConverter) {
			this.documentReferenceConverter = documentReferenceConverter;
		}

		@Override
		public LoadingContext<EntityReference, Void> build() {
			ProjectionHitMapper<EntityReference, Void> projectionHitMapper =
					new JavaBeanProjectionHitMapper( documentReferenceConverter );
			return new JavaBeanLoadingContext( projectionHitMapper );
		}
	}
}
