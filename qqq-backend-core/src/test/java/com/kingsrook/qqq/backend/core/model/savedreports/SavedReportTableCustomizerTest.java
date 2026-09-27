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

package com.kingsrook.qqq.backend.core.model.savedreports;


import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableFunction;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.FilterVariableExpression;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for SavedReportTableCustomizer
 *******************************************************************************/
class SavedReportTableCustomizerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QContext.getQInstance().add(new SavedReportsMetaDataProvider().defineSavedReportTable(TestUtils.MEMORY_BACKEND_NAME, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPreInsertAndPreUpdateAreWired() throws QException
   {
      SavedReport badRecord = new SavedReport()
         .withLabel("My Report")
         .withTableName("notATable");

      /////////////////////////////////////////////////////////////////////
      // assertions to apply both to a failed insert and a failed update //
      /////////////////////////////////////////////////////////////////////
      Consumer<QRecord> asserter = record -> assertThat(record.getErrors())
         .hasSizeGreaterThanOrEqualTo(2)
         .anyMatch(e -> e.getMessage().contains("Unrecognized table name"))
         .anyMatch(e -> e.getMessage().contains("must contain at least 1 column"));

      ////////////////////////////////////////////////////////////
      // go through insert action, to ensure wired-up correctly //
      ////////////////////////////////////////////////////////////
      InsertOutput insertOutput = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(badRecord));
      asserter.accept(insertOutput.getRecords().get(0));

      ////////////////////////////////
      // likewise for update action //
      ////////////////////////////////
      UpdateOutput updateOutput = new UpdateAction().execute(new UpdateInput(SavedReport.TABLE_NAME).withRecordEntity(badRecord));
      asserter.accept(updateOutput.getRecords().get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPrepareFilterVariable()
   {
      QQueryFilter qQueryFilter = new QQueryFilter(new QFilterCriteria("id", EQUALS, new FilterVariableExpression()));

      QRecord record = new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withQueryFilterJson(JsonUtils.toJson(qQueryFilter))
         .withColumnsJson(JsonUtils.toJson(new ReportColumns()
            .withColumn("id")
            .withColumn("firstName")
            .withColumn("lastName")
            .withColumn("birthDate")))
         .toQRecord();

      new SavedReportTableCustomizer().preValidateRecord(record);

      assertThat(record.getValueString("queryFilterJson").contains("idEquals"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testParseFails()
   {
      QRecord record = new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withQueryFilterJson("...")
         .withColumnsJson("x")
         .withPivotTableJson("[")
         .toQRecord();

      new SavedReportTableCustomizer().preValidateRecord(record);

      assertThat(record.getErrors())
         .hasSize(3)
         .anyMatch(e -> e.getMessage().contains("Unable to parse queryFilterJson"))
         .anyMatch(e -> e.getMessage().contains("Unable to parse columnsJson"))
         .anyMatch(e -> e.getMessage().contains("Unable to parse pivotTableJson"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoColumns()
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // given a reportColumns object, serialize it to json, put it in a saved report record, and run the pre-validator //
      // then assert we got error saying there were no columns.                                                         //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      Consumer<ReportColumns> asserter = reportColumns ->
      {
         SavedReport savedReport = new SavedReport()
            .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()))
            .withColumnsJson(JsonUtils.toJson(reportColumns));

         QRecord record = savedReport.toQRecord();
         new SavedReportTableCustomizer().preValidateRecord(record);

         assertThat(record.getErrors())
            .hasSize(1)
            .anyMatch(e -> e.getMessage().contains("must contain at least 1 column"));
      };

      asserter.accept(new ReportColumns());
      asserter.accept(new ReportColumns().withColumns(null));
      asserter.accept(new ReportColumns().withColumns(new ArrayList<>()));
      asserter.accept(new ReportColumns().withColumn(new ReportColumn()
         .withName("id").withIsVisible(false)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPivotTables()
   {
      BiConsumer<PivotTableDefinition, List<String>> asserter = (PivotTableDefinition ptd, List<String> expectedAnyMessageToContain) ->
      {
         SavedReport savedReport = new SavedReport()
            .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()))
            .withColumnsJson(JsonUtils.toJson(new ReportColumns()
               .withColumn("id")
               .withColumn("firstName")
               .withColumn("lastName")
               .withColumn("birthDate")))
            .withPivotTableJson(JsonUtils.toJson(ptd));

         QRecord record = savedReport.toQRecord();
         new SavedReportTableCustomizer().preValidateRecord(record);

         assertThat(record.getErrors()).hasSize(expectedAnyMessageToContain.size());

         for(String expected : expectedAnyMessageToContain)
         {
            assertThat(record.getErrors())
               .anyMatch(e -> e.getMessage().contains(expected));
         }
      };

      asserter.accept(new PivotTableDefinition(), List.of("must contain at least 1 row"));

      asserter.accept(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("id"))
            .withRow(new PivotTableGroupBy()),
         List.of("Missing field name for at least one pivot table row"));

      asserter.accept(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("id"))
            .withRow(new PivotTableGroupBy().withFieldName("createDate")),
         List.of("row is using field (Create Date) which is not an active column"));

      asserter.accept(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("id"))
            .withColumn(new PivotTableGroupBy()),
         List.of("Missing field name for at least one pivot table column"));

      asserter.accept(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("id"))
            .withColumn(new PivotTableGroupBy().withFieldName("createDate")),
         List.of("column is using field (Create Date) which is not an active column"));

      asserter.accept(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("id"))
            .withValue(new PivotTableValue().withFunction(PivotTableFunction.SUM)),
         List.of("Missing field name for at least one pivot table value"));

      asserter.accept(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("id"))
            .withValue(new PivotTableValue().withFieldName("createDate").withFunction(PivotTableFunction.SUM)),
         List.of("value is using field (Create Date) which is not an active column"));

      asserter.accept(new PivotTableDefinition()
            .withRow(new PivotTableGroupBy().withFieldName("id"))
            .withValue(new PivotTableValue().withFieldName("firstName")),
         List.of("Missing function for at least one pivot table value"));
   }

}
