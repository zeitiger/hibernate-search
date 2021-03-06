/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeContainedTypeContext;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoContainedTypeExtendedMappingCollector;

class HibernateOrmContainedTypeContext<E> extends AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmScopeContainedTypeContext<E> {

	private HibernateOrmContainedTypeContext(HibernateOrmContainedTypeContext.Builder<E> builder) {
		super( builder.javaClass, builder.entityName );
	}

	@Override
	public Object toIndexingPlanProvidedId(Object entityId) {
		// The concept of document ID is not relevant for contained types,
		// so we always provide the entity ID to indexing plans
		return entityId;
	}

	static class Builder<E> implements PojoContainedTypeExtendedMappingCollector {
		private final Class<E> javaClass;
		private final String entityName;

		Builder(Class<E> javaClass, String entityName) {
			this.javaClass = javaClass;
			this.entityName = entityName;
		}

		HibernateOrmContainedTypeContext<E> build() {
			return new HibernateOrmContainedTypeContext<>( this );
		}
	}
}
