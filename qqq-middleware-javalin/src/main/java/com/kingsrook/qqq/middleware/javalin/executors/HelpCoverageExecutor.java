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


import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageAction;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageInput;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.middleware.javalin.executors.io.HelpCoverageMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.HelpCoverageResponseV1;


/*******************************************************************************
    ** HelpCoverageExecutor contract.
    *******************************************************************************/
public class HelpCoverageExecutor extends AbstractMiddlewareExecutor<HelpCoverageMiddlewareInput, HelpCoverageResponseV1>
{
   /*******************************************************************************
    ** Execute using the authenticated request context.
    *******************************************************************************/
   @Override
   public void execute(HelpCoverageMiddlewareInput input, HelpCoverageResponseV1 output) throws QException
   {
      var coverage = new HelpCoverageAction().execute(new HelpCoverageInput().withKind(input.getKind()).withMissingOnly(input.getMissingOnly()).withParentKey(input.getParentKey()));
      output.setTotals(coverage.getTotals());
      output.setEntries(coverage.getEntries());
   }
}
