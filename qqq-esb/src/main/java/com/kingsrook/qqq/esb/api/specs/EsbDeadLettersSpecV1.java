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
 ** GET /esb/deadLetters/{trigger} - Dead letters of a trigger
 *******************************************************************************/
public class EsbDeadLettersSpecV1 extends AbstractEsbSpecV1
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation()
         .withPath("/esb/deadLetters/{trigger}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(EsbSpecSupport.EsbTag.ESB)
         .withShortSummary("Dead letters of a trigger")
         .withLongDescription("A page of the messages on a trigger dead-letter destination, oldest first. Needs the operate permission."));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      List<Parameter> parameters = new ArrayList<>();
      parameters.add(EsbSpecSupport.pathParameter("trigger", "Trigger name."));
      parameters.addAll(EsbSpecSupport.pagingParameters());
      return (parameters);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public EsbInput buildInput(Context context) throws Exception
   {
      String  trigger = context.pathParam("trigger");
      Integer offset  = EsbSpecSupport.getIntegerParam(context, "offset");
      Integer limit   = EsbSpecSupport.getIntegerParam(context, "limit");
      return (new EsbInput().withBodyBuilder(builder -> builder.browseDeadLetters(trigger, offset, limit)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getResponseSchemaName()
   {
      return (EsbUiApiSchemas.MESSAGE_PAGE);
   }
}
