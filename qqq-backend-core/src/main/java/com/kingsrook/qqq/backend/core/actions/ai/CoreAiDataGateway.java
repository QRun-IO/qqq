/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** The default AiDataGateway: the core actions, in-process, with the session in
 ** QContext (inputs prepared by AiToolContext, so permissions are enforced).
 *******************************************************************************/
public class CoreAiDataGateway implements AiDataGateway
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> query(QueryInput input) throws QException
   {
      return (new QueryAction().execute(input).getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer count(CountInput input) throws QException
   {
      return (new CountAction().execute(input).getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QRecord get(GetInput input) throws QException
   {
      return (new GetAction().execute(input).getRecord());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<RecordSearchResult> search(RecordSearchInput input) throws QException
   {
      return (new RecordSearchAction().execute(input).getResults());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Object> renderWidget(RenderWidgetInput input) throws QException
   {
      RenderWidgetOutput output = new RenderWidgetAction().execute(input);
      if(output == null || output.getWidgetData() == null)
      {
         return (null);
      }

      try
      {
         return (JsonUtils.toObject(JsonUtils.toJson(output.getWidgetData()), new TypeReference<>() {}));
      }
      catch(IOException e)
      {
         throw (new QException("Error reading widget data", e));
      }
   }
}
