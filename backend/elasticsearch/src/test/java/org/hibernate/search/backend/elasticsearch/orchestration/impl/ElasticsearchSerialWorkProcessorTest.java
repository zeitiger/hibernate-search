/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkAggregator;

import org.junit.Before;
import org.junit.Test;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IAnswer;


public class ElasticsearchSerialWorkProcessorTest extends EasyMockSupport {

	/**
	 * @return A value that should not matter, because it should not be used.
	 */
	private static <T> T unusedReturnValue() {
		return null;
	}

	private ElasticsearchWorkSequenceBuilder sequenceBuilderMock;
	private ElasticsearchWorkBulker bulkerMock;

	@Before
	public void initMocks() {
		sequenceBuilderMock = createStrictMock( ElasticsearchWorkSequenceBuilder.class );
		bulkerMock = createStrictMock( ElasticsearchWorkBulker.class );
	}

	@Test
	public void simple_singleWorkInWorkSet() {
		ElasticsearchWork<Object> work = work( 1 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		CompletableFuture<Object> workFuture = new CompletableFuture<>();
		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work ) ).andReturn( workFuture );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		replayAll();
		processor.beforeWorkSet();
		CompletableFuture<Object> returnedWork2Future = processor.submit( work );
		processor.afterWorkSet();
		verifyAll();
		assertThat( returnedWork2Future ).isSameAs( workFuture );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();
		sequenceFuture.complete( null );
		assertThat( futureAll ).isSuccessful( (Void) null );
	}

	@Test
	public void simple_multipleWorksInWorkSet() {
		ElasticsearchWork<Object> work1 = work( 1 );
		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( unusedReturnValue() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		processor.submit( work2 );
		CompletableFuture<Void> returnedSequenceFuture = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequenceFuture ).isSameAs( sequenceFuture );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		sequenceFuture.complete( null );
		replayAll();
		assertThat( futureAll ).isSuccessful();
	}

	@Test
	public void simple_sequenceFailure() {
		ElasticsearchWork<Object> work = work( 1 );

		CompletableFuture<Void> sequenceFuture = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		CompletableFuture<Object> workFuture = new CompletableFuture<>();
		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work ) ).andReturn( workFuture );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequenceFuture );
		replayAll();
		processor.beforeWorkSet();
		CompletableFuture<Object> returnedWork2Future = processor.submit( work );
		processor.afterWorkSet();
		verifyAll();
		assertThat( returnedWork2Future ).isSameAs( workFuture );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		replayAll();
		sequenceFuture.completeExceptionally( new RuntimeException() );
		verifyAll();
		// Failures in a sequence should be ignored
		assertThat( futureAll ).isSuccessful( (Void) null );
	}

	@Test
	public void newSequenceBetweenWorkset() {
		ElasticsearchWork<Object> work1 = work( 1 );

		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.addNonBulkExecution( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		CompletableFuture<Void> returnedSequence1Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( EasyMock.anyObject() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work2 );
		CompletableFuture<Void> returnedSequence2Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		sequence2Future.complete( null );
		replayAll();
		assertThat( futureAll ).isSuccessful();
	}

	@Test
	public void reuseBulkAcrossSequences() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );

		BulkableElasticsearchWork<Object> work2 = bulkableWork( 2 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		CompletableFuture<Void> returnedSequence1Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( EasyMock.anyObject() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.add( work2 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work2 );
		CompletableFuture<Void> returnedSequence2Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		sequence2Future.complete( null );
		replayAll();
		assertThat( futureAll ).isSuccessful();
	}

	@Test
	public void newBulkIfNonBulkable_sameWorkset() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		ElasticsearchWork<Object> work2 = work( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.addNonBulkExecution( work2 ) ).andReturn( unusedReturnValue() );
		expect( work3.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work3 ) );
		bulkerMock.finalizeBulkWork();
		expect( bulkerMock.add( work3 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		processor.submit( work2 );
		processor.submit( work3 );
		CompletableFuture<Void> returnedSequence1Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		sequence1Future.complete( null );
		replayAll();
		assertThat( futureAll ).isSuccessful();
	}

	@Test
	public void newBulkIfNonBulkable_differentWorksets() {
		BulkableElasticsearchWork<Object> work1 = bulkableWork( 1 );
		ElasticsearchWork<Object> work2 = work( 2 );
		BulkableElasticsearchWork<Object> work3 = bulkableWork( 3 );

		CompletableFuture<Void> sequence1Future = new CompletableFuture<>();
		CompletableFuture<Void> sequence2Future = new CompletableFuture<>();

		replayAll();
		ElasticsearchSerialWorkProcessor processor =
				new ElasticsearchSerialWorkProcessor( sequenceBuilderMock, bulkerMock );
		verifyAll();

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work1.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work1 ) );
		expect( bulkerMock.add( work1 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.build() ).andReturn( sequence1Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work1 );
		CompletableFuture<Void> returnedSequence1Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence1Future ).isSameAs( sequence1Future );

		resetAll();
		sequenceBuilderMock.init( anyObject() );
		expect( work2.aggregate( anyObject() ) ).andAnswer( nonBulkableAggregateAnswer( work2 ) );
		expect( bulkerMock.addWorksToSequence() ).andReturn( true );
		expect( sequenceBuilderMock.addNonBulkExecution( work2 ) ).andReturn( unusedReturnValue() );
		expect( work3.aggregate( anyObject() ) ).andAnswer( bulkableAggregateAnswer( work3 ) );
		bulkerMock.finalizeBulkWork();
		expect( bulkerMock.add( work3 ) ).andReturn( unusedReturnValue() );
		expect( bulkerMock.addWorksToSequence() ).andReturn( false );
		expect( sequenceBuilderMock.build() ).andReturn( sequence2Future );
		replayAll();
		processor.beforeWorkSet();
		processor.submit( work2 );
		processor.submit( work3 );
		CompletableFuture<Void> returnedSequence2Future = processor.afterWorkSet();
		verifyAll();
		assertThat( returnedSequence2Future ).isSameAs( sequence2Future );

		resetAll();
		bulkerMock.finalizeBulkWork();
		replayAll();
		CompletableFuture<Void> futureAll = processor.endBatch();
		verifyAll();
		assertThat( futureAll ).isPending();

		resetAll();
		sequence2Future.complete( null );
		replayAll();
		assertThat( futureAll ).isSuccessful();
	}

	private <T> ElasticsearchWork<T> work(int index) {
		return createStrictMock( "work" + index, ElasticsearchWork.class );
	}

	private <T> BulkableElasticsearchWork<T> bulkableWork(int index) {
		return createStrictMock( "bulkableWork" + index, BulkableElasticsearchWork.class );
	}

	private <T> IAnswer<CompletableFuture<T>> nonBulkableAggregateAnswer(ElasticsearchWork<T> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			return aggregator.addNonBulkable( mock );
		};
	}

	private <T> IAnswer<CompletableFuture<T>> bulkableAggregateAnswer(BulkableElasticsearchWork<T> mock) {
		return () -> {
			ElasticsearchWorkAggregator aggregator = (ElasticsearchWorkAggregator) getCurrentArguments()[0];
			return aggregator.addBulkable( mock );
		};
	}
}
