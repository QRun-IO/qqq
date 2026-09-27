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

package com.kingsrook.qqq.api.implementations.savedreports;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.api.model.actions.HttpApiResponse;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessOutputInterface;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import org.eclipse.jetty.http.HttpStatus;


/*******************************************************************************
 ** api process output specifier for the RenderSavedReport process
 *******************************************************************************/
public class RenderSavedReportProcessApiProcessOutput implements ApiProcessOutputInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable getOutputForProcess(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput) throws QException
   {
      //////////////////////////////////////////////////////////////////
      // we don't use output like this - see customizeHttpApiResponse //
      //////////////////////////////////////////////////////////////////
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void customizeHttpApiResponse(HttpApiResponse httpApiResponse, RunProcessInput runProcessInput, RunProcessOutput runProcessOutput) throws QException
   {
      /////////////////////////////////////////////////////////////////////////////////////////////
      // we don't need anyone else to format our response - assume that we've done so ourselves. //
      /////////////////////////////////////////////////////////////////////////////////////////////
      httpApiResponse.setNeedsFormattedAsJson(false);

      /////////////////////////////////////////////
      // set content type based on report format //
      /////////////////////////////////////////////
      ReportFormat reportFormat = ReportFormat.fromString(runProcessOutput.getValueString("reportFormat"));
      httpApiResponse.setContentType(reportFormat.getMimeType());

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // get an input stream from the backend where the report content is stored - send that down to the caller //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      String      storageTableName = runProcessOutput.getValueString("storageTableName");
      String      storageReference = runProcessOutput.getValueString("storageReference");
      httpApiResponse.setInputStream(new StorageAction().getInputStream(new StorageInput(storageTableName).withReference(storageReference)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public HttpStatus.Code getSuccessStatusCode(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput)
   {
      return (HttpStatus.Code.OK);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<Integer, Response> getSpecResponses(String apiName)
   {
      Map<String, Content> contentMap = new LinkedHashMap<>();
      contentMap.put(ReportFormat.JSON.getMimeType(), new Content()
         .withSchema(new Schema()
            .withDescription("JSON Report contents")
            .withExample("""
               [
                  {"id": 1, "name": "James"},
                  {"id": 2, "name": "Jean-Luc"}
               ]
               """)
            .withType(Type.STRING)
            .withFormat("text")));

      contentMap.put(ReportFormat.CSV.getMimeType(), new Content()
         .withSchema(new Schema()
            .withDescription("CSV Report contents")
            .withExample("""
               "id","name"
               1,"James"
               2,"Jean-Luc"
               """)
            .withType(Type.STRING)
            .withFormat("text")));

      contentMap.put(ReportFormat.XLSX.getMimeType(), new Content()
         .withSchema(new Schema()
            .withDescription("Excel Report contents")
            .withType(Type.STRING)
            .withFormat("binary")));

      return Map.of(HttpStatus.Code.OK.getCode(), new Response()
         .withDescription("Report contents in the requested format.")
         .withContent(contentMap));
   }

}
