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
import com.kingsrook.qqq.middleware.javalin.executors.BulkLoadSuggestMappingExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.BulkLoadSuggestMappingInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BulkLoadSuggestMappingResponseV1;
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
 ** POST /bulkLoad/{tableName}/suggestMapping: core's suggested bulk load
 ** profile for an uploaded file's header row (assisted bulk load, design spec
 ** #1171 section 3.2).
 *******************************************************************************/
public class BulkLoadSuggestMappingSpecV1 extends AbstractEndpointSpec<BulkLoadSuggestMappingInput, BulkLoadSuggestMappingResponseV1, BulkLoadSuggestMappingExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/bulkLoad/{tableName}/suggestMapping")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.BULK_LOAD)
         .withShortSummary("Suggest a bulk load mapping for an uploaded file")
         .withLongDescription("""
            Run the bulk load mapping suggester over an uploaded file's header row and return a v1 bulk load profile, the file \
            columns it leaves unmapped, the required fields it does not fill, and the mapped fields with a possible value source \
            (whose file values may need value mapping).  The upload must be the session user's.  Requires the table's bulk-insert \
            process permission.  Nothing is stored.""");
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
            .withDescription("The table the file would be loaded into.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      Map<String, Schema> properties = new LinkedHashMap<>();
      properties.put("uploadRef", new Schema().withType(Type.STRING).withDescription("The opaque ref of an upload the session user owns."));
      properties.put("isBulkEdit", new Schema().withType(Type.BOOLEAN).withDescription("Suggest a bulk edit profile (default false: bulk insert)."));

      Map<String, Content> content = new LinkedHashMap<>();
      content.put(ContentType.APPLICATION_JSON.getMimeType(), new Content().withSchema(new Schema().withType(Type.OBJECT).withProperties(properties)));
      return new RequestBody().withContent(content);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BulkLoadSuggestMappingInput buildInput(Context context) throws Exception
   {
      JSONObject body = getRequestBodyAsJsonObject(context);
      return (new BulkLoadSuggestMappingInput()
         .withTableName(getRequestParam(context, "tableName"))
         .withUploadRef(body == null ? null : body.optString("uploadRef", null))
         .withIsBulkEdit(body != null && body.optBoolean("isBulkEdit", false)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(BulkLoadSuggestMappingResponseV1.class.getSimpleName(), new BulkLoadSuggestMappingResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      ////////////////////////////////////////////////////////////////
      // ordered, so the published document is stable between runs //
      ////////////////////////////////////////////////////////////////
      Map<String, Object> profile = new LinkedHashMap<>();
      profile.put("version", "v1");
      profile.put("layout", "FLAT");
      profile.put("hasHeaderRow", true);
      examples.put("suggestion", new Example().withValue(new BulkLoadSuggestMappingResponseV1().withProfile(profile)));

      return new BasicResponse("""
         The suggested profile and what it leaves unmapped""",
         BulkLoadSuggestMappingResponseV1.class.getSimpleName(),
         examples
      );
   }

}
