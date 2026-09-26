/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.esb.stats;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for EsbStats and EsbCounterSnapshot.
 *******************************************************************************/
class EsbStatsTest
{
   private static final String DESTINATION = "orderEvents";
   private static final String TRIGGER     = "syncOrder.orderEvents";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      EsbStats.getInstance().reset();
   }



   /*******************************************************************************
    ** getInstance is a singleton.
    *******************************************************************************/
   @Test
   void testSingleton()
   {
      assertThat(EsbStats.getInstance()).isSameAs(EsbStats.getInstance());
   }



   /*******************************************************************************
    ** Destination counters accumulate published and publish-failure counts, and
    ** keep the last error and the last activity time.
    *******************************************************************************/
   @Test
   void testDestinationCountersAccumulate()
   {
      EsbStats stats  = EsbStats.getInstance();
      Instant  before = Instant.now();

      stats.published(DESTINATION);
      stats.published(DESTINATION);
      stats.published(DESTINATION);
      stats.publishFailed(DESTINATION, new Exception("first failure"));
      stats.publishFailed(DESTINATION, new Exception("Connection refused"));

      EsbCounterSnapshot snapshot = stats.destination(DESTINATION);
      assertThat(snapshot.published()).isEqualTo(3L);
      assertThat(snapshot.publishFailures()).isEqualTo(2L);
      assertThat(snapshot.lastError()).isEqualTo("Connection refused");
      assertThat(snapshot.lastActivity()).isBetween(before, Instant.now());

      assertThat(snapshot.consumed()).isZero();
      assertThat(snapshot.succeeded()).isZero();
      assertThat(snapshot.failed()).isZero();
      assertThat(snapshot.retried()).isZero();
      assertThat(snapshot.deadLettered()).isZero();
      assertThat(snapshot.inFlight()).isZero();
      assertThat(snapshot.avgMs()).isZero();
      assertThat(snapshot.maxMs()).isZero();
   }



   /*******************************************************************************
    ** Trigger counters accumulate; avgMs and maxMs come from succeeded runs.
    *******************************************************************************/
   @Test
   void testTriggerCountersAccumulate()
   {
      EsbStats stats = EsbStats.getInstance();

      stats.consumed(TRIGGER);
      stats.consumed(TRIGGER);
      stats.consumed(TRIGGER);
      stats.consumed(TRIGGER);
      stats.inFlight(TRIGGER, 2);
      stats.succeeded(TRIGGER, 10);
      stats.succeeded(TRIGGER, 30);
      stats.inFlight(TRIGGER, -1);
      stats.failed(TRIGGER, new IllegalStateException("boom"));
      stats.retried(TRIGGER);
      stats.failed(TRIGGER, new IllegalStateException("boom again"));
      stats.deadLettered(TRIGGER);

      EsbCounterSnapshot snapshot = stats.trigger(TRIGGER);
      assertThat(snapshot.consumed()).isEqualTo(4L);
      assertThat(snapshot.succeeded()).isEqualTo(2L);
      assertThat(snapshot.failed()).isEqualTo(2L);
      assertThat(snapshot.retried()).isEqualTo(1L);
      assertThat(snapshot.deadLettered()).isEqualTo(1L);
      assertThat(snapshot.inFlight()).isEqualTo(1);
      assertThat(snapshot.avgMs()).isEqualTo(20L);
      assertThat(snapshot.maxMs()).isEqualTo(30L);
      assertThat(snapshot.lastError()).isEqualTo("boom again");
      assertThat(snapshot.lastActivity()).isNotNull();

      assertThat(snapshot.published()).isZero();
      assertThat(snapshot.publishFailures()).isZero();
   }



   /*******************************************************************************
    ** avgMs is rounded to the nearest millisecond; maxMs is the largest run.
    *******************************************************************************/
   @Test
   void testAvgAndMaxMs()
   {
      EsbStats stats = EsbStats.getInstance();

      stats.succeeded(TRIGGER, 1);
      stats.succeeded(TRIGGER, 2);
      assertThat(stats.trigger(TRIGGER).avgMs()).isEqualTo(2L);
      assertThat(stats.trigger(TRIGGER).maxMs()).isEqualTo(2L);

      stats.succeeded(TRIGGER, 100);
      stats.succeeded(TRIGGER, 5);
      assertThat(stats.trigger(TRIGGER).avgMs()).isEqualTo(27L);
      assertThat(stats.trigger(TRIGGER).maxMs()).isEqualTo(100L);
   }



   /*******************************************************************************
    ** A snapshot doesn't change after later counts.
    *******************************************************************************/
   @Test
   void testSnapshotIsFixed()
   {
      EsbStats stats = EsbStats.getInstance();
      stats.published(DESTINATION);

      EsbCounterSnapshot snapshot = stats.destination(DESTINATION);
      stats.published(DESTINATION);
      stats.publishFailed(DESTINATION, new Exception("later"));

      assertThat(snapshot.published()).isEqualTo(1L);
      assertThat(snapshot.publishFailures()).isZero();
      assertThat(snapshot.lastError()).isNull();
      assertThat(stats.destination(DESTINATION).published()).isEqualTo(2L);
   }



   /*******************************************************************************
    ** A name with no counts gets an all-zero snapshot, with null last activity
    ** and last error.
    *******************************************************************************/
   @Test
   void testUnknownNameIsZero()
   {
      for(EsbCounterSnapshot snapshot : List.of(EsbStats.getInstance().destination("nope"), EsbStats.getInstance().trigger("nope"), EsbStats.getInstance().trigger(null)))
      {
         assertThat(snapshot).isEqualTo(EsbCounterSnapshot.EMPTY);
         assertThat(snapshot.published()).isZero();
         assertThat(snapshot.inFlight()).isZero();
         assertThat(snapshot.avgMs()).isZero();
         assertThat(snapshot.lastActivity()).isNull();
         assertThat(snapshot.lastError()).isNull();
      }
   }



   /*******************************************************************************
    ** Destinations and triggers are counted separately, even with the same name.
    *******************************************************************************/
   @Test
   void testDestinationsAndTriggersAreSeparate()
   {
      EsbStats stats = EsbStats.getInstance();
      stats.published("sameName");
      stats.consumed("sameName");

      assertThat(stats.destination("sameName").published()).isEqualTo(1L);
      assertThat(stats.destination("sameName").consumed()).isZero();
      assertThat(stats.trigger("sameName").consumed()).isEqualTo(1L);
      assertThat(stats.trigger("sameName").published()).isZero();
   }



   /*******************************************************************************
    ** An exception without a message is recorded by its class name; a null
    ** exception or a null name is ignored rather than thrown.
    *******************************************************************************/
   @Test
   void testErrorTextAndNulls()
   {
      EsbStats stats = EsbStats.getInstance();

      stats.failed(TRIGGER, new NullPointerException());
      assertThat(stats.trigger(TRIGGER).lastError()).isEqualTo("NullPointerException");

      stats.failed(TRIGGER, null);
      assertThat(stats.trigger(TRIGGER).failed()).isEqualTo(2L);
      assertThat(stats.trigger(TRIGGER).lastError()).isEqualTo("NullPointerException");

      stats.published(null);
      stats.publishFailed(null, new Exception("x"));
      stats.consumed(null);
      stats.succeeded(null, 1);
      stats.failed(null, new Exception("x"));
      stats.retried(null);
      stats.deadLettered(null);
      stats.inFlight(null, 1);
      assertThat(stats.destination(null)).isEqualTo(EsbCounterSnapshot.EMPTY);
   }



   /*******************************************************************************
    ** reset clears all counters.
    *******************************************************************************/
   @Test
   void testReset()
   {
      EsbStats stats = EsbStats.getInstance();
      stats.published(DESTINATION);
      stats.consumed(TRIGGER);

      stats.reset();
      assertThat(stats.destination(DESTINATION)).isEqualTo(EsbCounterSnapshot.EMPTY);
      assertThat(stats.trigger(TRIGGER)).isEqualTo(EsbCounterSnapshot.EMPTY);
   }



   /*******************************************************************************
    ** Counts from many threads at once all land.
    *******************************************************************************/
   @Test
   void testConcurrentCounts() throws Exception
   {
      EsbStats       stats          = EsbStats.getInstance();
      int            threadCount    = 8;
      int            countPerThread = 1000;
      CountDownLatch startLatch     = new CountDownLatch(1);
      List<Thread>   threads        = new ArrayList<>();

      for(int t = 0; t < threadCount; t++)
      {
         threads.add(Thread.ofPlatform().start(() ->
         {
            try
            {
               startLatch.await();
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               return;
            }

            for(int i = 0; i < countPerThread; i++)
            {
               stats.published(DESTINATION);
               stats.inFlight(TRIGGER, 1);
               stats.succeeded(TRIGGER, i);
               stats.inFlight(TRIGGER, -1);
            }
         }));
      }

      startLatch.countDown();
      for(Thread thread : threads)
      {
         thread.join();
      }

      assertThat(stats.destination(DESTINATION).published()).isEqualTo((long) threadCount * countPerThread);
      assertThat(stats.trigger(TRIGGER).succeeded()).isEqualTo((long) threadCount * countPerThread);
      assertThat(stats.trigger(TRIGGER).inFlight()).isZero();
      assertThat(stats.trigger(TRIGGER).maxMs()).isEqualTo(countPerThread - 1L);
      assertThat(stats.trigger(TRIGGER).avgMs()).isEqualTo(500L);
   }

}
