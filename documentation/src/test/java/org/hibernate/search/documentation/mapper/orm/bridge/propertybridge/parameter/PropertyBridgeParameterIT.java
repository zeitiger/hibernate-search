/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.mapper.orm.bridge.propertybridge.parameter;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import javax.persistence.EntityManagerFactory;

import org.hibernate.search.documentation.testsupport.BackendConfigurations;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.automaticindexing.AutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PropertyBridgeParameterIT {
	@Parameterized.Parameters(name = "{0}")
	public static Object[] backendConfigurations() {
		return BackendConfigurations.simple().toArray();
	}

	@Rule
	public OrmSetupHelper setupHelper;

	private EntityManagerFactory entityManagerFactory;

	public PropertyBridgeParameterIT(BackendConfiguration backendConfiguration) {
		this.setupHelper = OrmSetupHelper.withSingleBackend( backendConfiguration );
	}

	@Before
	public void setup() {
		entityManagerFactory = setupHelper.start()
				.withProperty(
						HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
						AutomaticIndexingSynchronizationStrategyName.SEARCHABLE
				)
				.setup( Invoice.class );
	}

	@Test
	public void smoke() {
		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			Invoice invoice = new Invoice();
			invoice.getLineItems()
					.add( new InvoiceLineItem( InvoiceLineItemCategory.BOOK, new BigDecimal( "5.99" ) ) );
			invoice.getLineItems()
					.add( new InvoiceLineItem( InvoiceLineItemCategory.BOOK, new BigDecimal( "8.99" ) ) );
			invoice.getLineItems()
					.add( new InvoiceLineItem( InvoiceLineItemCategory.BOOK, new BigDecimal( "15.99" ) ) );
			invoice.getLineItems()
					.add( new InvoiceLineItem( InvoiceLineItemCategory.SHIPPING, new BigDecimal( "7.99" ) ) );
			entityManager.persist( invoice );
		} );

		OrmUtils.withinJPATransaction( entityManagerFactory, entityManager -> {
			SearchSession searchSession = Search.session( entityManager );

			List<Invoice> result = searchSession.search( Invoice.class )
					.predicate( f -> f.bool()
							.must( f.range().field( "itemSummary.total" )
									.atLeast( new BigDecimal( "20.0" ) ) )
							.must( f.range().field( "itemSummary.shipping" )
									.atMost( new BigDecimal( "10.0" ) ) )
					)
					.fetchHits( 20 );

			assertThat( result ).hasSize( 1 );
		} );
	}

}
