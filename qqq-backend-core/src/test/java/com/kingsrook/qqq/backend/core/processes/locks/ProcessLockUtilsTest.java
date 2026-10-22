/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.backend.core.processes.locks;


import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for ProcessLockUtils 
 *******************************************************************************/
class ProcessLockUtilsTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance                   qInstance = QContext.getQInstance();
      MetaDataProducerMultiOutput metaData  = new ProcessLockMetaDataProducer().produce(qInstance);

      for(QTableMetaData table : metaData.getEach(QTableMetaData.class))
      {
         table.setBackendName(TestUtils.MEMORY_BACKEND_NAME);
      }

      metaData.addSelfToInstance(qInstance);
      new QInstanceValidator().revalidate(qInstance);

      new InsertAction().execute(new InsertInput(ProcessLockType.TABLE_NAME).withRecordEntities(List.of(
         new ProcessLockType()
            .withName("typeA")
            .withLabel("Type A"),
         new ProcessLockType()
            .withName("typeB")
            .withLabel("Type B")
            .withDefaultExpirationSeconds(1),
         new ProcessLockType()
            .withName("typeC")
            .withLabel("Type C")
            .withDefaultExpirationSeconds(10)
      )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      /////////////////////////////////////////
      // make sure that we can create a lock //
      /////////////////////////////////////////
      ProcessLock processLock = ProcessLockUtils.create("1", "typeA", "me");
      assertNotNull(processLock.getId());
      assertNotNull(processLock.getCheckInTimestamp());
      assertNull(processLock.getExpiresAtTimestamp());

      /////////////////////////////////////////////////////////
      // make sure we can't create a second for the same key //
      /////////////////////////////////////////////////////////
      assertThatThrownBy(() -> ProcessLockUtils.create("1", "typeA", "you"))
         .isInstanceOf(UnableToObtainProcessLockException.class)
         .hasMessageContaining("Held by: " + QContext.getQSession().getUser().getIdReference())
         .hasMessageContaining("with details: me")
         .hasMessageNotContaining("expiring at: 20")
         .matches(e -> ((UnableToObtainProcessLockException) e).getExistingLock() != null);

      /////////////////////////////////////////////////////////
      // make sure we can create another for a different key //
      /////////////////////////////////////////////////////////
      ProcessLockUtils.create("2", "typeA", "him");

      /////////////////////////////////////////////////////////////////////
      // make sure we can create another for a different type (same key) //
      /////////////////////////////////////////////////////////////////////
      ProcessLockUtils.create("1", "typeB", "her");

      //////////////////////////////
      // make sure we can release //
      //////////////////////////////
      ProcessLockUtils.release(processLock);

      //////////////////////
      // and then you can //
      //////////////////////
      processLock = ProcessLockUtils.create("1", "typeA", "you");
      assertNotNull(processLock.getId());
      assertEquals("you", processLock.getDetails());

      assertThatThrownBy(() -> ProcessLockUtils.create("1", "notAType", "you"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Unrecognized process lock type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSucceedWaitingForExpiration() throws QException
   {
      ProcessLock processLock = ProcessLockUtils.create("1", "typeB", "me");
      assertNotNull(processLock.getId());
      assertNotNull(processLock.getCheckInTimestamp());
      assertNotNull(processLock.getExpiresAtTimestamp());

      /////////////////////////////////////////////////////////////////////////
      // make sure someone else can, if they wait longer than the expiration //
      /////////////////////////////////////////////////////////////////////////
      processLock = ProcessLockUtils.create("1", "typeB", "you", Duration.of(1, ChronoUnit.SECONDS), Duration.of(3, ChronoUnit.SECONDS));
      assertNotNull(processLock.getId());
      assertThat(processLock.getDetails()).endsWith("you");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFailWaitingForExpiration() throws QException
   {
      ProcessLock processLock = ProcessLockUtils.create("1", "typeC", "me");
      assertNotNull(processLock.getId());
      assertNotNull(processLock.getCheckInTimestamp());
      assertNotNull(processLock.getExpiresAtTimestamp());

      //////////////////////////////////////////////////////////////////
      // make sure someone else fails, if they don't wait long enough //
      //////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> ProcessLockUtils.create("1", "typeC", "you", Duration.of(1, ChronoUnit.SECONDS), Duration.of(3, ChronoUnit.SECONDS)))
         .isInstanceOf(UnableToObtainProcessLockException.class)
         .hasMessageContaining("Held by: " + QContext.getQSession().getUser().getIdReference())
         .hasMessageContaining("with details: me")
         .hasMessageContaining("expiring at: 20")
         .matches(e -> ((UnableToObtainProcessLockException) e).getExistingLock() != null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCheckInUpdatesExpiration() throws QException
   {
      ProcessLock processLock = ProcessLockUtils.create("1", "typeB", "me");
      assertNotNull(processLock.getId());
      Instant originalCheckIn    = processLock.getCheckInTimestamp();
      Instant originalExpiration = processLock.getExpiresAtTimestamp();

      SleepUtils.sleep(5, TimeUnit.MILLISECONDS);
      ProcessLockUtils.checkIn(processLock);

      ProcessLock freshLock = ProcessLockUtils.getById(processLock.getId());
      assertNotNull(freshLock);
      assertNotEquals(originalCheckIn, freshLock.getCheckInTimestamp());
      assertNotEquals(originalExpiration, freshLock.getExpiresAtTimestamp());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReleaseById() throws QException
   {
      ////////////////////////////////////////////
      // assert no exceptions for these 2 cases //
      ////////////////////////////////////////////
      ProcessLockUtils.releaseById(null);
      ProcessLockUtils.releaseById(1);

      ProcessLock processLock = ProcessLockUtils.create("1", "typeA", "me");
      ProcessLockUtils.releaseById(processLock.getId());
      assertNull(ProcessLockUtils.getById(processLock.getId()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReleaseBadInputs()
   {
      //////////////////////////////////////////////////////////
      // make sure we don't blow up, just noop in these cases //
      //////////////////////////////////////////////////////////
      QCollectingLogger qCollectingLogger = QLogger.activateCollectingLoggerForClass(ProcessLockUtils.class);
      ProcessLockUtils.releaseById(null);
      ProcessLockUtils.release(null);
      ProcessLockUtils.releaseMany(null);
      ProcessLockUtils.releaseByIds(null);
      ProcessLockUtils.releaseMany(ListBuilder.of(null));
      ProcessLockUtils.releaseByIds(ListBuilder.of(null));
      QLogger.deactivateCollectingLoggerForClass(ProcessLockUtils.class);
      assertEquals(6, qCollectingLogger.getCollectedMessages().stream().filter(m -> m.getMessage().contains("noop")).count());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUserAndSessionNullness() throws QException
   {
      {
         QContext.getQSession().setUser(new QUser().withIdReference("me"));
         ProcessLock processLock = ProcessLockUtils.create("1", "typeA", null);
         assertNull(processLock.getDetails());
         assertEquals("me", processLock.getUserId());
         assertEquals(QContext.getQSession().getUuid(), processLock.getSessionUUID());
      }

      {
         ProcessLock processLock = ProcessLockUtils.create("2", "typeA", "foo");
         assertEquals("foo", processLock.getDetails());
         assertEquals("me", processLock.getUserId());
         assertEquals(QContext.getQSession().getUuid(), processLock.getSessionUUID());
      }

      {
         QContext.getQSession().setUser(null);
         ProcessLock processLock = ProcessLockUtils.create("3", "typeA", "bar");
         assertEquals("bar", processLock.getDetails());
         assertNull(processLock.getUserId());
         assertEquals(QContext.getQSession().getUuid(), processLock.getSessionUUID());
      }

      {
         QContext.getQSession().setUuid(null);
         ProcessLock processLock = ProcessLockUtils.create("4", "typeA", "baz");
         assertEquals("baz", processLock.getDetails());
         assertNull(processLock.getUserId());
         assertNull(processLock.getSessionUUID());
      }

      {
         QContext.getQSession().setUuid(null);
         ProcessLock processLock = ProcessLockUtils.create("5", "typeA", "");
         assertEquals("", processLock.getDetails());
         assertNull(processLock.getUserId());
         assertNull(processLock.getSessionUUID());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCheckInExpiresAtTimestampsWithNoDefault() throws QException
   {
      /////////////////////////////////////////
      // this type has no default expiration //
      /////////////////////////////////////////
      ProcessLock processLock = ProcessLockUtils.create("1", "typeA", null);
      assertNull(processLock.getExpiresAtTimestamp());

      /////////////////////////////////////////////////////////////
      // checkin w/o specifying an expires-time - leaves it null //
      /////////////////////////////////////////////////////////////
      ProcessLockUtils.checkIn(processLock);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertNull(processLock.getExpiresAtTimestamp());

      //////////////////////////////////////////////
      // checkin specifying null - leaves it null //
      //////////////////////////////////////////////
      ProcessLockUtils.checkIn(processLock, null);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertNull(processLock.getExpiresAtTimestamp());

      //////////////////////////////////////////////
      // checkin w/ a time - sets it to that time //
      //////////////////////////////////////////////
      Instant specifiedTime = Instant.now().plusSeconds(47);
      ProcessLockUtils.checkIn(processLock, specifiedTime);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertEquals(specifiedTime, processLock.getExpiresAtTimestamp());

      ///////////////////////////////////////////////////////////
      // checkin w/o specifying time - leaves it previous time //
      ///////////////////////////////////////////////////////////
      SleepUtils.sleep(1, TimeUnit.MILLISECONDS);
      ProcessLockUtils.checkIn(processLock);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertEquals(specifiedTime, processLock.getExpiresAtTimestamp());

      ////////////////////////////////////////////////////
      // checkin specifying null - puts it back to null //
      ////////////////////////////////////////////////////
      ProcessLockUtils.checkIn(processLock, null);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertNull(processLock.getExpiresAtTimestamp());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCheckInExpiresAtTimestampsWithSomeDefault() throws QException
   {
      /////////////////////////////////////////
      // this type has a default expiration //
      /////////////////////////////////////////
      ProcessLock processLock = ProcessLockUtils.create("1", "typeB", null);
      assertNotNull(processLock.getExpiresAtTimestamp());
      Instant expiresAtTimestamp = processLock.getExpiresAtTimestamp();

      ///////////////////////////////////////////////////////////////
      // checkin w/o specifying an expires-time - moves it forward //
      ///////////////////////////////////////////////////////////////
      SleepUtils.sleep(1, TimeUnit.MILLISECONDS);
      ProcessLockUtils.checkIn(processLock);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertNotNull(processLock.getExpiresAtTimestamp());
      assertNotEquals(expiresAtTimestamp, processLock.getExpiresAtTimestamp());

      ///////////////////////////////////////////////
      // checkin specifying null - sets it to null //
      ///////////////////////////////////////////////
      ProcessLockUtils.checkIn(processLock, null);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertNull(processLock.getExpiresAtTimestamp());

      //////////////////////////////////////////////
      // checkin w/ a time - sets it to that time //
      //////////////////////////////////////////////
      Instant specifiedTime = Instant.now();
      ProcessLockUtils.checkIn(processLock, specifiedTime);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertEquals(specifiedTime, processLock.getExpiresAtTimestamp());

      /////////////////////////////////////////////////////////////////////////
      // checkin w/o specifying time - uses the default and moves it forward //
      /////////////////////////////////////////////////////////////////////////
      SleepUtils.sleep(1, TimeUnit.MILLISECONDS);
      ProcessLockUtils.checkIn(processLock);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertNotEquals(specifiedTime, processLock.getExpiresAtTimestamp());

      ////////////////////////////////////////////////////
      // checkin specifying null - puts it back to null //
      ////////////////////////////////////////////////////
      ProcessLockUtils.checkIn(processLock, null);
      processLock = ProcessLockUtils.getById(processLock.getId());
      assertNull(processLock.getExpiresAtTimestamp());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMany() throws QException
   {
      /////////////////////////////////////////////////
      // make sure that we can create multiple locks //
      /////////////////////////////////////////////////
      List<String>                                         keys         = List.of("1", "2", "3");
      List<ProcessLock>                   processLocks = new ArrayList<>();
      Map<String, ProcessLockOrException> results      = ProcessLockUtils.createMany(keys, "typeA", "me");
      for(String key : keys)
      {
         ProcessLock processLock = results.get(key).processLock();
         assertNotNull(processLock.getId());
         assertNotNull(processLock.getCheckInTimestamp());
         assertNull(processLock.getExpiresAtTimestamp());
         processLocks.add(processLock);
      }

      /////////////////////////////////////////////////////////
      // make sure we can't create a second for the same key //
      /////////////////////////////////////////////////////////
      assertThatThrownBy(() -> ProcessLockUtils.create("1", "typeA", "you"))
         .isInstanceOf(UnableToObtainProcessLockException.class)
         .hasMessageContaining("Held by: " + QContext.getQSession().getUser().getIdReference())
         .hasMessageContaining("with details: me")
         .hasMessageNotContaining("expiring at: 20")
         .matches(e -> ((UnableToObtainProcessLockException) e).getExistingLock() != null);

      /////////////////////////////////////////////////////////
      // make sure we can create another for a different key //
      /////////////////////////////////////////////////////////
      ProcessLockUtils.create("4", "typeA", "him");

      /////////////////////////////////////////////////////////////////////
      // make sure we can create another for a different type (same key) //
      /////////////////////////////////////////////////////////////////////
      ProcessLockUtils.create("1", "typeB", "her");

      ////////////////////////////////////////////////////////////////////
      // now try to create some that will overlap, but one that'll work //
      ////////////////////////////////////////////////////////////////////
      keys = List.of("3", "4", "5");
      results = ProcessLockUtils.createMany(keys, "typeA", "me");
      for(String key : List.of("3", "4"))
      {
         UnableToObtainProcessLockException exception = results.get(key).unableToObtainProcessLockException();
         assertNotNull(exception);
      }

      ProcessLock processLock = results.get("5").processLock();
      assertNotNull(processLock.getId());
      assertNotNull(processLock.getCheckInTimestamp());
      assertNull(processLock.getExpiresAtTimestamp());
      processLocks.add(processLock);

      //////////////////////////////
      // make sure we can release //
      //////////////////////////////
      ProcessLockUtils.releaseMany(processLocks);

      ///////////////////////////////////////////////////////
      // make sure can re-lock 1 now after it was released //
      ///////////////////////////////////////////////////////
      processLock = ProcessLockUtils.create("1", "typeA", "you");
      assertNotNull(processLock.getId());
      assertEquals("you", processLock.getDetails());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testManyWithSleep() throws QException
   {
      /////////////////////////////////////////////////
      // make sure that we can create multiple locks //
      /////////////////////////////////////////////////
      List<String>                        keys     = List.of("1", "2", "3");
      Map<String, ProcessLockOrException> results0 = ProcessLockUtils.createMany(keys, "typeB", "me");
      for(String key : keys)
      {
         assertNotNull(results0.get(key).processLock());
      }

      ////////////////////////////////////////////////////////////
      // try again - and 2 and 3 should fail, if we don't sleep //
      ////////////////////////////////////////////////////////////
      keys = List.of("2", "3", "4");
      Map<String, ProcessLockOrException> results1 = ProcessLockUtils.createMany(keys, "typeB", "you");
      assertNull(results1.get("2").processLock());
      assertNull(results1.get("3").processLock());
      assertNotNull(results1.get("4").processLock());

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try another insert, which should initially succeed for #5, then sleep, and eventually succeed on 3 & 4 as well //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      keys = List.of("3", "4", "5");
      Map<String, ProcessLockOrException> results2 = ProcessLockUtils.createMany(keys, "typeB", "them", Duration.of(1, ChronoUnit.SECONDS), Duration.of(3, ChronoUnit.SECONDS));
      for(String key : keys)
      {
         assertNotNull(results2.get(key).processLock());
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure that we have a different ids for some that expired and then succeeded post-sleep //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      assertNotEquals(results0.get("3").processLock().getId(), results2.get("3").processLock().getId());
      assertNotEquals(results1.get("4").processLock().getId(), results2.get("4").processLock().getId());

   }

}