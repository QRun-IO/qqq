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
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.middleware.javalin.executors.RecordDeveloperModeExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordDeveloperModeInput;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils.AssociatedScriptDetails;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordDeveloperModeResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** Record "developer mode":  a record, with its table's associated scripts
 ** (the script the record references, its revisions, and fields for testing).
 *******************************************************************************/
public class RecordDeveloperModeSpecV1 extends AbstractEndpointSpec<RecordDeveloperModeInput, RecordDeveloperModeResponseV1, RecordDeveloperModeExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/{primaryKey}/developer")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Get a record's associated scripts (developer mode)")
         .withLongDescription("""
            Fetch a single record (with display values), along with each of its table's associated scripts:
            the script type, the script that the record references (if any) with its revisions (newest first),
            and, for scripts that can be tested, the fields to test them with.
            
            Requires permission to read records from the table.  If the record is not found, a 404 is returned."""
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
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RecordDeveloperModeInput buildInput(Context context) throws Exception
   {
      return (new RecordDeveloperModeInput()
         .withTableName(getRequestParam(context, "tableName"))
         .withPrimaryKey(getRequestParam(context, "primaryKey")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(RecordDeveloperModeResponseV1.class.getSimpleName(), new RecordDeveloperModeResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      AssociatedScriptDetails details = new AssociatedScriptDetails();
      details.setAssociatedScript(new AssociatedScript().withFieldName("greetingScriptId").withScriptTypeId(1));
      details.setScriptType(new QRecord()
         .withTableName("scriptType").withRecordLabel("Greeting")
         .withValue("id", 1).withValue("name", "Greeting"));
      details.setScript(new QRecord()
         .withTableName("script").withRecordLabel("Darin Kelkhoff - Greeting")
         .withValue("id", 7).withValue("name", "Darin Kelkhoff - Greeting").withValue("scriptTypeId", 1).withValue("currentScriptRevisionId", 12));
      details.setScriptRevisions(new ArrayList<>(List.of(new QRecord()
         .withTableName("scriptRevision")
         .withValue("id", 12).withValue("scriptId", 7).withValue("sequenceNo", 2).withValue("commitMessage", "Say hello by name"))));
      details.setTestInputFields(new ArrayList<>(List.of(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name"))));
      details.setTestOutputFields(new ArrayList<>(List.of(new QFieldMetaData("message", QFieldType.STRING).withLabel("Message"))));

      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new RecordDeveloperModeResponseV1()
            .withRecord(new QRecord()
               .withRecordLabel("Darin Kelkhoff")
               .withTableName("person")
               .withValue("id", 1).withValue("firstName", "Darin").withValue("greetingScriptId", 7)
               .withDisplayValue("id", "1").withDisplayValue("firstName", "Darin").withDisplayValue("greetingScriptId", "7"))
            .withAssociatedScripts(List.of(details))));

      return new BasicResponse("""
         The record, with its table's associated scripts""",
         RecordDeveloperModeResponseV1.class.getSimpleName(),
         examples
      );
   }

}
