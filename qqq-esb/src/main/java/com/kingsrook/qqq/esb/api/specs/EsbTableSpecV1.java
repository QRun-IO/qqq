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

package com.kingsrook.qqq.esb.api.specs;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.esb.api.specs.EsbSpecSupport.AbstractEsbSpecV1;
import com.kingsrook.qqq.esb.api.specs.EsbSpecSupport.EsbInput;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import io.javalin.http.Context;


/*******************************************************************************
 ** GET /esb/table/{table} - ESB of a table
 *******************************************************************************/
public class EsbTableSpecV1 extends AbstractEsbSpecV1
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation()
         .withPath("/esb/table/{table}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(EsbSpecSupport.EsbTag.ESB)
         .withShortSummary("ESB of a table")
         .withLongDescription("The destinations a table publishes its events to, the triggers subscribed to them (on processes the user can access), and what the user may do. Needs READ on the table."));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      List<Parameter> parameters = new ArrayList<>();
      parameters.add(EsbSpecSupport.pathParameter("table", "Table name."));
      return (parameters);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public EsbInput buildInput(Context context) throws Exception
   {
      String table = context.pathParam("table");
      return (new EsbInput().withBodyBuilder(builder -> builder.buildTable(table)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getResponseSchemaName()
   {
      return (EsbUiApiSchemas.TABLE);
   }
}
