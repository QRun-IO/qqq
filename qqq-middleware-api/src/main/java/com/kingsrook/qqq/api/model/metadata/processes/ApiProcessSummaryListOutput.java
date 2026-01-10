/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata.processes;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryFilterLink;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryRecordLink;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.jetty.http.HttpStatus;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiProcessSummaryListOutput implements ApiProcessOutputInterface
{
   private static final QLogger LOG = QLogger.getLogger(ApiProcessSummaryListOutput.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public HttpStatus.Code getSuccessStatusCode(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput)
   {
      @SuppressWarnings("unchecked")
      List<ProcessSummaryLineInterface> processSummaryLineInterfaces = (List<ProcessSummaryLineInterface>) runProcessOutput.getValues().get("processResults");
      if(processSummaryLineInterfaces.isEmpty())
      {
         //////////////////////////////////////////////////////////////////////////
         // if there are no summary lines, all we can return is 204 - no content //
         //////////////////////////////////////////////////////////////////////////
         return (HttpStatus.Code.NO_CONTENT);
      }
      else
      {
         ///////////////////////////////////////////////////////////////////////////////////
         // else if there are summary lines, we'll represent them as a 207 - multi-status //
         ///////////////////////////////////////////////////////////////////////////////////
         return (HttpStatus.Code.MULTI_STATUS);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<Integer, Response> getSpecResponses(String apiName)
   {
      Map<String, Schema> propertiesFor207Object = new LinkedHashMap<>();
      propertiesFor207Object.put("id", new Schema().withType(Type.INTEGER).withDescription("Id of the record whose status is being described in the object"));
      propertiesFor207Object.put("statusCode", new Schema().withType(Type.INTEGER).withDescription("HTTP Status code indicating the success or failure of the process on this record"));
      propertiesFor207Object.put("statusText", new Schema().withType(Type.STRING).withDescription("HTTP Status text indicating the success or failure of the process on this record"));
      propertiesFor207Object.put("message", new Schema().withType(Type.STRING).withDescription("Additional descriptive information about the result of the process on this record."));

      List<Object> exampleFor207Object = ListBuilder.of(MapBuilder.of(LinkedHashMap::new)
            .with("id", 42)
            .with("statusCode", io.javalin.http.HttpStatus.OK.getCode())
            .with("statusText", io.javalin.http.HttpStatus.OK.getMessage())
            .with("message", "record was processed successfully.")
            .build(),
         MapBuilder.of(LinkedHashMap::new)
            .with("id", 47)
            .with("statusCode", io.javalin.http.HttpStatus.BAD_REQUEST.getCode())
            .with("statusText", io.javalin.http.HttpStatus.BAD_REQUEST.getMessage())
            .with("message", "error executing process on record.")
            .build());

      return MapBuilder.of(
         HttpStatus.Code.MULTI_STATUS.getCode(), new Response()
            .withDescription("For each input record, an object describing its status may be returned.")
            .withContent(MapBuilder.of(ContentType.JSON, new Content()
               .withSchema(new Schema()
                  .withType(Type.ARRAY)
                  .withItems(new Schema()
                     .withType(Type.OBJECT)
                     .withProperties(propertiesFor207Object))
                  .withExample(exampleFor207Object)
               )
            )),

         HttpStatus.Code.NO_CONTENT.getCode(), new Response()
            .withDescription("If no records were found, there may be no content in the response.")
      );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable getOutputForProcess(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput) throws QException
   {
      try
      {
         ArrayList<Serializable> apiOutput = new ArrayList<>();

         @SuppressWarnings("unchecked")
         List<ProcessSummaryLineInterface> processSummaryLineInterfaces = (List<ProcessSummaryLineInterface>) runProcessOutput.getValues().get("processResults");
         for(ProcessSummaryLineInterface processSummaryLineInterface : processSummaryLineInterfaces)
         {
            if(processSummaryLineInterface instanceof ProcessSummaryLine processSummaryLine)
            {
               processSummaryLine.setCount(1);
               processSummaryLine.pickMessage(true);

               List<Serializable> primaryKeys = processSummaryLine.getPrimaryKeys();
               if(CollectionUtils.nullSafeHasContents(primaryKeys))
               {
                  for(Serializable primaryKey : primaryKeys)
                  {
                     HashMap<String, Serializable> map = toMap(processSummaryLine);
                     map.put("id", primaryKey);
                     apiOutput.add(map);
                  }
               }
               else
               {
                  apiOutput.add(toMap(processSummaryLine));
               }
            }
            else if(processSummaryLineInterface instanceof ProcessSummaryRecordLink processSummaryRecordLink)
            {
               apiOutput.add(toMap(processSummaryRecordLink));
            }
            else if(processSummaryLineInterface instanceof ProcessSummaryFilterLink processSummaryFilterLink)
            {
               apiOutput.add(toMap(processSummaryFilterLink));
            }
            else
            {
               throw new NotImplementedException("Unknown ProcessSummaryLineInterface handling");
            }
         }

         return (apiOutput);
      }
      catch(Exception e)
      {
         LOG.warn("Error getting api output for process", e);
         throw (new QException("Error generating process output", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HashMap<String, Serializable> toMap(ProcessSummaryFilterLink processSummaryFilterLink)
   {
      HashMap<String, Serializable> map = initResultMapForProcessSummaryLine(processSummaryFilterLink);

      String messagePrefix = getResultMapMessagePrefix(processSummaryFilterLink);
      map.put("message", messagePrefix + processSummaryFilterLink.getFullText());

      return (map);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HashMap<String, Serializable> toMap(ProcessSummaryRecordLink processSummaryRecordLink)
   {
      HashMap<String, Serializable> map = initResultMapForProcessSummaryLine(processSummaryRecordLink);

      String messagePrefix = getResultMapMessagePrefix(processSummaryRecordLink);
      map.put("message", messagePrefix + processSummaryRecordLink.getFullText());

      return (map);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static HashMap<String, Serializable> toMap(ProcessSummaryLine processSummaryLine)
   {
      HashMap<String, Serializable> map = initResultMapForProcessSummaryLine(processSummaryLine);

      String messagePrefix = getResultMapMessagePrefix(processSummaryLine);
      map.put("message", messagePrefix + processSummaryLine.getMessage());

      return (map);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String getResultMapMessagePrefix(ProcessSummaryLineInterface processSummaryLine)
   {
      String messagePrefix = switch(processSummaryLine.getStatus())
      {
         case OK, INFO, ERROR -> "";
         case WARNING -> "Warning: ";
      };
      return messagePrefix;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static HashMap<String, Serializable> initResultMapForProcessSummaryLine(ProcessSummaryLineInterface processSummaryLine)
   {
      HashMap<String, Serializable> map = new HashMap<>();

      HttpStatus.Code code = switch(processSummaryLine.getStatus())
      {
         case OK, WARNING, INFO -> HttpStatus.Code.OK;
         case ERROR -> HttpStatus.Code.INTERNAL_SERVER_ERROR;
      };

      map.put("statusCode", code.getCode());
      map.put("statusText", code.getMessage());
      return map;
   }

}
