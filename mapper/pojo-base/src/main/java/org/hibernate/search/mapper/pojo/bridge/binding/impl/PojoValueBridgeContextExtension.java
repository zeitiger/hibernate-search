/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.bridge.binding.impl;

import java.util.Optional;

import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.ToDocumentFieldValueConvertContextExtension;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.mapping.context.spi.AbstractPojoBackendMappingContext;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;

class PojoValueBridgeContextExtension
		implements ToDocumentFieldValueConvertContextExtension<ValueBridgeToIndexedValueContext>,
		FromDocumentFieldValueConvertContextExtension<ValueBridgeFromIndexedValueContext> {
	public static final PojoValueBridgeContextExtension INSTANCE = new PojoValueBridgeContextExtension();

	@Override
	public Optional<ValueBridgeToIndexedValueContext> extendOptional(ToDocumentFieldValueConvertContext original,
		BackendMappingContext mappingContext) {
		if ( mappingContext instanceof AbstractPojoBackendMappingContext ) {
			AbstractPojoBackendMappingContext pojoMappingContext = (AbstractPojoBackendMappingContext) mappingContext;
			return Optional.of( pojoMappingContext.getToIndexedValueContext() );
		}
		else {
			return Optional.empty();
		}
	}

	@Override
	public Optional<ValueBridgeFromIndexedValueContext> extendOptional(FromDocumentFieldValueConvertContext original,
			BackendSessionContext sessionContext) {
		if ( sessionContext instanceof AbstractPojoBackendSessionContext ) {
			AbstractPojoBackendSessionContext pojoSessionContext = (AbstractPojoBackendSessionContext) sessionContext;
			return Optional.of( pojoSessionContext.getValueBridgeFromIndexedValueContext() );
		}
		else {
			return Optional.empty();
		}
	}
}
