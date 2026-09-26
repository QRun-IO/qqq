/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
