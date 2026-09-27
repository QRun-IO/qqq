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

package com.kingsrook.sampleapp.dashboard.widgets;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;


/*******************************************************************************
 **
 *******************************************************************************/
public class PersonsByCreateDateBarChart extends AbstractWidgetRenderer
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RenderWidgetOutput render(RenderWidgetInput input) throws QException
   {
      try
      {
         /*
         // todo - always do this as SQL... if we had database in CI...
         ConnectionManager connectionManager = new ConnectionManager();
         Connection        connection        = connectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());

         String sql = """
            SELECT
               COUNT(*) AS count,
               DATE_FORMAT(create_date, '%m-%Y') AS month
            FROM
               person
            GROUP BY
               2
            ORDER BY
               2
            """;

         List<Map<String, Object>> rows = QueryManager.executeStatementForRows(connection, sql);

         for(Map<String, Object> row : rows)
         {
            labels.add(ValueUtils.getValueAsString(row.get("month")));
            data.add(ValueUtils.getValueAsInteger(row.get("count")));
         }
          */

         List<String> labels = new ArrayList<>();
         List<Number> data   = new ArrayList<>();

         labels.add("Jan. 2022");
         data.add(17);

         labels.add("Feb. 2022");
         data.add(42);

         labels.add("Mar. 2022");
         data.add(47);

         labels.add("Apr. 2022");
         data.add(0);

         labels.add("May 2022");
         data.add(64);

         return (new RenderWidgetOutput(new ChartData("Persons created per Month", null, "Person records", labels, data)));
      }
      catch(Exception e)
      {
         throw (new QException("Error rendering widget", e));
      }
   }

}
