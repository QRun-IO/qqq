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

package com.kingsrook.qqq.middleware.javalin.health;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;


/*******************************************************************************
 ** GET /health/summary.
 *******************************************************************************/
public class HealthSummarySpecV1 extends HealthSpecSupport
{
   /*******************************************************************************
    ** Public contract of this secured operation.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/health/summary").withHttpMethod(HttpMethod.GET)
         .withTag(() -> "Health").withShortSummary("Health summary")
         .withLongDescription("Requires health.read permission. Returns redacted health state."));
   }



   /*******************************************************************************
    ** Parameters declared in the extension contract.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of());
   }



   /*******************************************************************************
    ** Versioned response schema.
    *******************************************************************************/
   @Override
   protected String getResponseSchemaName()
   {
      return ("HealthSummaryV1");
   }



   /*******************************************************************************
    ** Check permissions before looking up names or executing a probe.
    *******************************************************************************/
   @Override
   protected Object handle(JsonInput input) throws Exception
   {
      requirePermission(false);
      return (summary());
   }
}
