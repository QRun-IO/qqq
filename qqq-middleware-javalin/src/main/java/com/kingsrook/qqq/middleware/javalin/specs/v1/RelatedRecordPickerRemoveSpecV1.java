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
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RelatedRecordPickerRemoveResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.RelatedRecordPickerSpecUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** POST /table/{tableName}/relatedRecordPicker/{pickerName}/remove
 ** (docs/RELATED-RECORD-PICKER.md).
 *******************************************************************************/
public class RelatedRecordPickerRemoveSpecV1 extends AbstractEndpointSpec<RelatedRecordPickerMiddlewareInput, RelatedRecordPickerRemoveResponseV1, RelatedRecordPickerExecutor>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath(RelatedRecordPickerSpecUtils.PATH_PREFIX + "/remove")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Unlink picked records (LINK)")
         .withLongDescription("""
            Deletes the parent's join rows for the given targets through the standard delete action (DELETE permission on the join table, record security locks, audit). Only for LINK pickers (else 400); child rows of a MULTI_CHILD picker are deleted with the table delete route.""");
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
      return (RelatedRecordPickerSpecUtils.defineRequestBody(RelatedRecordPickerMiddlewareInput.Operation.REMOVE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RelatedRecordPickerMiddlewareInput buildInput(Context context) throws Exception
   {
      return (RelatedRecordPickerSpecUtils.buildInput(RelatedRecordPickerMiddlewareInput.Operation.REMOVE,
         getRequestParam(context, "tableName"), getRequestParam(context, "pickerName"), getRequestBodyAsJsonObject(context)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(RelatedRecordPickerRemoveResponseV1.class.getSimpleName(), new RelatedRecordPickerRemoveResponseV1().toSchema());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("Unlink picked records (LINK): result", RelatedRecordPickerRemoveResponseV1.class.getSimpleName());
   }
}
