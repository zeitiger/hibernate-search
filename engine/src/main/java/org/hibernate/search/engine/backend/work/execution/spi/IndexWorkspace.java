/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution.spi;

import java.util.concurrent.CompletableFuture;

/**
 * The entry point for explicit index operations on a single index.
 */
public interface IndexWorkspace {

	CompletableFuture<?> optimize();

	CompletableFuture<?> purge();

	CompletableFuture<?> flush();

}
