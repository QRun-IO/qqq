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

package com.kingsrook.qqq.backend.core.modules.backend;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for SequenceResyncResult, and the default (no-op) resyncKeySequences.
 *******************************************************************************/
class SequenceResyncResultTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDefaultResyncIsNoOp() throws QException
   {
      QBackendMetaData backend = new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class);
      List<SequenceResyncResult> results = new MemoryBackendModule().resyncKeySequences(backend, List.of(new QTableMetaData().withName("t")));
      assertTrue(results.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetters()
   {
      SequenceResyncResult result = new SequenceResyncResult("t", SequenceResyncResult.Status.RESYNCED, 5L);
      assertEquals("t", result.getTableName());
      assertEquals(SequenceResyncResult.Status.RESYNCED, result.getStatus());
      assertEquals(5L, result.getNewValue());
      assertTrue(result.toString().contains("RESYNCED"));
   }
}
