/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.tracing.ProcessTracerKeyRecordMessage;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for BulkInsertStepUtils
 *******************************************************************************/
class BulkInsertStepUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetStorageInputForTheFile_nullValue_throwsQException()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      // "theFile" is not set — value is null

      assertThatThrownBy(() -> BulkInsertStepUtils.getStorageInputForTheFile(input))
         .isInstanceOf(QException.class)
         .hasMessageContaining("not found in process state");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetStorageInputForTheFile_emptyList_throwsQException()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("theFile", new ArrayList<StorageInput>());

      assertThatThrownBy(() -> BulkInsertStepUtils.getStorageInputForTheFile(input))
         .isInstanceOf(QException.class)
         .hasMessageContaining("empty list");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetStorageInputForTheFile_returnsFirstElement() throws QException
   {
      StorageInput    storageInput = new StorageInput("myTable").withReference("some/path.csv");
      RunProcessInput rpi          = new RunProcessInput();
      BulkInsertStepUtils.setStorageInputForTheFile(rpi, storageInput);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("theFile", rpi.getValues().get("theFile"));

      StorageInput result = BulkInsertStepUtils.getStorageInputForTheFile(input);
      assertEquals("some/path.csv", result.getReference());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetStorageInputForTheFile_storesListWithOneElement()
   {
      StorageInput    storageInput = new StorageInput("myTable").withReference("path/file.csv");
      RunProcessInput rpi          = new RunProcessInput();

      BulkInsertStepUtils.setStorageInputForTheFile(rpi, storageInput);

      @SuppressWarnings("unchecked")
      ArrayList<StorageInput> stored = (ArrayList<StorageInput>) rpi.getValues().get("theFile");
      assertNotNull(stored);
      assertEquals(1, stored.size());
      assertEquals("path/file.csv", stored.get(0).getReference());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetNextStepStreamedETLPreview_setsExpectedOverride()
   {
      RunBackendStepOutput output = new RunBackendStepOutput();
      BulkInsertStepUtils.setNextStepStreamedETLPreview(output);
      assertEquals("receiveValueMapping", output.getOverrideLastStepName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetNextStepPrepareValueMapping_setsExpectedOverride()
   {
      RunBackendStepOutput output = new RunBackendStepOutput();
      BulkInsertStepUtils.setNextStepPrepareValueMapping(output);
      assertEquals("receiveFileMapping", output.getOverrideLastStepName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetBulkLoadProfile_v1BasicFields_parsedCorrectly()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("version", "v1");
      input.addValue("layout", "FLAT");
      input.addValue("hasHeaderRow", true);
      input.addValue("keyFields", "id");
      input.addValue("isBulkEdit", false);
      input.addValue("fieldListJSON", """
         [{"fieldName":"firstName","headerName":"First Name","columnIndex":0,"doValueMapping":false,"clearIfEmpty":false}]
         """);

      BulkLoadProfile profile = BulkInsertStepUtils.getBulkLoadProfile(input);

      assertNotNull(profile);
      assertEquals("v1", profile.getVersion());
      assertEquals("FLAT", profile.getLayout());
      assertTrue(profile.getHasHeaderRow());
      assertEquals(1, profile.getFieldList().size());
      assertEquals("firstName", profile.getFieldList().get(0).getFieldName());
      assertEquals("First Name", profile.getFieldList().get(0).getHeaderName());
      assertEquals(0, profile.getFieldList().get(0).getColumnIndex());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetBulkLoadProfile_v1WithValueMappings_parsedCorrectly()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("version", "v1");
      input.addValue("layout", "FLAT");
      input.addValue("hasHeaderRow", true);
      input.addValue("keyFields", "");
      input.addValue("isBulkEdit", false);
      input.addValue("fieldListJSON", """
         [{"fieldName":"status","doValueMapping":true,"valueMappings":{"active":"A","inactive":"I"}}]
         """);

      BulkLoadProfile profile = BulkInsertStepUtils.getBulkLoadProfile(input);

      assertNotNull(profile.getFieldList().get(0).getValueMappings());
      assertEquals("A", profile.getFieldList().get(0).getValueMappings().get("active"));
      assertEquals("I", profile.getFieldList().get(0).getValueMappings().get("inactive"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetBulkLoadProfile_v1WithOptionalFieldsAbsent_noNullPointer()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("version", "v1");
      input.addValue("layout", "FLAT");
      input.addValue("hasHeaderRow", false);
      input.addValue("keyFields", "");
      input.addValue("isBulkEdit", false);
      // headerName and columnIndex omitted — should be null, not throw
      input.addValue("fieldListJSON", """
         [{"fieldName":"lastName","doValueMapping":false,"clearIfEmpty":true}]
         """);

      BulkLoadProfile profile = BulkInsertStepUtils.getBulkLoadProfile(input);

      assertNull(profile.getFieldList().get(0).getHeaderName());
      assertNull(profile.getFieldList().get(0).getColumnIndex());
      assertTrue(profile.getFieldList().get(0).getClearIfEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetBulkLoadProfile_unsupportedVersion_throwsIllegalArgument()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("version", "v99");
      input.addValue("fieldListJSON", "[]");

      assertThatThrownBy(() -> BulkInsertStepUtils.getBulkLoadProfile(input))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("v99");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHandleSavedBulkLoadProfileIdValue_nullId_returnsNull() throws QException
   {
      RunBackendStepInput  input  = new RunBackendStepInput();
      RunBackendStepOutput output = new RunBackendStepOutput();
      // savedBulkLoadProfileId not set

      assertNull(BulkInsertStepUtils.handleSavedBulkLoadProfileIdValue(input, output));
      assertNull(output.getValues().get("savedBulkLoadProfileRecord"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsHeadless_falseByDefault()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      assertFalse(BulkInsertStepUtils.isHeadless(input));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIsHeadless_trueWhenSet()
   {
      RunProcessInput rpi = new RunProcessInput();
      BulkInsertStepUtils.setHeadless(rpi);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("isHeadless", rpi.getValues().get("isHeadless"));

      assertTrue(BulkInsertStepUtils.isHeadless(input));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetProcessTracerKeyRecordMessage_correctType_returned()
   {
      ProcessTracerKeyRecordMessage msg = new ProcessTracerKeyRecordMessage("order", 42);
      RunProcessInput               rpi = new RunProcessInput();
      BulkInsertStepUtils.setProcessTracerKeyRecordMessage(rpi, msg);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("processTracerKeyRecordMessage", rpi.getValues().get("processTracerKeyRecordMessage"));

      ProcessTracerKeyRecordMessage result = BulkInsertStepUtils.getProcessTracerKeyRecordMessage(input);
      assertNotNull(result);
      assertEquals("order", result.getTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetProcessTracerKeyRecordMessage_wrongType_returnsNull()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue("processTracerKeyRecordMessage", "not-a-message");

      assertNull(BulkInsertStepUtils.getProcessTracerKeyRecordMessage(input));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetProcessTracerKeyRecordMessage_notSet_returnsNull()
   {
      RunBackendStepInput input = new RunBackendStepInput();
      assertNull(BulkInsertStepUtils.getProcessTracerKeyRecordMessage(input));
   }

}
