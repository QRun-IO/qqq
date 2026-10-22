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
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlock;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelTone;
import com.kingsrook.qqq.middleware.javalin.executors.RecordDeveloperPanelsExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordDeveloperModeInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordDeveloperPanelsResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** GET /table/{tableName}/{primaryKey}/developer/panels - the panels that
 ** developer panel providers (built-in, modules, qbits) contribute to a
 ** record's developer view.
 *******************************************************************************/
public class RecordDeveloperPanelsSpecV1 extends AbstractEndpointSpec<RecordDeveloperModeInput, RecordDeveloperPanelsResponseV1, RecordDeveloperPanelsExecutor>
{

   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/table/{tableName}/{primaryKey}/developer/panels")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Get a record's developer panels")
         .withLongDescription("""
            Fetch the panels shown on a record's developer view: one per developer panel provider (built in, or
            registered by modules and qbits in the instance's developerPanels supplemental meta-data) that is available
            for this table and record for the current user - for example record security, ESB events, quick search
            indexing, or SQL workbench queries.

            Requires permission to read records from the table, and the record must be visible under its record
            security locks.  If the record is not found, a 404 is returned.  A provider that fails yields a panel
            with an error message rather than failing the request."""
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
            .withIn(In.PATH)
      );
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public RecordDeveloperModeInput buildInput(Context context) throws Exception
   {
      return (new RecordDeveloperModeInput()
         .withTableName(getRequestParam(context, "tableName"))
         .withPrimaryKey(getRequestParam(context, "primaryKey")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(RecordDeveloperPanelsResponseV1.class.getSimpleName(), new RecordDeveloperPanelsResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("Record security", new Example()
         .withValue(new RecordDeveloperPanelsResponseV1()
            .withPanels(List.of(new DeveloperPanel()
               .withId("recordSecurity")
               .withTitle("Record Security")
               .withOrder(150)
               .withStatusLabel("1 lock")
               .withStatusTone(DeveloperPanelTone.SUCCESS)
               .withBlock(DeveloperPanelBlock.table("Locks", List.of("Security key", "Field", "Scope", "Null values", "This record's value", "Your access"))
                  .withRow(List.of("store", "storeId", "READ_AND_WRITE", "DENY", "42", "Your session has key value 42")))))));

      return new BasicResponse("""
         The record's developer panels""",
         RecordDeveloperPanelsResponseV1.class.getSimpleName(),
         examples
      );
   }

}
