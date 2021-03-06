/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.types.dsl.impl;

import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.DataTypes;
import org.hibernate.search.backend.elasticsearch.document.model.esnative.impl.PropertyMapping;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchDoubleFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;

class ElasticsearchDoubleIndexFieldTypeOptionsStep
		extends AbstractElasticsearchScalarFieldTypeOptionsStep<ElasticsearchDoubleIndexFieldTypeOptionsStep, Double> {

	ElasticsearchDoubleIndexFieldTypeOptionsStep(ElasticsearchIndexFieldTypeBuildContext buildContext) {
		super( buildContext, Double.class, DataTypes.DOUBLE );
	}

	@Override
	protected ElasticsearchFieldCodec<Double> complete(PropertyMapping mapping) {
		return ElasticsearchDoubleFieldCodec.INSTANCE;
	}

	@Override
	protected ElasticsearchDoubleIndexFieldTypeOptionsStep thisAsS() {
		return this;
	}
}
