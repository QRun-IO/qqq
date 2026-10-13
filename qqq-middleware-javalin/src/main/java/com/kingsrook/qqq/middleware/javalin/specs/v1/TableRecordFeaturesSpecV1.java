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
import com.kingsrook.qqq.middleware.javalin.executors.TableRecordFeaturesExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableRecordFeaturesInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordFeatureSummaryV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableRecordFeaturesSpecV1 extends AbstractEndpointSpec<TableRecordFeaturesInput, RecordFeatureSummaryV1, TableRecordFeaturesExecutor>
{



   /*******************************************************************************
    **
    *******************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/table/{tableName}/{primaryKey}/features").withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES).withShortSummary("Get record collaboration features")
         .withLongDescription("Features and capabilities for a visible record, with optional summary provider contributions."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(new Parameter().withName("tableName").withIn(In.PATH).withRequired(true).withSchema(new Schema().withType(Type.STRING)),
         new Parameter().withName("primaryKey").withIn(In.PATH).withRequired(true).withSchema(new Schema().withType(Type.STRING))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public TableRecordFeaturesInput buildInput(Context context)
   {
      return (new TableRecordFeaturesInput().withTableName(getRequestParam(context, "tableName")).withPrimaryKey(getRequestParam(context, "primaryKey")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Map<String, Schema> defineComponentSchemas()
   {
      return (Map.of("RecordFeatureSummaryV1", new RecordFeatureSummaryV1().toSchema()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Record collaboration summary", "RecordFeatureSummaryV1", Map.of()));
   }

}
