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
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAiRoute;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.executors.BulkLoadUploadProfileExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.BulkLoadUploadProfileInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BulkLoadUploadProfileResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** GET /bulkLoad/uploads/{uploadRef}/profile: the header guess and column
 ** profiles of an uploaded file, for a target table (assisted bulk load,
 ** design spec #1171 section 3.2).
 *******************************************************************************/
public class BulkLoadUploadProfileSpecV1 extends AbstractEndpointSpec<BulkLoadUploadProfileInput, BulkLoadUploadProfileResponseV1, BulkLoadUploadProfileExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/bulkLoad/uploads/{uploadRef}/profile")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.BULK_LOAD)
         .withShortSummary("Profile an uploaded file for a bulk load")
         .withLongDescription("""
            Read an uploaded file (by its upload ref, which must be the session user's) and describe it for a bulk load into a table: \
            whether row 1 is a header row, and each column's inferred type, percent blank, distinct count, maximum length and pattern \
            class.  Sample values are included only for columns mapped to an unclassified (or PUBLIC) field, or for every column when \
            the request's `X-QQQ-AI-Route` header is `hipaa`; unmapped columns never get samples otherwise.  Requires the table's \
            bulk-insert process permission.  Nothing is stored.""");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return List.of(
         new Parameter()
            .withName("uploadRef")
            .withDescription("The opaque ref of an upload the session user owns.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withIn(In.PATH),
         new Parameter()
            .withName("tableName")
            .withDescription("The table the file would be loaded into.")
            .withRequired(true)
            .withSchema(new Schema().withType(Type.STRING))
            .withExample("person")
            .withIn(In.QUERY),
         new Parameter()
            .withName("sampleCount")
            .withDescription("Sample values per column, where allowed (0 to 20; default 5).")
            .withRequired(false)
            .withSchema(new Schema().withType(Type.INTEGER))
            .withIn(In.QUERY)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BulkLoadUploadProfileInput buildInput(Context context) throws Exception
   {
      return (new BulkLoadUploadProfileInput()
         .withUploadRef(getRequestParam(context, "uploadRef"))
         .withTableName(getRequestParam(context, "tableName"))
         .withSampleCount(ValueUtils.getValueAsInteger(getRequestParam(context, "sampleCount")))
         .withRoute(BulkLoadAiRoute.fromHeader(context.header(BulkLoadAiRoute.HEADER_NAME))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(BulkLoadUploadProfileResponseV1.class.getSimpleName(), new BulkLoadUploadProfileResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("profile", new Example().withValue(new BulkLoadUploadProfileResponseV1().withUploadRef("u_4f2c").withFileName("people.csv")
         .withHasHeaderRow(true).withRowsProfiled(250)));

      return new BasicResponse("""
         The upload's header guess and column profiles""",
         BulkLoadUploadProfileResponseV1.class.getSimpleName(),
         examples
      );
   }

}
