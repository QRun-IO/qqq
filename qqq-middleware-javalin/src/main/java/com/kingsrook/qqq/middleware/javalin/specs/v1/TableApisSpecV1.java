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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.AbstractJsonEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** GET /table/{tableName}/apis: the application (model) APIs that expose a
 ** table, with their versions and the path of the table's OpenAPI document in
 ** each, for the developer views.  Replaces the unversioned /apis.json and
 ** {api}/versions.json for v1 clients.  The list comes from every
 ** TableApisProviderInterface on the classpath (qqq-middleware-api provides
 ** one); with none it is empty.
 *******************************************************************************/
public class TableApisSpecV1 extends AbstractJsonEndpointSpec
{
   private static final String RESPONSE_SCHEMA = "TableApisResponseV1";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation()
         .withPath("/table/{tableName}/apis")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("List the APIs that expose a table")
         .withLongDescription("""
            The application APIs (served by the API middleware beside this one) that expose the \
            table, with each API's versions and the root-relative path of the table's OpenAPI \
            document in each version.  Empty when the application serves no APIs.  The caller must \
            be allowed to read the table."""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(new Parameter().withName("tableName").withIn(In.PATH).withRequired(true)
         .withDescription("Name of the table.")
         .withSchema(new Schema().withType(Type.STRING))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getResponseSchemaName()
   {
      return (RESPONSE_SCHEMA);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      Map<String, Schema> versionProperties = new LinkedHashMap<>();
      versionProperties.put("version", new Schema().withType(Type.STRING).withDescription("The version, as it appears in the API's URLs."));
      versionProperties.put("semanticVersion", new Schema().withType(Type.STRING).withNullable(true).withDescription("Its semantic version, when the API declares one."));
      versionProperties.put("tableSpecPath", new Schema().withType(Type.STRING).withDescription("Root-relative path of the table's OpenAPI document (JSON) in this version."));

      Map<String, Schema> apiProperties = new LinkedHashMap<>();
      apiProperties.put("name", new Schema().withType(Type.STRING).withDescription("The API's name (a stable key)."));
      apiProperties.put("label", new Schema().withType(Type.STRING).withDescription("Label to show users."));
      apiProperties.put("path", new Schema().withType(Type.STRING).withDescription("Root-relative base path of the API, e.g. `/api/`."));
      apiProperties.put("currentVersion", new Schema().withType(Type.STRING).withNullable(true).withDescription("The version the API calls current."));
      apiProperties.put("versions", new Schema().withType(Type.ARRAY).withItems(new Schema().withRefToSchema("TableApiVersionV1")).withDescription("Every version the API serves."));

      Map<String, Schema> schemas = new LinkedHashMap<>();
      schemas.put("TableApiVersionV1", new Schema().withType(Type.OBJECT).withDescription("One version of an API.").withProperties(versionProperties));
      schemas.put("TableApiV1", new Schema().withType(Type.OBJECT).withDescription("An application API that exposes the table.").withProperties(apiProperties));
      schemas.put(RESPONSE_SCHEMA, new Schema().withType(Type.OBJECT).withDescription("The APIs that expose a table.")
         .withProperties(Map.of("apis", new Schema().withType(Type.ARRAY).withItems(new Schema().withRefToSchema("TableApiV1")).withDescription("The APIs, by name."))));
      return (schemas);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected Object handle(JsonInput input) throws Exception
   {
      String    tableName = input.getPathParam("tableName");
      QInstance qInstance = QContext.getQInstance();

      /////////////////////////////////////////////////////////////////////////
      // an unknown table answers 403, the same as one the user cannot read. //
      /////////////////////////////////////////////////////////////////////////
      if(qInstance.getTable(tableName) == null || !PermissionsHelper.hasTablePermission(new QueryInput(tableName), tableName, TablePermissionSubType.READ))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }

      List<TableApisProviderInterface.TableApi> apis = new ArrayList<>();
      for(TableApisProviderInterface provider : ServiceLoader.load(TableApisProviderInterface.class))
      {
         apis.addAll(provider.getTableApis(qInstance, tableName));
      }
      apis.sort(Comparator.comparing(TableApisProviderInterface.TableApi::name));

      return (Map.of("apis", apis));
   }

}
