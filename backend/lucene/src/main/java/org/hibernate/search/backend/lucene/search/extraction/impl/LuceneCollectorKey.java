/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.extraction.impl;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TotalHitCountCollector;

/**
 * Tagging interface for collector keys.
 * @param <C> The type of collector.
 * @see LuceneCollectorsBuilder#addCollector(LuceneCollectorFactory)
 * @see LuceneCollectors#getCollectors()
 */
public interface LuceneCollectorKey<C extends Collector> {

	static <C extends Collector> LuceneCollectorKey<C> create() {
		return new LuceneCollectorKey<C>() {
		};
	}

	LuceneCollectorKey<TotalHitCountCollector> TOTAL_HIT_COUNT = create();

	LuceneCollectorKey<TopDocsCollector> TOP_DOCS = create();

}
