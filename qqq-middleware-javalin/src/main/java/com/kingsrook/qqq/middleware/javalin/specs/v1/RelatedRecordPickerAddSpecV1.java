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
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RelatedRecordPickerAddResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.RelatedRecordPickerSpecUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** POST /table/{tableName}/relatedRecordPicker/{pickerName}/add
 ** (docs/RELATED-RECORD-PICKER.md).
 *******************************************************************************/
public class RelatedRecordPickerAddSpecV1 extends AbstractEndpointSpec<RelatedRecordPickerMiddlewareInput, RelatedRecordPickerAddResponseV1, RelatedRecordPickerExecutor>
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath(RelatedRecordPickerSpecUtils.PATH_PREFIX + "/add")
         .withHttpMethod(HttpMethod.POST)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Add child or join rows for picked records (MULTI_CHILD, LINK)")
         .withLongDescription("""
            Inserts one row per picked target (targetIds) or per reviewed row (records), all or nothing, through the standard insert action: INSERT permission on the child table, record security locks, validation, customizers, automations and audit. Only the picker's review fields are taken from the client; the parent and target keys always come from the server. Every target must be available through the picker's query (else 400). Row errors are a 400 naming each row, and nothing is saved.""");
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
      return (RelatedRecordPickerSpecUtils.defineRequestBody(RelatedRecordPickerMiddlewareInput.Operation.ADD));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public RelatedRecordPickerMiddlewareInput buildInput(Context context) throws Exception
   {
      return (RelatedRecordPickerSpecUtils.buildInput(RelatedRecordPickerMiddlewareInput.Operation.ADD,
         getRequestParam(context, "tableName"), getRequestParam(context, "pickerName"), getRequestBodyAsJsonObject(context)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(RelatedRecordPickerAddResponseV1.class.getSimpleName(), new RelatedRecordPickerAddResponseV1().toSchema());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return new BasicResponse("Add child or join rows for picked records (MULTI_CHILD, LINK): result", RelatedRecordPickerAddResponseV1.class.getSimpleName());
   }
}
