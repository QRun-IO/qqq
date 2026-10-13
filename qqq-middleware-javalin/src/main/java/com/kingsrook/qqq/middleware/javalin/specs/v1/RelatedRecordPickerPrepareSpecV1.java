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
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RelatedRecordPickerPrepareResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.RelatedRecordPickerSpecUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** POST /table/{tableName}/relatedRecordPicker/{pickerName}/prepare
 ** (docs/RELATED-RECORD-PICKER.md).
 *******************************************************************************/
public class RelatedRecordPickerPrepareSpecV1 extends AbstractEndpointSpec<RelatedRecordPickerMiddlewareInput, RelatedRecordPickerPrepareResponseV1, RelatedRecordPickerExecutor>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath(RelatedRecordPickerSpecUtils.PATH_PREFIX + "/prepare")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Prepare child rows for picked records (MULTI_CHILD, LINK)")
         .withLongDescription("""
            Builds, without saving, one child row per picked target: the picker's child field defaults, values copied from the target, and the parent and target keys. The client shows the reviewFieldNames of each row for the user to fill, then calls add. Needs INSERT on the child table; every target must be available through the picker's query (else 400).""");
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
      return (RelatedRecordPickerSpecUtils.defineRequestBody(RelatedRecordPickerMiddlewareInput.Operation.PREPARE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RelatedRecordPickerMiddlewareInput buildInput(Context context) throws Exception
   {
      return (RelatedRecordPickerSpecUtils.buildInput(RelatedRecordPickerMiddlewareInput.Operation.PREPARE,
         getRequestParam(context, "tableName"), getRequestParam(context, "pickerName"), getRequestBodyAsJsonObject(context)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(RelatedRecordPickerPrepareResponseV1.class.getSimpleName(), new RelatedRecordPickerPrepareResponseV1().toSchema());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("Prepare child rows for picked records (MULTI_CHILD, LINK): result", RelatedRecordPickerPrepareResponseV1.class.getSimpleName());
   }
}
