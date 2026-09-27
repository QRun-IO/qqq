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

package com.kingsrook.qqq.backend.core.model.audits;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for AuditsMetaDataProvider
 *******************************************************************************/
class AuditsMetaDataProviderTest extends BaseTest
{

   /*******************************************************************************
    ** Test that the default recordIdType is INTEGER for backwards compatibility.
    *******************************************************************************/
   @Test
   void testDefaultRecordIdTypeIsInteger() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      QTableMetaData auditTable = qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT);
      assertNotNull(auditTable);

      QFieldMetaData recordIdField = auditTable.getField("recordId");
      assertNotNull(recordIdField);
      assertEquals(QFieldType.INTEGER, recordIdField.getType());
   }



   /*******************************************************************************
    ** Test that recordIdType can be configured as STRING.
    *******************************************************************************/
   @Test
   void testRecordIdTypeConfiguredAsString() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider()
         .withRecordIdType(QFieldType.STRING)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      QTableMetaData auditTable = qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT);
      assertNotNull(auditTable);

      QFieldMetaData recordIdField = auditTable.getField("recordId");
      assertNotNull(recordIdField);
      assertEquals(QFieldType.STRING, recordIdField.getType());
   }



   /*******************************************************************************
    ** Test the getter and setter for recordIdType.
    *******************************************************************************/
   @Test
   void testRecordIdTypeGetterSetter()
   {
      AuditsMetaDataProvider provider = new AuditsMetaDataProvider();

      //////////////////////////////////
      // verify default is INTEGER    //
      //////////////////////////////////
      assertEquals(QFieldType.INTEGER, provider.getRecordIdType());

      ////////////////////////////////////////
      // verify setter changes the value    //
      ////////////////////////////////////////
      provider.setRecordIdType(QFieldType.STRING);
      assertEquals(QFieldType.STRING, provider.getRecordIdType());

      //////////////////////////////////////////
      // verify fluent setter returns self    //
      //////////////////////////////////////////
      AuditsMetaDataProvider result = provider.withRecordIdType(QFieldType.LONG);
      assertEquals(provider, result);
      assertEquals(QFieldType.LONG, provider.getRecordIdType());
   }



   /*******************************************************************************
    ** Test that all audit tables are created when defineAll is called.
    *******************************************************************************/
   @Test
   void testDefineAllCreatesAllTables() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      //////////////////////////////////////
      // verify all four tables are added //
      //////////////////////////////////////
      assertNotNull(qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT));
      assertNotNull(qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT_TABLE));
      assertNotNull(qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER));
      assertNotNull(qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL));

      /////////////////////////////////////////////
      // verify possible value sources are added //
      /////////////////////////////////////////////
      assertNotNull(qInstance.getPossibleValueSource(AuditsMetaDataProvider.TABLE_NAME_AUDIT_TABLE));
      assertNotNull(qInstance.getPossibleValueSource(AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER));
      assertNotNull(qInstance.getPossibleValueSource(AuditsMetaDataProvider.TABLE_NAME_AUDIT));

      /////////////////////////////////////////
      // verify process is added             //
      /////////////////////////////////////////
      assertNotNull(qInstance.getProcess("GetAuditsForRecord"));
   }

}
