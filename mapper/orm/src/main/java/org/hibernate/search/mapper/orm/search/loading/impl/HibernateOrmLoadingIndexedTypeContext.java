/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

public interface HibernateOrmLoadingIndexedTypeContext<E> {

	/**
	 * @return The entity type as a {@link Class}.
	 */
	Class<E> getJavaClass();

	EntityLoaderFactory getLoaderFactory();

}
