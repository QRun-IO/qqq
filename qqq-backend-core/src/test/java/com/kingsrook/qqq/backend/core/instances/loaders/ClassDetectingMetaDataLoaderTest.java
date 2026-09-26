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

package com.kingsrook.qqq.backend.core.instances.loaders;


import java.nio.charset.StandardCharsets;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ClassDetectingMetaDataLoader 
 *******************************************************************************/
class ClassDetectingMetaDataLoaderTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBasicSuccess() throws QMetaDataLoaderException
   {
      QMetaDataObject qMetaDataObject = new ClassDetectingMetaDataLoader().fileToMetaDataObject(new QInstance(), IOUtils.toInputStream("""
         class: QTableMetaData
         version: 1
         name: myTable
         backendName: someBackend
         """, StandardCharsets.UTF_8), "myTable.yaml");

      assertThat(qMetaDataObject).isInstanceOf(QTableMetaData.class);
      QTableMetaData qTableMetaData = (QTableMetaData) qMetaDataObject;
      assertEquals("myTable", qTableMetaData.getName());
      assertEquals("someBackend", qTableMetaData.getBackendName());
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcess() throws QMetaDataLoaderException
   {
      QMetaDataObject qMetaDataObject = new ClassDetectingMetaDataLoader().fileToMetaDataObject(new QInstance(), IOUtils.toInputStream("""
         class: QProcessMetaData
         version: 1
         name: myProcess
         tableName: someTable
         """, StandardCharsets.UTF_8), "myProcess.yaml");

      assertThat(qMetaDataObject).isInstanceOf(QProcessMetaData.class);
      QProcessMetaData qProcessMetaData = (QProcessMetaData) qMetaDataObject;
      assertEquals("myProcess", qProcessMetaData.getName());
      assertEquals("someTable", qProcessMetaData.getTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownClassFails()
   {
      assertThatThrownBy(() -> new ClassDetectingMetaDataLoader().fileToMetaDataObject(new QInstance(), IOUtils.toInputStream("""
         class: ya whatever
         version: 1
         name: myTable
         """, StandardCharsets.UTF_8), "whatever.yaml"))
         .isInstanceOf(QMetaDataLoaderException.class)
         .hasMessageContaining("Unexpected class");
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingClassAttributeFails()
   {
      assertThatThrownBy(() -> new ClassDetectingMetaDataLoader().fileToMetaDataObject(new QInstance(), IOUtils.toInputStream("""
         version: 1
         name: myTable
         """, StandardCharsets.UTF_8), "aTable.yaml"))
         .isInstanceOf(QMetaDataLoaderException.class)
         .hasMessageContaining("[class] attribute was not specified");
   }

}