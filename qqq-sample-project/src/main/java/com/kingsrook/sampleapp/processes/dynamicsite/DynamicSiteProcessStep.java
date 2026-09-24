/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.sampleapp.processes.dynamicsite;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.middleware.javalin.routeproviders.ProcessBasedRouterPayload;


/*******************************************************************************
 **
 *******************************************************************************/
public class DynamicSiteProcessStep implements BackendStep
{
   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ProcessBasedRouterPayload processPayload = runBackendStepInput.getProcessPayload(ProcessBasedRouterPayload.class);

      String path = processPayload.getPath();
      processPayload.setResponseHeaders(Map.of("Content-Type", "text/plain; charset=utf-8"));
      processPayload.setResponseString("You requested: " + path + "(at path-param: " + processPayload.getPathParams().get("pagePath") + ")");
      runBackendStepOutput.setProcessPayload(processPayload);
   }

}
