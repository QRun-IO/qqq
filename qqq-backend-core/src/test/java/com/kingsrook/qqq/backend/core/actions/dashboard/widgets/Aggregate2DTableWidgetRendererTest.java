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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for Aggregate2DTableWidgetRenderer 
 *******************************************************************************/
class Aggregate2DTableWidgetRendererTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
         new QRecord().withValue("lastName", "Simpson").withValue("homeStateId", 50),
         new QRecord().withValue("lastName", "Simpson").withValue("homeStateId", 50),
         new QRecord().withValue("lastName", "Simpson").withValue("homeStateId", 50),
         new QRecord().withValue("lastName", "Simpson").withValue("homeStateId", 49),
         new QRecord().withValue("lastName", "Flanders").withValue("homeStateId", 49),
         new QRecord().withValue("lastName", "Flanders").withValue("homeStateId", 49),
         new QRecord().withValue("lastName", "Burns").withValue("homeStateId", 50)
      )));

      RenderWidgetInput input = new RenderWidgetInput();
      input.setWidgetMetaData(new QWidgetMetaData()
         .withDefaultValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withDefaultValue("valueField", "id")
         .withDefaultValue("rowField", "lastName")
         .withDefaultValue("columnField", "homeStateId")
         .withDefaultValue("orderBys", "row")
      );
      RenderWidgetOutput output    = new Aggregate2DTableWidgetRenderer().render(input);
      TableData          tableData = (TableData) output.getWidgetData();
      System.out.println(tableData.getRows());

      TableDataAssert.assertThat(tableData)
         .hasRowWithColumnContaining("_row", "Simpson", row ->
            row.hasColumnContaining("50", "3")
               .hasColumnContaining("49", "1")
               .hasColumnContaining("_total", "4"))
         .hasRowWithColumnContaining("_row", "Flanders", row ->
            row.hasColumnContaining("50", "0")
               .hasColumnContaining("49", "2")
               .hasColumnContaining("_total", "2"))
         .hasRowWithColumnContaining("_row", "Burns", row ->
            row.hasColumnContaining("50", "1")
               .hasColumnContaining("49", "0")
               .hasColumnContaining("_total", "1"))
         .hasRowWithColumnContaining("_row", "Total", row ->
            row.hasColumnContaining("50", "4")
               .hasColumnContaining("49", "3")
               .hasColumnContaining("_total", "7"));

      List<String> rowLabels = tableData.getRows().stream().map(r -> r.get("_row").toString()).toList();
      assertEquals(List.of("Burns", "Flanders", "Simpson", "Total"), rowLabels);
   }

}