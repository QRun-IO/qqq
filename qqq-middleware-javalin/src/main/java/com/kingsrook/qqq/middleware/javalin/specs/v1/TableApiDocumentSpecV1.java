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
import java.util.Optional;
import java.util.ServiceLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.AbstractJsonEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** GET /table/{tableName}/apis/{apiName}/{version}/openapi: the table's OpenAPI
 ** document in one version of an application API (one listed by
 ** GET /table/{tableName}/apis), for the developer views - so they read it
 ** through the UI API rather than from the API middleware's
 ** {api.path}{version}/{table}/openapi.json.  The document comes from the
 ** TableApisProviderInterface that serves the API.
 *******************************************************************************/
public class TableApiDocumentSpecV1 extends AbstractJsonEndpointSpec
{
   private static final String RESPONSE_SCHEMA = "TableApiDocumentV1";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation()
         .withPath("/table/{tableName}/apis/{apiName}/{version}/openapi")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.TABLES)
         .withShortSummary("Get a table's OpenAPI document in an application API")
         .withLongDescription("""
            The OpenAPI 3 document of the table in one version of an application API that exposes \
            it (as listed by `GET /table/{tableName}/apis`).  The caller must be allowed to read the \
            table.  Answers 404 when no API by that name exposes the table in that version."""));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(
         new Parameter().withName("tableName").withIn(In.PATH).withRequired(true)
            .withDescription("Name of the table.").withSchema(new Schema().withType(Type.STRING)),
         new Parameter().withName("apiName").withIn(In.PATH).withRequired(true)
            .withDescription("The API's name (`name` in `GET /table/{tableName}/apis`).").withSchema(new Schema().withType(Type.STRING)),
         new Parameter().withName("version").withIn(In.PATH).withRequired(true)
            .withDescription("The API version (`versions[].version`).").withSchema(new Schema().withType(Type.STRING))));
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
      return (Map.of(RESPONSE_SCHEMA, new Schema().withType(Type.OBJECT).withAdditionalProperties(true)
         .withDescription("An OpenAPI 3 document (the application API's own format, not part of this API's schema).")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected Object handle(JsonInput input) throws Exception
   {
      String    tableName = input.getPathParam("tableName");
      String    apiName   = input.getPathParam("apiName");
      String    version   = input.getPathParam("version");
      QInstance qInstance = QContext.getQInstance();

      /////////////////////////////////////////////////////////////////////////
      // an unknown table answers 403, the same as one the user cannot read. //
      /////////////////////////////////////////////////////////////////////////
      if(qInstance.getTable(tableName) == null || !PermissionsHelper.hasTablePermission(new QueryInput(tableName), tableName, TablePermissionSubType.READ))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }

      for(TableApisProviderInterface provider : ServiceLoader.load(TableApisProviderInterface.class))
      {
         Optional<String> json = provider.getTableSpecJson(qInstance, apiName, version, tableName);
         if(json.isPresent())
         {
            return (JsonUtils.toObject(json.get(), LinkedHashMap.class));
         }
      }

      throw (new QNotFoundException("No API named " + apiName + " exposes this table in version " + version + "."));
   }

}
