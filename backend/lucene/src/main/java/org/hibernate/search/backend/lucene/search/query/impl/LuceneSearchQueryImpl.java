/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneReadWorkOrchestrator;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchQuery;
import org.hibernate.search.backend.lucene.search.query.LuceneSearchResult;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.backend.lucene.work.impl.LuceneReadWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneSearcher;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.dsl.spi.DslExtensionState;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.spi.AbstractSearchQuery;
import org.hibernate.search.engine.search.query.SearchQueryExtension;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;



public class LuceneSearchQueryImpl<H> extends AbstractSearchQuery<H, LuceneSearchResult<H>>
		implements LuceneSearchQuery<H> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final LuceneReadWorkOrchestrator queryOrchestrator;
	private final LuceneWorkFactory workFactory;
	private final LuceneSearchContext searchContext;
	private final BackendSessionContext sessionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final Set<String> routingKeys;
	private final Query luceneQuery;
	private final Sort luceneSort;
	private final LuceneSearcher<LuceneLoadableSearchResult<H>> searcher;

	LuceneSearchQueryImpl(LuceneReadWorkOrchestrator queryOrchestrator,
			LuceneWorkFactory workFactory, LuceneSearchContext searchContext,
			BackendSessionContext sessionContext,
			LoadingContext<?, ?> loadingContext,
			Set<String> routingKeys,
			Query luceneQuery, Sort luceneSort,
			LuceneSearcher<LuceneLoadableSearchResult<H>> searcher) {
		this.queryOrchestrator = queryOrchestrator;
		this.workFactory = workFactory;
		this.searchContext = searchContext;
		this.sessionContext = sessionContext;
		this.loadingContext = loadingContext;
		this.routingKeys = routingKeys;
		this.luceneQuery = luceneQuery;
		this.luceneSort = luceneSort;
		this.searcher = searcher;
	}

	@Override
	public String getQueryString() {
		return luceneQuery.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[query=" + getQueryString() + ", sort=" + luceneSort + "]";
	}

	@Override
	public <Q> Q extension(SearchQueryExtension<Q, H> extension) {
		return DslExtensionState.returnIfSupported(
				extension, extension.extendOptional( this, loadingContext )
		);
	}

	@Override
	public LuceneSearchResult<H> fetch(Integer offset, Integer limit) {
		LuceneReadWork<LuceneLoadableSearchResult<H>> work = workFactory.search( searcher, offset, limit );
		return doSubmit( work )
				/*
				 * WARNING: the following call must run in the user thread.
				 * If we introduce async processing, we will have to add a loadAsync method here,
				 * as well as in ProjectionHitMapper and EntityLoader.
				 * This method may not be easy to implement for blocking mappers,
				 * so we may choose to throw exceptions for those.
				 */
				.loadBlocking();
	}

	@Override
	public long fetchTotalHitCount() {
		LuceneReadWork<Integer> work = workFactory.count( searcher );
		return doSubmit( work );
	}

	@Override
	public Explanation explain(String id) {
		Contracts.assertNotNull( id, "id" );

		Set<String> targetedIndexNames = searchContext.getIndexNames();
		if ( targetedIndexNames.size() != 1 ) {
			throw log.explainRequiresIndexName( targetedIndexNames );
		}

		return doExplain( targetedIndexNames.iterator().next(), id );
	}

	@Override
	public Explanation explain(String indexName, String id) {
		Contracts.assertNotNull( indexName, "indexName" );
		Contracts.assertNotNull( id, "id" );

		Set<String> targetedIndexNames = searchContext.getIndexNames();
		if ( !targetedIndexNames.contains( indexName ) ) {
			throw log.explainRequiresIndexTargetedByQuery( targetedIndexNames, indexName );
		}

		return doExplain( indexName, id );
	}

	private <T> T doSubmit(LuceneReadWork<T> work) {
		return queryOrchestrator.submit(
				searchContext.getIndexNames(),
				searchContext.getIndexManagerContexts(),
				routingKeys,
				work
		);
	}

	private Explanation doExplain(String indexName, String id) {
		Query explainedDocumentQuery = new BooleanQuery.Builder()
				.add( new TermQuery( new Term( LuceneFields.indexFieldName(), indexName ) ), BooleanClause.Occur.MUST )
				.add( new TermQuery( new Term( LuceneFields.idFieldName(), id ) ), BooleanClause.Occur.MUST )
				.build();
		explainedDocumentQuery = searchContext.decorateLuceneQuery(
				explainedDocumentQuery, sessionContext.getTenantIdentifier()
		);

		LuceneReadWork<Explanation> work = workFactory.explain(
				searcher, indexName, id, explainedDocumentQuery
		);
		return doSubmit( work );
	}
}
