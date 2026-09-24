/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.sharing;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumns;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for sharing scope validation and asset ownership.
 *******************************************************************************/
class SharedRecordProcessUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws QException
   {
      new SavedReportsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
   }



   /*******************************************************************************
    ** Valid scope strings must parse without exception.
    *******************************************************************************/
   @Test
   void testValidateScopeId_validScope_returnsEnum() throws QException
   {
      for(ShareScope scope : ShareScope.values())
      {
         ShareScope result = SharedRecordProcessUtils.validateScopeId(scope.name());
         assertEquals(scope, result);
      }
   }



   /*******************************************************************************
    ** BUG: error message currently says "[null] is not a recognized value" because
    ** `shareScope` is null at the time the exception is caught.  The message should
    ** include the original invalid string (e.g. "INVALID_SCOPE").
    **
    ** Once the bug is fixed, update the containsIgnoringCase assertion to use the
    ** expected string and remove the "null" check.
    *******************************************************************************/
   @Test
   void testValidateScopeId_invalidScope_errorMessageContainsInput()
   {
      assertThatThrownBy(() -> SharedRecordProcessUtils.validateScopeId("INVALID_SCOPE"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("INVALID_SCOPE");
   }



   /*******************************************************************************
    ** getAssetTableAndRecord — unknown table name should throw with the table name.
    *******************************************************************************/
   @Test
   void testGetAssetTableAndRecord_unknownTable_throws()
   {
      assertThatThrownBy(() -> SharedRecordProcessUtils.getAssetTableAndRecord("noSuchTable", "1"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("noSuchTable");
   }



   /*******************************************************************************
    ** getAssetTableAndRecord — table that exists but has no ShareableTableMetaData throws.
    *******************************************************************************/
   @Test
   void testGetAssetTableAndRecord_nonShareableTable_throws()
   {
      assertThatThrownBy(() -> SharedRecordProcessUtils.getAssetTableAndRecord("person", "1"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("not shareable");
   }



   /*******************************************************************************
    ** getAssetTableAndRecord — shareable table with a record that doesn't exist throws.
    *******************************************************************************/
   @Test
   void testGetAssetTableAndRecord_recordNotFound_throws()
   {
      assertThatThrownBy(() -> SharedRecordProcessUtils.getAssetTableAndRecord(SavedReport.TABLE_NAME, "9999"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("9999");
   }



   /*******************************************************************************
    ** getAssetTableAndRecord — happy path: returns table, metadata, and record.
    *******************************************************************************/
   @Test
   void testGetAssetTableAndRecord_existingRecord_returnsAssetTableAndRecord() throws QException
   {
      QRecord insertedReport = insertSavedReport("user-001");
      Integer id             = insertedReport.getValueInteger("id");

      SharedRecordProcessUtils.AssetTableAndRecord result = SharedRecordProcessUtils.getAssetTableAndRecord(SavedReport.TABLE_NAME, String.valueOf(id));

      assertNotNull(result);
      assertNotNull(result.table());
      assertNotNull(result.shareableTableMetaData());
      assertNotNull(result.record());
      assertEquals(id, result.recordId());
   }



   /*******************************************************************************
    ** assertRecordOwnership — current user owns the record → no exception.
    *******************************************************************************/
   @Test
   void testAssertRecordOwnership_ownerMatches_noException() throws QException
   {
      QRecord report = insertSavedReport(DEFAULT_USER_ID);
      SharedRecordProcessUtils.AssetTableAndRecord asset = SharedRecordProcessUtils.getAssetTableAndRecord(
         SavedReport.TABLE_NAME, String.valueOf(report.getValueInteger("id")));

      SharedRecordProcessUtils.assertRecordOwnership(asset, "delete");
   }



   /*******************************************************************************
    ** assertRecordOwnership — different user owns the record → QException thrown.
    *******************************************************************************/
   @Test
   void testAssertRecordOwnership_ownerMismatch_throws() throws QException
   {
      QRecord report = insertSavedReport("other-user");
      SharedRecordProcessUtils.AssetTableAndRecord asset = SharedRecordProcessUtils.getAssetTableAndRecord(
         SavedReport.TABLE_NAME, String.valueOf(report.getValueInteger("id")));

      assertThatThrownBy(() -> SharedRecordProcessUtils.assertRecordOwnership(asset, "delete"))
         .isInstanceOf(QException.class)
         .hasMessageContaining("not the owner");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insertSavedReport(String userId) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(SavedReport.TABLE_NAME);
      insertInput.setRecords(java.util.List.of(
         new QRecord()
            .withValue("label", "Test Report")
            .withValue("tableName", "person")
            .withValue("userId", userId)
            .withValue("columnsJson", JsonUtils.toJson(new ReportColumns().withColumn("id")))
      ));
      QSession originalSession = QContext.getQSession();
      try
      {
         QContext.setQSession(newSession(userId));
         QRecord record = new InsertAction().execute(insertInput).getRecords().get(0);
         assertThat(record.getErrors()).isNullOrEmpty();
         assertNotNull(record.getValueInteger("id"));
         return (record);
      }
      finally
      {
         QContext.setQSession(originalSession);
      }
   }
}
