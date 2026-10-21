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
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAiRoute;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAssist;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.BulkLoadValidateExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.BulkLoadValidateInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BulkLoadValidateResponseV1;
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
 ** POST /bulkLoad/{tableName}/validate: a dry run of a bulk load profile over
 ** an uploaded file, through the table's real bulk-insert process, stopping
 ** before anything is stored (assisted bulk load, design spec #1171 section
 ** 3.2).
 *******************************************************************************/
public class BulkLoadValidateSpecV1 extends AbstractEndpointSpec<BulkLoadValidateInput, BulkLoadValidateResponseV1, BulkLoadValidateExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/bulkLoad/{tableName}/validate")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.BULK_LOAD)
         .withShortSummary("Dry-run a bulk load profile over an uploaded file")
         .withLongDescription("""
            Run the table's bulk-insert process over an uploaded file with the given v1 bulk load profile - the same mapping, value \
            mapping and validation a real load uses - and stop at its review step, so nothing is stored.  SAMPLE validates the first \
            100 rows; FULL the whole file.  Returns the standard process summary lines, counts, and error groups with row numbers.  \
            Values of classified fields are left out unless the request's `X-QQQ-AI-Route` header is `hipaa`.  The upload must be \
            the session user's.  Requires the table's bulk-insert process permission.""");
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
      properties.put("profile", new Schema().withType(Type.OBJECT).withDescription("""
         A v1 bulk load profile: version, layout (FLAT), hasHeaderRow, and fieldList entries of fieldName, columnIndex, headerName, \
         defaultValue, doValueMapping, valueMappings and clearIfEmpty."""));
      properties.put("mode", new Schema().withType(Type.STRING).withEnumValues(List.of("SAMPLE", "FULL")).withDescription("SAMPLE (default, the first 100 rows) or FULL."));

      Map<String, Content> content = new LinkedHashMap<>();
      content.put(ContentType.APPLICATION_JSON.getMimeType(), new Content().withSchema(new Schema().withType(Type.OBJECT).withProperties(properties)));
      return new RequestBody().withContent(content);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BulkLoadValidateInput buildInput(Context context) throws Exception
   {
      JSONObject body = getRequestBodyAsJsonObject(context);
      if(body == null || !body.has("profile") || body.isNull("profile"))
      {
         throw (new QBadRequestException("A JSON body with uploadRef and profile is required."));
      }

      String mode = body.optString("mode", BulkLoadAssist.Mode.SAMPLE.name()).toUpperCase();
      if(!mode.equals(BulkLoadAssist.Mode.SAMPLE.name()) && !mode.equals(BulkLoadAssist.Mode.FULL.name()))
      {
         throw (new QBadRequestException("mode must be SAMPLE or FULL."));
      }

      BulkLoadProfile profile;
      try
      {
         profile = JsonUtils.toObject(body.getJSONObject("profile").toString(), BulkLoadProfile.class);
      }
      catch(Exception e)
      {
         throw (new QBadRequestException("profile is not a valid bulk load profile: " + e.getMessage()));
      }

      return (new BulkLoadValidateInput()
         .withTableName(getRequestParam(context, "tableName"))
         .withUploadRef(body.optString("uploadRef", null))
         .withProfile(profile)
         .withMode(mode)
         .withRoute(BulkLoadAiRoute.fromHeader(context.header(BulkLoadAiRoute.HEADER_NAME))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(BulkLoadValidateResponseV1.class.getSimpleName(), new BulkLoadValidateResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("dryRun", new Example().withValue(new BulkLoadValidateResponseV1().withMode("SAMPLE").withProcessName("person.bulkInsert")
         .withRecordCount(3).withOkCount(2).withErrorCount(1)));

      return new BasicResponse("""
         The dry run's summary and error groups""",
         BulkLoadValidateResponseV1.class.getSimpleName(),
         examples
      );
   }

}
