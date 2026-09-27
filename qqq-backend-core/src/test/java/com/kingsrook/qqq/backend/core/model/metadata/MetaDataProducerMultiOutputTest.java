/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for MetaDataProducerMultiOutput 
 *******************************************************************************/
class MetaDataProducerMultiOutputTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetEachAndGet()
   {
      //////////////////////
      // given this setup //
      //////////////////////
      MetaDataProducerMultiOutput metaDataProducerMultiOutput = new MetaDataProducerMultiOutput();
      metaDataProducerMultiOutput.add(new QTableMetaData().withName("tableA"));
      metaDataProducerMultiOutput.add(new QProcessMetaData().withName("processB"));
      metaDataProducerMultiOutput.add(new QBackendMetaData().withName("backendC"));
      metaDataProducerMultiOutput.add(new QTableMetaData().withName("tableD"));

      ///////////////////////////
      // test calls to getEach //
      ///////////////////////////
      List<QTableMetaData> tables = metaDataProducerMultiOutput.getEach(QTableMetaData.class);
      assertEquals(2, tables.size());
      assertEquals("tableA", tables.get(0).getName());
      assertEquals("tableD", tables.get(1).getName());

      List<QProcessMetaData> processes = metaDataProducerMultiOutput.getEach(QProcessMetaData.class);
      assertEquals(1, processes.size());
      assertEquals("processB", processes.get(0).getName());

      List<QBackendMetaData> backends = metaDataProducerMultiOutput.getEach(QBackendMetaData.class);
      assertEquals(1, backends.size());
      assertEquals("backendC", backends.get(0).getName());

      List<QQueueProviderMetaData> queueProviders = metaDataProducerMultiOutput.getEach(QQueueProviderMetaData.class);
      assertEquals(0, queueProviders.size());

      //////////////////////////////////////////////
      // test some calls to get that takes a name //
      //////////////////////////////////////////////
      assertEquals("tableA", metaDataProducerMultiOutput.get(QTableMetaData.class, "tableA").getName());
      assertNull(metaDataProducerMultiOutput.get(QProcessMetaData.class, "tableA"));
      assertNull(metaDataProducerMultiOutput.get(QQueueMetaData.class, "queueQ"));
   }

}