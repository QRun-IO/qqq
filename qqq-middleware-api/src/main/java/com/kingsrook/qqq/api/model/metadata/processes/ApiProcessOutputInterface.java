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

package com.kingsrook.qqq.api.model.metadata.processes;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.api.model.actions.HttpApiResponse;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.openapi.model.Response;
import org.eclipse.jetty.http.HttpStatus;


/*******************************************************************************
 **
 *******************************************************************************/
public interface ApiProcessOutputInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   Serializable getOutputForProcess(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput) throws QException;

   /*******************************************************************************
    **
    *******************************************************************************/
   default HttpStatus.Code getSuccessStatusCode(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput)
   {
      return (HttpStatus.Code.NO_CONTENT);
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default Map<Integer, Response> getSpecResponses(String apiName)
   {
      return (MapBuilder.of(
         HttpStatus.Code.NO_CONTENT.getCode(), new Response()
            .withDescription("Process has been successfully executed.")
      ));
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   default void customizeHttpApiResponse(HttpApiResponse httpApiResponse, RunProcessInput runProcessInput, RunProcessOutput runProcessOutput) throws QException
   {

   }

}
