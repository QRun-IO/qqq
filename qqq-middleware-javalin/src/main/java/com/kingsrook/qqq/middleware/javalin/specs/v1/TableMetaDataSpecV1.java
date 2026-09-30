/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.executors.TableMetaDataExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableMetaDataInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableMetaDataResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.MetaDataSpecUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableMetaDataSpecV1 extends AbstractEndpointSpec<TableMetaDataInput, TableMetaDataResponseV1, TableMetaDataExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/metaData/table/{tableName}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Get table metaData")
         .withLongDescription("""
            Load the full metadata for a single table, including all fields, which a frontend
            needs to display to users."""
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
            .withDescription("Name of the table to load.")
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
   public TableMetaDataInput buildInput(Context context) throws Exception
   {
      TableMetaDataInput input = new TableMetaDataInput();
      input.setTableName(getRequestParam(context, "tableName"));
      return (input);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(TableMetaDataResponseV1.class.getSimpleName(), new TableMetaDataResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();

      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // double-wrap the context here, so the instance will exist when the system-user-session is created //
      // to avoid warnings out of system-user-session about there not being an instance in context.       //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      QInstance exampleInstance = MetaDataSpecUtils.getExampleInstance();
      QContext.withTemporaryContext(new CapturedContext(exampleInstance, null), () ->
      {
         QContext.withTemporaryContext(new CapturedContext(exampleInstance, new QSystemUserSession()), () ->
         {
            String                   tableName             = "person";
            AbstractTableActionInput actionInput           = new com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput().withTableName(tableName);
            QFrontendTableMetaData   frontendTableMetaData = new QFrontendTableMetaData(actionInput, exampleInstance.getBackendForTable(tableName), exampleInstance.getTable(tableName), true, true);

            try
            {
               examples.put("Example", new Example()
                  .withValue(new TableMetaDataResponseV1().withTableMetaData(frontendTableMetaData)));
            }
            catch(Exception e)
            {
               examples.put("Example", new Example().withValue("Error building example: " + e.getMessage()));
            }
         });
      });

      return new BasicResponse("""
         The full table metadata""",
         TableMetaDataResponseV1.class.getSimpleName(),
         examples
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleOutput(Context context, TableMetaDataResponseV1 output) throws Exception
   {
      context.result(JsonUtils.toJson(output.getTableMetaData()));
   }

}
