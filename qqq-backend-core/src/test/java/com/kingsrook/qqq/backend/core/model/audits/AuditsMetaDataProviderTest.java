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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


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


   /*******************************************************************************
    ** The audit record screens: named sections, the field changes listed on an
    ** audit record through a child-record widget, labeled detail records.
    *******************************************************************************/
   @Test
   void testDefineAllDefinesRecordScreens() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().withDefineRecordScreens(true).defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      QTableMetaData audit = qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT);
      assertEquals(List.of("audit", AuditsMetaDataProvider.FIELD_CHANGES_SECTION_NAME), audit.getSections().stream().map(QFieldSection::getName).toList());
      String widgetName = audit.getSection(AuditsMetaDataProvider.FIELD_CHANGES_SECTION_NAME).getWidgetName();
      assertEquals(WidgetType.CHILD_RECORD_LIST.getType(), qInstance.getWidget(widgetName).getType());
      assertEquals("Table", audit.getField("auditTableId").getLabel());

      QTableMetaData detail = qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT_DETAIL);
      assertEquals(List.of("fieldName"), detail.getRecordLabelFields());
      assertEquals(List.of("change"), detail.getSections().stream().map(QFieldSection::getName).toList());

      for(String tableName : List.of(AuditsMetaDataProvider.TABLE_NAME_AUDIT_TABLE, AuditsMetaDataProvider.TABLE_NAME_AUDIT_USER))
      {
         assertNotNull(qInstance.getTable(tableName).getSections(), tableName);
      }
   }



   /*******************************************************************************
    ** An application's own sections are kept.
    *******************************************************************************/
   @Test
   void testDefineStandardAuditScreensKeepsExistingSections() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      AuditsMetaDataProvider provider = new AuditsMetaDataProvider().withDefineRecordScreens(true);
      provider.defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      int sections = qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT).getSections().size();

      provider.defineStandardAuditScreens(qInstance);
      assertEquals(sections, qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT).getSections().size());
   }


   /*******************************************************************************
    ** Without the opt-in, defineAll leaves the audit tables unsectioned (so an
    ** application may still add fields to them).
    *******************************************************************************/
   @Test
   void testRecordScreensAreOptIn() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      assertNull(qInstance.getTable(AuditsMetaDataProvider.TABLE_NAME_AUDIT).getSections());
   }

}
