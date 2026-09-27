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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptOutput;
import com.kingsrook.qqq.middleware.javalin.executors.RecordAssociatedScriptStoreExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordAssociatedScriptStoreInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordAssociatedScriptStoreResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import org.json.JSONObject;


/*******************************************************************************
 ** Store a new revision of a record's associated script (developer mode).
 *******************************************************************************/
public class RecordAssociatedScriptStoreSpecV1 extends AbstractEndpointSpec<RecordAssociatedScriptStoreInput, RecordAssociatedScriptStoreResponseV1, RecordAssociatedScriptStoreExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/{primaryKey}/developer/associatedScript/{fieldName}")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Store a new revision of a record's associated script (developer mode)")
         .withLongDescription("""
            Save new contents for the script that a record references in one of its table's associated script
            fields, as a new revision of that script.  If the record does not yet reference a script, one is
            inserted (and the record is updated to reference it).
            
            Requires permission to edit records in the table."""
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
            .withDescription("Name of the associated script field (in the table) whose script is being stored.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("greetingScriptId")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return new RequestBody()
         .withContent(ContentType.APPLICATION_JSON.getMimeType(), new Content()
            .withSchema(new Schema()
               .withType(Type.OBJECT)
               .withProperty("contents", new Schema()
                  .withType(Type.STRING)
                  .withDescription("The script's new contents (its code).")
                  .withExample("return (\"Hello, \" + input.getName());"))
               .withProperty("commitMessage", new Schema()
                  .withType(Type.STRING)
                  .withDescription("A message describing the change.  If not given, a default message is used.")
                  .withExample("Say hello by name"))
            )
         );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RecordAssociatedScriptStoreInput buildInput(Context context) throws Exception
   {
      RecordAssociatedScriptStoreInput input = new RecordAssociatedScriptStoreInput()
         .withTableName(getRequestParam(context, "tableName"))
         .withPrimaryKey(getRequestParam(context, "primaryKey"))
         .withFieldName(getRequestParam(context, "fieldName"));

      JSONObject requestBody = getRequestBodyAsJsonObject(context);
      if(requestBody != null)
      {
         input.setContents(requestBody.optString("contents", null));
         input.setCommitMessage(requestBody.optString("commitMessage", null));
      }

      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(RecordAssociatedScriptStoreResponseV1.class.getSimpleName(), new RecordAssociatedScriptStoreResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      StoreAssociatedScriptOutput storeAssociatedScriptOutput = new StoreAssociatedScriptOutput();
      storeAssociatedScriptOutput.setScriptId(7);
      storeAssociatedScriptOutput.setScriptName("Darin Kelkhoff - Greeting");
      storeAssociatedScriptOutput.setScriptRevisionId(13);
      storeAssociatedScriptOutput.setScriptRevisionSequenceNo(3);

      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("TODO", new Example()
         .withValue(new RecordAssociatedScriptStoreResponseV1().withStoreAssociatedScriptOutput(storeAssociatedScriptOutput)));

      return new BasicResponse("""
         The script, and the revision that was stored""",
         RecordAssociatedScriptStoreResponseV1.class.getSimpleName(),
         examples
      );
   }

}
