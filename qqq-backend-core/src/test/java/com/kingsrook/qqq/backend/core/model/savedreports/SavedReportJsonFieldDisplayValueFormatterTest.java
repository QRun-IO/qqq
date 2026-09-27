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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableValue;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for SavedReportJsonFieldDisplayValueFormatter 
 *******************************************************************************/
class SavedReportJsonFieldDisplayValueFormatterTest extends BaseTest
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
   void testPostQuery() throws QException
   {
      UnsafeFunction<SavedReport, QRecord, QException> customize = savedReport ->
      {
         QInstance      qInstance = QContext.getQInstance();
         QTableMetaData table     = qInstance.getTable(SavedReport.TABLE_NAME);

         QRecord record = savedReport.toQRecord();

         for(String fieldName : List.of("queryFilterJson", "columnsJson", "pivotTableJson"))
         {
            SavedReportJsonFieldDisplayValueFormatter.getInstance().apply(ValueBehaviorApplier.Action.FORMATTING, List.of(record), qInstance, table, table.getField(fieldName));
         }

         return (record);
      };

      {
         QRecord record = customize.apply(new SavedReport()
            .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()))
            .withColumnsJson(JsonUtils.toJson(new ReportColumns()))
            .withPivotTableJson(JsonUtils.toJson(new PivotTableDefinition())));

         assertEquals("0 Filters", record.getDisplayValue("queryFilterJson"));
         assertEquals("0 Columns", record.getDisplayValue("columnsJson"));
         assertEquals("0 Rows, 0 Columns, and 0 Values", record.getDisplayValue("pivotTableJson"));
      }

      {
         QRecord record = customize.apply(new SavedReport()
            .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()))
            .withColumnsJson(JsonUtils.toJson(new ReportColumns())));

         assertEquals("0 Filters", record.getDisplayValue("queryFilterJson"));
         assertEquals("0 Columns", record.getDisplayValue("columnsJson"));
         assertNull(record.getDisplayValue("pivotTableJson"));
      }

      {
         QRecord record = customize.apply(new SavedReport()
            .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()
               .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.IS_NOT_BLANK))))
            .withColumnsJson(JsonUtils.toJson(new ReportColumns()
               .withColumn(new ReportColumn().withName("birthDate"))))
            .withPivotTableJson(JsonUtils.toJson(new PivotTableDefinition()
               .withRow(new PivotTableGroupBy())
               .withValue(new PivotTableValue())
            )));

         assertEquals("1 Filter", record.getDisplayValue("queryFilterJson"));
         assertEquals("1 Column", record.getDisplayValue("columnsJson"));
         assertEquals("1 Row, 0 Columns, and 1 Value", record.getDisplayValue("pivotTableJson"));
      }

      {
         QRecord record = customize.apply(new SavedReport()
            .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withQueryFilterJson(JsonUtils.toJson(new QQueryFilter()
               .withCriteria(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, 1))
               .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.IS_NOT_BLANK))))
            .withColumnsJson(JsonUtils.toJson(new ReportColumns()
               .withColumn(new ReportColumn().withName("__check__").withIsVisible(true))
               .withColumn(new ReportColumn().withName("id"))
               .withColumn(new ReportColumn().withName("firstName").withIsVisible(true))
               .withColumn(new ReportColumn().withName("lastName").withIsVisible(false))
               .withColumn(new ReportColumn().withName("birthDate"))))
            .withPivotTableJson(JsonUtils.toJson(new PivotTableDefinition()
               .withRow(new PivotTableGroupBy())
               .withRow(new PivotTableGroupBy())
               .withColumn(new PivotTableGroupBy())
               .withValue(new PivotTableValue())
               .withValue(new PivotTableValue())
               .withValue(new PivotTableValue())
            )));

         assertEquals("2 Filters", record.getDisplayValue("queryFilterJson"));
         assertEquals("3 Columns", record.getDisplayValue("columnsJson"));
         assertEquals("2 Rows, 1 Column, and 3 Values", record.getDisplayValue("pivotTableJson"));
      }

      {
         QRecord record = customize.apply(new SavedReport()
            .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withQueryFilterJson("blah")
            .withColumnsJson("<xml?>")
            .withPivotTableJson("{]"));

         assertEquals("Invalid Filter...", record.getDisplayValue("queryFilterJson"));
         assertEquals("Invalid Columns...", record.getDisplayValue("columnsJson"));
         assertEquals("Invalid Pivot Table...", record.getDisplayValue("pivotTableJson"));
      }

   }

}