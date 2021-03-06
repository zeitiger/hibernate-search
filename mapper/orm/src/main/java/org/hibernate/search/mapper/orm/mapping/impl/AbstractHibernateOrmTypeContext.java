/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import org.hibernate.search.mapper.orm.event.impl.HibernateOrmListenerTypeContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeTypeContext;

abstract class AbstractHibernateOrmTypeContext<E>
		implements HibernateOrmScopeTypeContext<E>, HibernateOrmListenerTypeContext {
	private final Class<E> javaClass;
	private final String entityName;

	AbstractHibernateOrmTypeContext(Class<E> javaClass, String entityName) {
		this.javaClass = javaClass;
		this.entityName = entityName;
	}

	@Override
	public Class<E> getJavaClass() {
		return javaClass;
	}

	public String getEntityName() {
		return entityName;
	}
}
