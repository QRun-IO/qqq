/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.state;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for InMemoryStateProvider
 *******************************************************************************/
public class InMemoryStateProviderTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testStateNotFound()
   {
      InMemoryStateProvider stateProvider = InMemoryStateProvider.getInstance();
      UUIDAndTypeStateKey   key           = new UUIDAndTypeStateKey(StateType.PROCESS_STATUS);

      Assertions.assertTrue(stateProvider.get(QRecord.class, key).isEmpty(), "Key not found in state should return empty");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSimpleStateFound()
   {
      InMemoryStateProvider stateProvider = InMemoryStateProvider.getInstance();
      UUIDAndTypeStateKey   key           = new UUIDAndTypeStateKey(StateType.PROCESS_STATUS);

      String  uuid    = UUID.randomUUID().toString();
      QRecord qRecord = new QRecord().withValue("uuid", uuid);
      stateProvider.put(key, qRecord);

      QRecord qRecordFromState = stateProvider.get(QRecord.class, key).get();
      Assertions.assertEquals(uuid, qRecordFromState.getValueString("uuid"), "Should read value from state persistence");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testWrongTypeOnGet()
   {
      InMemoryStateProvider stateProvider = InMemoryStateProvider.getInstance();
      UUIDAndTypeStateKey   key           = new UUIDAndTypeStateKey(StateType.PROCESS_STATUS);

      String  uuid    = UUID.randomUUID().toString();
      QRecord qRecord = new QRecord().withValue("uuid", uuid);
      stateProvider.put(key, qRecord);

      Assertions.assertThrows(Exception.class, () ->
      {
         stateProvider.get(QTableMetaData.class, key);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testClean()
   {
      InMemoryStateProvider stateProvider = InMemoryStateProvider.getInstance();

      /////////////////////////////////////////////////////////////
      // Add an entry that is 3 hours old, should not be cleaned //
      /////////////////////////////////////////////////////////////
      UUIDAndTypeStateKey newKey     = new UUIDAndTypeStateKey(UUID.randomUUID(), StateType.PROCESS_STATUS, Instant.now().minus(3, ChronoUnit.HOURS));
      String              newUUID    = UUID.randomUUID().toString();
      QRecord             newQRecord = new QRecord().withValue("uuid", newUUID);
      stateProvider.put(newKey, newQRecord);

      ////////////////////////////////////////////////////////////
      // Add an entry that is 5 hours old, it should be cleaned //
      ////////////////////////////////////////////////////////////
      UUIDAndTypeStateKey oldKey     = new UUIDAndTypeStateKey(UUID.randomUUID(), StateType.PROCESS_STATUS, Instant.now().minus(5, ChronoUnit.HOURS));
      String              oldUUID    = UUID.randomUUID().toString();
      QRecord             oldQRecord = new QRecord().withValue("uuid", oldUUID);
      stateProvider.put(oldKey, oldQRecord);

      ///////////////////
      // Call to clean //
      ///////////////////
      stateProvider.clean(Instant.now().minus(4, ChronoUnit.HOURS));

      QRecord qRecordFromState = stateProvider.get(QRecord.class, newKey).get();
      Assertions.assertEquals(newUUID, qRecordFromState.getValueString("uuid"), "Should read value from state persistence");

      Assertions.assertTrue(stateProvider.get(QRecord.class, oldKey).isEmpty(), "Key not found in state should return empty");
   }



   /*******************************************************************************
    ** originally written with N=100000, but showed the error as small as 1000.
    *******************************************************************************/
   @Test
   void testDemonstrateConcurrentModificationIfNonSynchronizedMap()
   {
      int N = 1000;
      InMemoryStateProvider stateProvider = InMemoryStateProvider.getInstance();

      ExecutorService executorService = Executors.newFixedThreadPool(10);
      executorService.submit(() ->
      {
         for(int i = 0; i < N; i++)
         {
            UUIDAndTypeStateKey oldKey = new UUIDAndTypeStateKey(UUID.randomUUID(), StateType.PROCESS_STATUS, Instant.now().minus(5, ChronoUnit.HOURS));
            stateProvider.put(oldKey, UUID.randomUUID());
         }
      });

      for(int i = 0; i < N; i++)
      {
         stateProvider.clean(Instant.now().minus(4, ChronoUnit.HOURS));
      }
   }

}