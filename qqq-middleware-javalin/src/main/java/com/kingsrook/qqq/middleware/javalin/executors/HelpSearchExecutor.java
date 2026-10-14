/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors;


import com.kingsrook.qqq.backend.core.actions.help.HelpSearchAction;
import com.kingsrook.qqq.backend.core.actions.help.HelpSearchInput;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.middleware.javalin.executors.io.HelpSearchMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.HelpSearchResponseV1;


/*******************************************************************************
    ** HelpSearchExecutor contract.
    *******************************************************************************/
public class HelpSearchExecutor extends AbstractMiddlewareExecutor<HelpSearchMiddlewareInput, HelpSearchResponseV1>
{
   /*******************************************************************************
    ** Execute using the authenticated request context.
    *******************************************************************************/
   @Override
   public void execute(HelpSearchMiddlewareInput input, HelpSearchResponseV1 output) throws QException
   {
      output.setResults(new HelpSearchAction().execute(new HelpSearchInput().withQuery(input.getQuery()).withLimit(input.getLimit())));
   }
}
