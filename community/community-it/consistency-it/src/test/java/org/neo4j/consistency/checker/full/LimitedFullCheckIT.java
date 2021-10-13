/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.consistency.checker.full;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import org.neo4j.common.EntityType;
import org.neo4j.configuration.Config;
import org.neo4j.consistency.RecordType;
import org.neo4j.consistency.checker.DebugContext;
import org.neo4j.consistency.checker.EntityBasedMemoryLimiter;
import org.neo4j.consistency.checking.full.ConsistencyCheckIncompleteException;
import org.neo4j.consistency.checking.full.ConsistencyFlags;
import org.neo4j.consistency.checking.full.FullCheck;
import org.neo4j.consistency.checking.full.FullCheckIntegrationTest;
import org.neo4j.consistency.report.ConsistencySummaryStatistics;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.internal.helpers.progress.ProgressMonitorFactory;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.io.pagecache.tracing.PageCacheTracer;
import org.neo4j.kernel.api.exceptions.index.IndexEntryConflictException;
import org.neo4j.kernel.api.index.IndexAccessor;
import org.neo4j.kernel.api.index.IndexUpdater;
import org.neo4j.kernel.impl.api.index.IndexUpdateMode;
import org.neo4j.storageengine.api.IndexEntryUpdate;

import static org.neo4j.consistency.ConsistencyCheckService.defaultConsistencyCheckThreadsNumber;
import static org.neo4j.consistency.checking.cache.CacheSlots.CACHE_LINE_SIZE_BYTES;
import static org.neo4j.graphdb.Label.label;
import static org.neo4j.graphdb.RelationshipType.withName;
import static org.neo4j.io.pagecache.context.CursorContext.NULL;
import static org.neo4j.memory.EmptyMemoryTracker.INSTANCE;

class LimitedFullCheckIT extends FullCheckIntegrationTest
{
    @Override
    protected EntityBasedMemoryLimiter.Factory memoryLimit()
    {
        // Make it so that it will have to do the checking in a couple of node id ranges
        return ( pageCacheMemory, highNodeId, highRelationshipId ) -> new EntityBasedMemoryLimiter( pageCacheMemory, 0,
                pageCacheMemory + highNodeId * CACHE_LINE_SIZE_BYTES / 3,
                CACHE_LINE_SIZE_BYTES, highNodeId, highRelationshipId, 1 );
    }

    @Test
    void shouldFindDuplicatesInUniqueIndexEvenWhenInDifferentRanges() throws ConsistencyCheckIncompleteException, IndexEntryConflictException, IOException
    {
        // given
        Iterator<IndexDescriptor> indexRuleIterator = getValueIndexDescriptors();

        // Create 2 extra nodes to guarantee that the node id of our duplicate is not in the same range as the original entry.
        createOneNode();
        createOneNode();
        // Create a node so the duplicate in the index refers to a valid node
        // (IndexChecker only reports the duplicate if it refers to a node id lower than highId)
        long nodeId = createOneNode();
        while ( indexRuleIterator.hasNext() )
        {
            IndexDescriptor indexRule = indexRuleIterator.next();
            if ( indexRule.schema().entityType() == EntityType.NODE )
            {
                // Don't close this accessor. It will be done when shutting down db.
                IndexAccessor accessor = fixture.indexAccessorLookup().apply( indexRule );

                try ( IndexUpdater updater = accessor.newUpdater( IndexUpdateMode.ONLINE, NULL ) )
                {
                    // There is already another node (created in generateInitialData()) that has this value
                    updater.process( IndexEntryUpdate.add( nodeId, indexRule, values( indexRule ) ) );
                }
                accessor.force( NULL );
            }
        }

        // when
        ConsistencySummaryStatistics stats = check();

        // then
        on( stats ).verify( RecordType.NODE, 2 ) // the duplicate in the 2 unique indexes
                .verify( RecordType.INDEX, 6 ) // the index entries pointing to node that should not be in index (3 BTREE and 3 RANGE)
                .andThatsAllFolks();
    }

    @ParameterizedTest
    @ValueSource( booleans = { true, false } )
    void shouldFindIndexInconsistenciesWhenHaveDifferentNumberRangesForEntityTypes( boolean moreNodesThanRelationships )
            throws ConsistencyCheckIncompleteException, IOException, IndexEntryConflictException
    {
        long highEntityId = Math.max( fixture.neoStores().getNodeStore().getHighId(), fixture.neoStores().getRelationshipStore().getHighId() );

        // Adds more indexed entities to get more ranges for the entity type we want to have the most of.
        // Then removes the last indexed entity to make sure we can find problems in the last range for both
        // nodes and relationships even when we have different number of ranges.
        Pair<Long,Long> lastAddedIds = addMoreIndexedEntries( highEntityId, moreNodesThanRelationships );
        removeFromIndex( lastAddedIds.first(), lastAddedIds.other() );

        // Allow 3 entities in each range
        final EntityBasedMemoryLimiter.Factory factory =
                ( pageCacheMemory, highNodeId, highRelationshipId ) -> new EntityBasedMemoryLimiter( pageCacheMemory, 0,
                        pageCacheMemory + CACHE_LINE_SIZE_BYTES * 3,
                        CACHE_LINE_SIZE_BYTES, highNodeId, highRelationshipId, 1 );
        ConsistencySummaryStatistics stats = check( factory );

        on( stats ).verify( RecordType.NODE, 4 ) // 4 node indexes with 1 entry removed
                .verify( RecordType.RELATIONSHIP, 4 ) // 4 relationship indexes with 1 entry removed
                .andThatsAllFolks();
    }

    private ConsistencySummaryStatistics check( EntityBasedMemoryLimiter.Factory specialMemoryLimiter ) throws ConsistencyCheckIncompleteException
    {
        FullCheck checker = new FullCheck( ProgressMonitorFactory.NONE, defaultConsistencyCheckThreadsNumber(), ConsistencyFlags.DEFAULT, Config.defaults(),
                        DebugContext.NO_DEBUG, specialMemoryLimiter );

        return checker.execute( fixture.getInstantiatedPageCache(), fixture.readOnlyDirectStoreAccess(), fixture.counts(), fixture.groupDegrees(),
                fixture.indexAccessorLookup(), PageCacheTracer.NULL, INSTANCE, logProvider.getLog( "test" ) );
    }

    private Pair<Long,Long> addMoreIndexedEntries( long highEntityId, boolean moreNodesThanRelationships )
    {
        AtomicLong lastAddedNode = new AtomicLong();
        AtomicLong lastAddedRel = new AtomicLong();
        fixture.apply( tx ->
        {
            long nbrEntities = highEntityId;
            Node node;
            Relationship relationship;
            do
            {
                node = tx.createNode( label( "label3" ) );
                node.setProperty( PROP1, VALUE1 );
                node.setProperty( PROP2, VALUE2 );
            }
            while ( moreNodesThanRelationships && nbrEntities-- > 0 );

            do
            {
                relationship = node.createRelationshipTo( node, withName( "C" ) );
                relationship.setProperty( PROP1, VALUE1 );
                relationship.setProperty( PROP2, VALUE2 );
            }
            while ( !moreNodesThanRelationships && nbrEntities-- > 0 );

            lastAddedNode.set( node.getId() );
            lastAddedRel.set( relationship.getId() );
        } );

        return Pair.of( lastAddedNode.get(), lastAddedRel.get() );
    }

    private void removeFromIndex( long nodeToRemoveFromIndex, long relToRemoveFromIndex ) throws IOException, IndexEntryConflictException
    {
        Iterator<IndexDescriptor> indexRuleIterator = getValueIndexDescriptors();
        while ( indexRuleIterator.hasNext() )
        {
            IndexDescriptor indexRule = indexRuleIterator.next();
            // Don't close this accessor. It will be done when shutting down db.
            IndexAccessor accessor = fixture.indexAccessorLookup().apply( indexRule );

            try ( IndexUpdater updater = accessor.newUpdater( IndexUpdateMode.ONLINE, NULL ) )
            {
                long idToRemove = relToRemoveFromIndex;
                if ( indexRule.schema().entityType() == EntityType.NODE )
                {
                    idToRemove = nodeToRemoveFromIndex;
                }
                updater.process( IndexEntryUpdate.remove( idToRemove, indexRule, values( indexRule ) ) );
            }
            accessor.force( NULL );
        }
    }
}
