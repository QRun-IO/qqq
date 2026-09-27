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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordAssociatedScriptLogsOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.OutputRecord;


/*******************************************************************************
 ** Response for the route that gets the logs of a record's associated script
 ** revision.
 *******************************************************************************/
public class RecordAssociatedScriptLogsResponseV1 implements RecordAssociatedScriptLogsOutputInterface, ToSchema
{
   @OpenAPIDescription("The script revision's logs (scriptLog records, up to 100), newest (highest id) first.  Each record's values include `scriptLogLine`:  the log's lines, as a list of records (each with its own `values`, e.g., `text`).")
   @OpenAPIListItems(value = OutputRecord.class, useRef = true)
   private List<OutputRecord> scriptLogRecords;



   /*******************************************************************************
    ** Setter for scriptLogRecords
    *******************************************************************************/
   @Override
   public void setScriptLogRecords(List<QRecord> scriptLogRecords)
   {
      this.scriptLogRecords = scriptLogRecords == null ? null : scriptLogRecords.stream().map(record -> new OutputRecord(record, true)).toList();
   }



   /*******************************************************************************
    ** Fluent setter for scriptLogRecords
    *******************************************************************************/
   public RecordAssociatedScriptLogsResponseV1 withScriptLogRecords(List<QRecord> scriptLogRecords)
   {
      setScriptLogRecords(scriptLogRecords);
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptLogRecords - an empty list is published as such.
    *******************************************************************************/
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public List<OutputRecord> getScriptLogRecords()
   {
      return (this.scriptLogRecords);
   }

}
