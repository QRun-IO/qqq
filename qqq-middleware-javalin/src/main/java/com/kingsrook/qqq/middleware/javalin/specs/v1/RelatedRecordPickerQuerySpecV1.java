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


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.middleware.javalin.executors.RelatedRecordPickerExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.RelatedRecordPickerMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RelatedRecordPickerQueryResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.RelatedRecordPickerSpecUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** POST /table/{tableName}/relatedRecordPicker/{pickerName}/query
 ** (docs/RELATED-RECORD-PICKER.md).
 *******************************************************************************/
public class RelatedRecordPickerQuerySpecV1 extends AbstractEndpointSpec<RelatedRecordPickerMiddlewareInput, RelatedRecordPickerQueryResponseV1, RelatedRecordPickerExecutor>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath(RelatedRecordPickerSpecUtils.PATH_PREFIX + "/query")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Query the records a related-record picker offers")
         .withLongDescription("""
            Lists one page of the picker's target records as the user may read them: the user's filter (the target table's own filter builder or a saved view), ANDed with the picker's filter (resolved from the parent record or form values) and the search term. Needs READ on the target table; table permissions, record security locks and field security locks apply as on the table query route. Reads limit + 1 rows for hasMore (no count). For MULTI_CHILD and LINK with a primaryKey, linkedIds says which returned records the parent already has rows for.""");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (RelatedRecordPickerSpecUtils.defineRequestParameters());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return (RelatedRecordPickerSpecUtils.defineRequestBody(RelatedRecordPickerMiddlewareInput.Operation.QUERY));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RelatedRecordPickerMiddlewareInput buildInput(Context context) throws Exception
   {
      return (RelatedRecordPickerSpecUtils.buildInput(RelatedRecordPickerMiddlewareInput.Operation.QUERY,
         getRequestParam(context, "tableName"), getRequestParam(context, "pickerName"), getRequestBodyAsJsonObject(context)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(RelatedRecordPickerQueryResponseV1.class.getSimpleName(), new RelatedRecordPickerQueryResponseV1().toSchema());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("Query the records a related-record picker offers: result", RelatedRecordPickerQueryResponseV1.class.getSimpleName());
   }
}
