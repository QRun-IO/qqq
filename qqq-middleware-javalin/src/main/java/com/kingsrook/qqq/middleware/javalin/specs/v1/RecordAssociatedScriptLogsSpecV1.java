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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.middleware.javalin.executors.RecordAssociatedScriptLogsExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordAssociatedScriptLogsInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordAssociatedScriptLogsResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** Get the logs of a revision of a record's associated script (developer mode).
 *******************************************************************************/
public class RecordAssociatedScriptLogsSpecV1 extends AbstractEndpointSpec<RecordAssociatedScriptLogsInput, RecordAssociatedScriptLogsResponseV1, RecordAssociatedScriptLogsExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/{primaryKey}/developer/associatedScript/{fieldName}/{scriptRevisionId}/logs")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Get the logs of a record's associated script revision (developer mode)")
         .withLongDescription("""
            Fetch the logs (up to 100, newest first) of runs of a script revision, each with its log lines.
            
            Requires permission to read records from the table, and the record must exist (else a 404 is returned)."""
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(
         new Parameter()
            .withName("tableName")
            .withDescription("Name of the table that the record is in.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH),
         new Parameter()
            .withName("primaryKey")
            .withDescription("Primary key value of the record.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("42")
            .withIn(In.PATH),
         new Parameter()
            .withName("fieldName")
            .withDescription("Name of the associated script field (in the table) whose script the revision is of.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("greetingScriptId")
            .withIn(In.PATH),
         new Parameter()
            .withName("scriptRevisionId")
            .withDescription("Id of the script revision to get logs for.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("12")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RecordAssociatedScriptLogsInput buildInput(Context context) throws Exception
   {
      return (new RecordAssociatedScriptLogsInput()
         .withTableName(getRequestParam(context, "tableName"))
         .withPrimaryKey(getRequestParam(context, "primaryKey"))
         .withFieldName(getRequestParam(context, "fieldName"))
         .withScriptRevisionId(getRequestParam(context, "scriptRevisionId")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(RecordAssociatedScriptLogsResponseV1.class.getSimpleName(), new RecordAssociatedScriptLogsResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      String  startTimestamp = "2024-10-21T14:30:00Z";
      QRecord scriptLog      = new QRecord()
         .withTableName("scriptLog")
         .withValue("id", 101).withValue("scriptRevisionId", 12).withValue("startTimestamp", startTimestamp).withValue("hadError", false)
         .withValue("scriptLogLine", new ArrayList<>(List.of(new QRecord()
            .withTableName("scriptLogLine")
            .withValue("id", 1001).withValue("scriptLogId", 101).withValue("timestamp", startTimestamp).withValue("text", "Hello, Darin"))));

      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new RecordAssociatedScriptLogsResponseV1().withScriptLogRecords(List.of(scriptLog))));

      return new BasicResponse("""
         The script revision's logs""",
         RecordAssociatedScriptLogsResponseV1.class.getSimpleName(),
         examples
      );
   }

}
