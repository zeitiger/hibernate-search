/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.analysis.impl.ScopedAnalyzer;
import org.hibernate.search.engine.backend.types.converter.spi.ToDocumentIdentifierValueConverter;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.CollectionHelper;

import org.apache.lucene.facet.FacetsConfig;


public class LuceneIndexModel implements AutoCloseable {

	private final String indexName;

	private final ToDocumentIdentifierValueConverter<?> idDslConverter;

	private final Map<String, LuceneIndexSchemaObjectNode> objectNodes;

	private final Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodes;

	private final ScopedAnalyzer scopedAnalyzer;

	private final FacetsConfig facetsConfig;

	public LuceneIndexModel(String indexName,
			ToDocumentIdentifierValueConverter<?> idDslConverter,
			Map<String, LuceneIndexSchemaObjectNode> objectNodesBuilder,
			Map<String, LuceneIndexSchemaFieldNode<?>> fieldNodesBuilder,
			ScopedAnalyzer scopedAnalyzer,
			FacetsConfig facetsConfig) {
		this.indexName = indexName;
		this.idDslConverter = idDslConverter;
		this.fieldNodes = CollectionHelper.toImmutableMap( fieldNodesBuilder );
		this.objectNodes = CollectionHelper.toImmutableMap( objectNodesBuilder );
		this.scopedAnalyzer = scopedAnalyzer;
		this.facetsConfig = facetsConfig;
	}

	@Override
	public void close() {
		scopedAnalyzer.close();
	}

	public String getIndexName() {
		return indexName;
	}

	public EventContext getEventContext() {
		return EventContexts.fromIndexName( indexName );
	}

	public ToDocumentIdentifierValueConverter<?> getIdDslConverter() {
		return idDslConverter;
	}

	public LuceneIndexSchemaFieldNode<?> getFieldNode(String absoluteFieldPath) {
		return fieldNodes.get( absoluteFieldPath );
	}

	public LuceneIndexSchemaObjectNode getObjectNode(String absolutePath) {
		return objectNodes.get( absolutePath );
	}

	public ScopedAnalyzer getScopedAnalyzer() {
		return scopedAnalyzer;
	}

	public FacetsConfig getFacetsConfig() {
		return facetsConfig;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "indexName=" ).append( indexName )
				.append( "]" )
				.toString();
	}
}
