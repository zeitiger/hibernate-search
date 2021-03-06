/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.search.loading.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.metamodel.Type;

import org.hibernate.AssertionFailure;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.search.loading.EntityLoadingCacheLookupStrategy;

public class HibernateOrmByIdEntityLoader<E> implements HibernateOrmComposableEntityLoader<E> {

	public static <E> EntityLoaderFactory factory(SessionFactoryImplementor sessionFactory,
			Class<E> entityType) {
		return new Factory( toRootEntityClass( sessionFactory, entityType ) );
	}

	private final Session session;
	private final Class<E> entityType;
	private final EntityLoadingCacheLookupStrategyImplementor<E> cacheLookupStrategyImplementor;
	private final MutableEntityLoadingOptions loadingOptions;

	private HibernateOrmByIdEntityLoader(
			Class<E> entityType,
			Session session,
			EntityLoadingCacheLookupStrategyImplementor<E> cacheLookupStrategyImplementor,
			MutableEntityLoadingOptions loadingOptions) {
		this.entityType = entityType;
		this.session = session;
		this.cacheLookupStrategyImplementor = cacheLookupStrategyImplementor;
		this.loadingOptions = loadingOptions;
	}

	@Override
	public List<E> loadBlocking(List<EntityReference> references) {
		if ( cacheLookupStrategyImplementor == null ) {
			// Optimization: if we don't need to look up the cache, we don't need a map to store intermediary results.
			return loadEntities( references );
		}
		else {
			return HibernateOrmComposableEntityLoader.super.loadBlocking( references );
		}
	}

	@Override
	public void loadBlocking(List<EntityReference> references, Map<? super EntityReference, ? super E> entitiesByReference) {
		List<EntityReference> missingFromCacheReferences = loadBlockingFromCache( references, entitiesByReference );
		if ( missingFromCacheReferences.isEmpty() ) {
			return;
		}

		List<? extends E> loadedEntities = loadEntities( missingFromCacheReferences );
		Iterator<EntityReference> referencesIterator = missingFromCacheReferences.iterator();
		Iterator<? extends E> loadedEntityIterator = loadedEntities.iterator();
		while ( referencesIterator.hasNext() ) {
			EntityReference reference = referencesIterator.next();
			E loadedEntity = loadedEntityIterator.next();
			if ( loadedEntity != null ) {
				entitiesByReference.put( reference, loadedEntity );
			}
		}
	}

	/**
	 * @param references The references to entities to load from the cache.
	 * @param entitiesByReference The map where loaded entities should be put.
	 * @return The references that could not be loaded from the cache.
	 */
	private List<EntityReference> loadBlockingFromCache(List<EntityReference> references,
			Map<? super EntityReference,? super E> entitiesByReference) {
		if ( cacheLookupStrategyImplementor == null ) {
			return references;
		}

		List<EntityReference> missingFromCacheReferences = new ArrayList<>( references.size() );

		for ( EntityReference reference : references ) {
			Object entityId = reference.getId();
			E loadedEntity = cacheLookupStrategyImplementor.lookup( entityId );
			if ( loadedEntity == null ) {
				missingFromCacheReferences.add( reference );
			}
			else if ( hasExpectedType( reference, loadedEntity ) ) {
				entitiesByReference.put( reference, loadedEntity );
			}
			else {
				// The index is out of sync and the referenced entity does not exist anymore.
				// Assume the entity we were attempting to load was deleted, and mark it as such.
				entitiesByReference.put( reference, null );
			}
		}

		return missingFromCacheReferences;
	}

	private List<E> loadEntities(List<EntityReference> references) {
		List<Serializable> ids = new ArrayList<>( references.size() );
		for ( EntityReference reference : references ) {
			ids.add( (Serializable) reference.getId() );
		}

		List<E> loadedEntities = getMultiAccess().multiLoad( ids );

		for ( int i = 0; i < references.size(); i++ ) {
			EntityReference reference = references.get( i );
			E loadedEntity = loadedEntities.get( i );
			if ( !hasExpectedType( reference, loadedEntity ) ) {
				// The index is out of sync and the referenced entity does not exist anymore.
				// Assume the entity we were attempting to load was deleted and mark it as such.
				loadedEntities.set( i, null );
			}
		}

		return loadedEntities;
	}

	private MultiIdentifierLoadAccess<E> getMultiAccess() {
		MultiIdentifierLoadAccess<E> multiAccess = session.byMultipleIds( entityType );

		multiAccess.withBatchSize( loadingOptions.getFetchSize() );

		return multiAccess;
	}

	/*
	 * Under some circumstances, the multi-access or the cache lookups may return entities that extend E,
	 * but not the type expected by users.
	 *
	 * For example, let's consider entity types A, B, C, D, with B, C, and D extending A
	 * Let's imagine an instance of type B and with id 4 is deleted from the database
	 * and replaced with an instance of type D and id 4.
	 * If a search on entity types B and C is performed before the index is refreshed,
	 * we might be requested to load entity B with id 4,
	 * and since we're working with the common supertype A,
	 * loading will succeed but will yield an entity of type D with id 4.
	 *
	 * Now, the entity will still be an instance of A, but... the user doesn't care about A:
	 * the user asked for a search on entities B and C.
	 * Returning D might be a problem, especially if the user intends to call methods defined on an interface I,
	 * implemented by B and C, but not D.
	 * This will be a problem since that entity does not implement I.
	 *
	 * The easiest way to avoid this problem is to just check the type of every loaded entity,
	 * to be sure it's the same type that was originally requested.
	 * Then we will be safe, because callers are expected to only pass entity references
	 * to types that were originally targeted by the search,
	 * and these types are known to implement any interface that the user could possibly rely on.
	 */
	private static boolean hasExpectedType(EntityReference reference, Object loadedEntity) {
		return reference.getType().isInstance( loadedEntity );
	}

	private static Class<?> toRootEntityClass(SessionFactoryImplementor sessionFactory, Class<?> entityClass) {
		/*
		 * We need to rely on Hibernate ORM's SPIs: this is complex stuff.
		 * For example there may be class hierarchies such as A > B > C
		 * where A and C are entity types and B is a mapped superclass.
		 * So we need to exclude non-entity types, and for that we need the Hibernate ORM metamodel.
		 */
		MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		String rootEntityName = metamodel.entityPersister( entityClass ).getRootEntityName();
		return metamodel.entityPersister( rootEntityName ).getMappedClass();
	}

	private static class Factory implements EntityLoaderFactory {

		private final Class<?> rootEntityType;

		private Factory(Class<?> rootEntityType) {
			this.rootEntityType = rootEntityType;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == null || !( getClass().equals( obj.getClass() ) ) ) {
				return false;
			}
			Factory other = (Factory) obj;
			// If the root entity type is different,
			// the factories work in separate ID spaces and should be used separately.
			return rootEntityType.equals( other.rootEntityType );
		}

		@Override
		public int hashCode() {
			return rootEntityType.hashCode();
		}

		@Override
		public <E> HibernateOrmComposableEntityLoader<E> create(Class<E> targetEntityType,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
			if ( !rootEntityType.isAssignableFrom( targetEntityType ) ) {
				throw new AssertionFailure(
						"The targeted entity type is not a subclass of the expected root entity type."
								+ " There is a bug in Hibernate Search, please report it."
								+ " Expected root entity type: " + rootEntityType
								+ " Targeted entity type: " + targetEntityType
				);
			}

			return doCreate( targetEntityType, session, cacheLookupStrategy,loadingOptions );
		}

		@Override
		public <E2> HibernateOrmComposableEntityLoader<? extends E2> create(List<Class<? extends E2>> targetEntityTypes,
				SessionImplementor session, EntityLoadingCacheLookupStrategy cacheLookupStrategy,
				MutableEntityLoadingOptions loadingOptions) {
			Class<?> commonSuperClass = toMostSpecificCommonEntitySuperClass( session.getSessionFactory(), targetEntityTypes );
			if ( !rootEntityType.isAssignableFrom( commonSuperClass ) ) {
				throw new AssertionFailure(
						"Some types among the targeted entity types are not subclasses of the expected root entity type."
								+ " There is a bug in Hibernate Search, please report it."
								+ " Expected root entity type: " + rootEntityType
								+ " Targeted entity types: " + targetEntityTypes
				);
			}

			/*
			 * Theoretically, this cast is unsafe,
			 * since the loader could return entities of any type extending "commonSuperClass",
			 * which is either E2 or a common supertype of some child types of E2.
			 *
			 * However, we perform some runtime checks that make this cast safe.
			 *
			 * See hasExpectedType() and its callers for more information.
			 */
			@SuppressWarnings("unchecked")
			HibernateOrmComposableEntityLoader<E2> result = (HibernateOrmComposableEntityLoader<E2>) doCreate(
					commonSuperClass, session, cacheLookupStrategy, loadingOptions
			);

			return result;
		}

		private <E> HibernateOrmComposableEntityLoader<E> doCreate(Class<E> targetEntityType,
				SessionImplementor session,
				EntityLoadingCacheLookupStrategy cacheLookupStrategy, MutableEntityLoadingOptions loadingOptions) {
			EntityLoadingCacheLookupStrategyImplementor<E> cacheLookupStrategyImplementor;

			/*
			 * Ideally, in order to comply with the cache lookup strategy,
			 * we would use multiAccess setters such as
			 * with(CacheMode) and enableSessionCheck(boolean),
			 * and let Hibernate ORM do it for us.
			 *
			 * However, with(CacheMode) has a side-effect: it can also affect how entities are put into the cache.
			 * Since the cache lookup strategy has nothing to do with that,
			 * we go the safer route and wrap the loader with other loaders that
			 * will perform PC and 2LC checking prior to using the multiAccess.
			 */
			switch ( cacheLookupStrategy ) {
				case SKIP:
					cacheLookupStrategyImplementor = null;
					break;
				case PERSISTENCE_CONTEXT:
					cacheLookupStrategyImplementor =
							PersistenceContextLookupStrategy.create( targetEntityType, session );
					break;
				case PERSISTENCE_CONTEXT_THEN_SECOND_LEVEL_CACHE:
					cacheLookupStrategyImplementor =
							PersistenceContextThenSecondLevelCacheLookupStrategy.create( targetEntityType, session );
					break;
				default:
					throw new AssertionFailure( "Unexpected cache lookup strategy: " + cacheLookupStrategy );
			}

			return new HibernateOrmByIdEntityLoader<>(
					targetEntityType, session, cacheLookupStrategyImplementor, loadingOptions
			);
		}

		private static Class<?> toMostSpecificCommonEntitySuperClass(SessionFactory sessionFactory,
				Iterable<? extends Class<?>> targetedClasses) {
			Metamodel metamodel = sessionFactory.getMetamodel();
			IdentifiableType<?> result = null;
			for ( Class<?> targetedClass : targetedClasses ) {
				IdentifiableType<?> type = metamodel.entity( targetedClass );
				if ( result == null ) {
					result = type;
				}
				else {
					result = toMostSpecificCommonEntitySuperType( result, type );
				}
			}
			return result.getJavaType();
		}

		private static IdentifiableType<?> toMostSpecificCommonEntitySuperType(
				IdentifiableType<?> type1, IdentifiableType<?> type2) {
			/*
			 * We need to rely on Hibernate ORM's SPIs: this is complex stuff.
			 * For example there may be class hierarchies such as A > B > C
			 * where A and C are entity types and B is a mapped superclass.
			 * So even if we know the two types have a common superclass,
			 * we need to skip non-entity superclasses, and for that we need the Hibernate ORM metamodel.
			 */
			IdentifiableType<?> superType = type1;
			while (
					!superType.getJavaType().isAssignableFrom( type2.getJavaType() )
					// Be sure to ignore mapped superclass hiding in the middle of the entity hierarchy
					|| !Type.PersistenceType.ENTITY.equals( superType.getPersistenceType() )
			) {
				superType = superType.getSupertype();
			}
			return superType;
		}
	}
}
