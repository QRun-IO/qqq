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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryAssert;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadRecordUtils;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadValueTypeError;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for BulkInsertTransformStep
 *******************************************************************************/
class BulkInsertTransformStepTest extends BaseTest
{
   public static final String TABLE_NAME = "ukTest";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultipleUniqueKeys() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      QTableMetaData table = defineTable(new QTableMetaData()
         .withName(TABLE_NAME)
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withUniqueKey(new UniqueKey().withFieldName("uuid"))
         .withUniqueKey(new UniqueKey().withFieldName("sku").withFieldName("storeId")), instance);

      reInitInstanceInContext(instance);

      ////////////////////////////////////////////////////////////
      // insert some records that will cause some UK violations //
      ////////////////////////////////////////////////////////////
      TestUtils.insertRecords(table, List.of(
         newUkTestQRecord("uuid-A", "SKU-1", 1),
         newUkTestQRecord("uuid-B", "SKU-2", 1),
         newUkTestQRecord("uuid-C", "SKU-2", 2)
      ));

      ///////////////////////////////////////////
      // setup & run the bulk insert transform //
      ///////////////////////////////////////////
      BulkInsertTransformStep bulkInsertTransformStep = new BulkInsertTransformStep();
      RunBackendStepInput     input                   = new RunBackendStepInput();
      RunBackendStepOutput    output                  = new RunBackendStepOutput();

      input.setTableName(TABLE_NAME);
      input.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      input.setRecords(List.of(
         newUkTestQRecord("uuid-1", "SKU-A", 1), // OK.
         newUkTestQRecord("uuid-1", "SKU-B", 1), // violate uuid UK in this set
         newUkTestQRecord("uuid-2", "SKU-C", 1), // OK.
         newUkTestQRecord("uuid-3", "SKU-C", 2), // OK.
         newUkTestQRecord("uuid-4", "SKU-C", 1), // violate sku/storeId UK in this set
         newUkTestQRecord("uuid-A", "SKU-X", 1), // violate uuid UK from pre-existing records
         newUkTestQRecord("uuid-D", "SKU-2", 1)  // violate sku/storeId UK from pre-existing records
      ));
      bulkInsertTransformStep.preRun(input, output);
      bulkInsertTransformStep.runOnePage(input, output);

      ///////////////////////////////////////////////////////
      // assert about the records that passed successfully //
      ///////////////////////////////////////////////////////
      assertEquals(3, output.getRecords().size());
      assertThat(output.getRecords())
         .anyMatch(r -> recordEquals(r, "uuid-1", "SKU-A", 1))
         .anyMatch(r -> recordEquals(r, "uuid-2", "SKU-C", 1))
         .anyMatch(r -> recordEquals(r, "uuid-3", "SKU-C", 2));

      /////////////////////////////
      // assert about the errors //
      /////////////////////////////
      ArrayList<ProcessSummaryLineInterface> processSummary = bulkInsertTransformStep.doGetProcessSummary(output, false);
      List<ProcessSummaryLine> errorLines = processSummary.stream()
         .filter(pl -> pl instanceof ProcessSummaryLine psl && psl.getStatus().equals(Status.ERROR))
         .map(pl -> ((ProcessSummaryLine) pl))
         .collect(Collectors.toList());
      assertEquals(2, errorLines.size());
      assertThat(errorLines)
         .anyMatch(psl -> psl.getMessage().contains("Uuid") && psl.getCount().equals(2))
         .anyMatch(psl -> psl.getMessage().contains("Sku and Store Id") && psl.getCount().equals(2));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private QTableMetaData defineTable(QTableMetaData table, QInstance instance)
   {
      table.withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("uuid", QFieldType.STRING))
         .withField(new QFieldMetaData("sku", QFieldType.STRING))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING));
      instance.addTable(table);
      return table;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoUniqueKeys() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      QTableMetaData table = defineTable(new QTableMetaData()
         .withName(TABLE_NAME)
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME), instance);
      reInitInstanceInContext(instance);

      ////////////////////////////////////////////////////////////
      // insert some records that will cause some UK violations //
      ////////////////////////////////////////////////////////////
      TestUtils.insertRecords(table, List.of(
         newUkTestQRecord("uuid-A", "SKU-1", 1),
         newUkTestQRecord("uuid-B", "SKU-2", 1),
         newUkTestQRecord("uuid-C", "SKU-2", 2)
      ));

      ///////////////////////////////////////////
      // setup & run the bulk insert transform //
      ///////////////////////////////////////////
      BulkInsertTransformStep bulkInsertTransformStep = new BulkInsertTransformStep();
      RunBackendStepInput     input                   = new RunBackendStepInput();
      RunBackendStepOutput    output                  = new RunBackendStepOutput();

      input.setTableName(TABLE_NAME);
      input.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      input.setRecords(List.of(
         newUkTestQRecord("uuid-1", "SKU-A", 1), // OK.
         newUkTestQRecord("uuid-1", "SKU-B", 1), // violate uuid UK in this set
         newUkTestQRecord("uuid-2", "SKU-C", 1), // OK.
         newUkTestQRecord("uuid-3", "SKU-C", 2), // OK.
         newUkTestQRecord("uuid-4", "SKU-C", 1), // violate sku/storeId UK in this set
         newUkTestQRecord("uuid-A", "SKU-X", 1), // violate uuid UK from pre-existing records
         newUkTestQRecord("uuid-D", "SKU-2", 1)  // violate sku/storeId UK from pre-existing records
      ));
      bulkInsertTransformStep.preRun(input, output);
      bulkInsertTransformStep.runOnePage(input, output);

      ///////////////////////////////////
      // assert that all records pass. //
      ///////////////////////////////////
      assertEquals(7, output.getRecords().size());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private boolean recordEquals(QRecord record, String uuid, String sku, Integer storeId)
   {
      return (record.getValue("uuid").equals(uuid)
              && record.getValue("sku").equals(sku)
              && record.getValue("storeId").equals(storeId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord newUkTestQRecord(String uuid, String sku, int storeId)
   {
      return new QRecord()
         .withValue("uuid", uuid)
         .withValue("sku", sku)
         .withValue("storeId", storeId)
         .withValue("name", "Some Item");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValueMappingTypeErrors() throws QException
   {
      ///////////////////////////////////////////
      // setup & run the bulk insert transform //
      ///////////////////////////////////////////
      BulkInsertTransformStep bulkInsertTransformStep = new BulkInsertTransformStep();
      RunBackendStepInput     input                   = new RunBackendStepInput();
      RunBackendStepOutput    output                  = new RunBackendStepOutput();
      Serializable[]          emptyValues             = new Serializable[0];

      input.setTableName(TestUtils.TABLE_NAME_ORDER);
      input.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      input.setRecords(ListBuilder.of(
         BulkLoadRecordUtils.addBackendDetailsAboutFileRows(new QRecord(), new BulkLoadFileRow(emptyValues, 1))
            .withError(new BulkLoadValueTypeError("storeId", "A", QFieldType.INTEGER, "Store"))
            .withError(new BulkLoadValueTypeError("orderDate", "47", QFieldType.DATE, "Order Date")),
         BulkLoadRecordUtils.addBackendDetailsAboutFileRows(new QRecord(), new BulkLoadFileRow(emptyValues, 2))
            .withError(new BulkLoadValueTypeError("storeId", "BCD", QFieldType.INTEGER, "Store"))
      ));

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // add 102 records with an error in the total field - which is more than the number of examples that should be given //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      for(int i = 0; i < 102; i++)
      {
         input.getRecords().add(BulkLoadRecordUtils.addBackendDetailsAboutFileRows(new QRecord(), new BulkLoadFileRow(emptyValues, 3 + i))
            .withError(new BulkLoadValueTypeError("total", "three-fifty-" + i, QFieldType.DECIMAL, "Total")));
      }

      bulkInsertTransformStep.preRun(input, output);
      bulkInsertTransformStep.runOnePage(input, output);
      ArrayList<ProcessSummaryLineInterface> processSummary = bulkInsertTransformStep.getProcessSummary(output, false);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("Cannot convert value for field [Store] to type [Integer]")
         .hasMessageContaining("Values:")
         .doesNotHaveMessageContaining("Example Values:")
         .hasAnyBulletsOfTextContaining("Row 1 [A]")
         .hasAnyBulletsOfTextContaining("Row 2 [BCD]")
         .hasStatus(Status.ERROR)
         .hasCount(2);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("Cannot convert value for field [Order Date] to type [Date]")
         .hasMessageContaining("Values:")
         .doesNotHaveMessageContaining("Example Values:")
         .hasAnyBulletsOfTextContaining("Row 1 [47]")
         .hasStatus(Status.ERROR)
         .hasCount(1);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("Cannot convert value for field [Total] to type [Decimal]")
         .hasMessageContaining("Example Values:")
         .hasAnyBulletsOfTextContaining("Row 3 [three-fifty-0]")
         .hasAnyBulletsOfTextContaining("Row 4 [three-fifty-1]")
         .hasAnyBulletsOfTextContaining("Row 5 [three-fifty-2]")
         .doesNotHaveAnyBulletsOfTextContaining("three-fifty-101")
         .hasStatus(Status.ERROR)
         .hasCount(102);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRollupOfValidationErrors() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);

      ///////////////////////////////////////////
      // setup & run the bulk insert transform //
      ///////////////////////////////////////////
      BulkInsertTransformStep bulkInsertTransformStep = new BulkInsertTransformStep();
      RunBackendStepInput     input                   = new RunBackendStepInput();
      RunBackendStepOutput    output                  = new RunBackendStepOutput();
      Serializable[]          emptyValues             = new Serializable[0];

      String tooLong = ".".repeat(201);

      input.setTableName(TestUtils.TABLE_NAME_ORDER);
      input.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      input.setRecords(ListBuilder.of(
         BulkLoadRecordUtils.addBackendDetailsAboutFileRows(new QRecord().withValue("shipToName", tooLong), new BulkLoadFileRow(emptyValues, 1)),
         BulkLoadRecordUtils.addBackendDetailsAboutFileRows(new QRecord().withValue("shipToName", "OK").withValue("storeId", 1), new BulkLoadFileRow(emptyValues, 2))
      ));

      /////////////////////////////////////////////////////////////////////
      // add 102 records with no security key - which should be an error //
      /////////////////////////////////////////////////////////////////////
      for(int i = 0; i < 102; i++)
      {
         input.getRecords().add(BulkLoadRecordUtils.addBackendDetailsAboutFileRows(new QRecord(), new BulkLoadFileRow(emptyValues, 3 + i)));
      }

      bulkInsertTransformStep.preRun(input, output);
      bulkInsertTransformStep.runOnePage(input, output);
      ArrayList<ProcessSummaryLineInterface> processSummary = bulkInsertTransformStep.getProcessSummary(output, false);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("value for Ship To Name is too long")
         .hasMessageContaining("Records:")
         .doesNotHaveMessageContaining("Example Records:")
         .hasAnyBulletsOfTextContaining("Row 1")
         .hasStatus(Status.ERROR)
         .hasCount(1);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("without a value in the field: Store Id")
         .hasMessageContaining("Example Records:")
         .hasAnyBulletsOfTextContaining("Row 1")
         .hasAnyBulletsOfTextContaining("Row 3")
         .hasAnyBulletsOfTextContaining("Row 4")
         .doesNotHaveAnyBulletsOfTextContaining("Row 101")
         .hasStatus(Status.ERROR)
         .hasCount(103); // the 102, plus row 1.

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("Order record will be inserted")
         .hasStatus(Status.OK)
         .hasCount(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPropagationOfErrorsFromAssociations() throws QException
   {
      ////////////////////////////////////////////////
      // set line item lineNumber field as required //
      ////////////////////////////////////////////////
      QInstance instance = TestUtils.defineInstance();
      instance.getTable(TestUtils.TABLE_NAME_LINE_ITEM).getField("lineNumber").setIsRequired(true);
      reInitInstanceInContext(instance);

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);

      ///////////////////////////////////////////
      // setup & run the bulk insert transform //
      ///////////////////////////////////////////
      BulkInsertTransformStep bulkInsertTransformStep = new BulkInsertTransformStep();
      RunBackendStepInput     input                   = new RunBackendStepInput();
      RunBackendStepOutput    output                  = new RunBackendStepOutput();

      input.setTableName(TestUtils.TABLE_NAME_ORDER);
      input.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      input.setRecords(ListBuilder.of(
         new QRecord().withValue("storeId", 1).withAssociatedRecord("orderLine", new QRecord()),
         new QRecord().withValue("storeId", 1).withAssociatedRecord("orderLine", new QRecord().withError(new BadInputStatusMessage("some mapping error")))
      ));

      bulkInsertTransformStep.preRun(input, output);
      bulkInsertTransformStep.runOnePage(input, output);
      ArrayList<ProcessSummaryLineInterface> processSummary = bulkInsertTransformStep.getProcessSummary(output, false);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("some mapping error")
         .hasMessageContaining("Records:")
         .hasStatus(Status.ERROR)
         .hasCount(1);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("records were processed from the file")
         .hasStatus(Status.INFO)
         .hasCount(2);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("Order record will be inserted")
         .hasStatus(Status.OK)
         .hasCount(1);

      ProcessSummaryAssert.assertThat(processSummary)
         .hasLineWithMessageContaining("Order Line record will be inserted")
         .hasStatus(Status.OK)
         .hasCount(1);
   }

}