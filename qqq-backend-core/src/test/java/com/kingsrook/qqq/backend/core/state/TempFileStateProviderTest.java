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


import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for TempFileStateProvider
 *******************************************************************************/
public class TempFileStateProviderTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testStateNotFound()
   {
      TempFileStateProvider stateProvider = TempFileStateProvider.getInstance();
      UUIDAndTypeStateKey   key           = new UUIDAndTypeStateKey(StateType.PROCESS_STATUS);

      Assertions.assertTrue(stateProvider.get(QRecord.class, key).isEmpty(), "Key not found in state should return empty");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSimpleStateFound()
   {
      TempFileStateProvider stateProvider = TempFileStateProvider.getInstance();
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
      TempFileStateProvider stateProvider = TempFileStateProvider.getInstance();
      UUIDAndTypeStateKey   key           = new UUIDAndTypeStateKey(StateType.PROCESS_STATUS);

      String  uuid    = UUID.randomUUID().toString();
      QRecord qRecord = new QRecord().withValue("uuid", uuid);
      stateProvider.put(key, qRecord);

      Assertions.assertThrows(Exception.class, () ->
      {
         stateProvider.get(QTableMetaData.class, key);
      });
   }

}