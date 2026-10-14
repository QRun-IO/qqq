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
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** Lists the application (model) APIs that expose a table, for
 ** GET /qqq/v1/table/{tableName}/apis (TableApisSpecV1).
 **
 ** Implementations are found with java.util.ServiceLoader (list the class in
 ** META-INF/services/com.kingsrook.qqq.middleware.javalin.specs.v1.TableApisProviderInterface),
 ** so a module that serves APIs (qqq-middleware-api) contributes by being on
 ** the classpath.  An implementation returns an empty list when the instance
 ** has none of its APIs.
 *******************************************************************************/
public interface TableApisProviderInterface
{

   /***************************************************************************
    ** The APIs in this instance that expose the table (which exists, and which
    ** the caller may read).
    ***************************************************************************/
   List<TableApi> getTableApis(QInstance qInstance, String tableName);



   /***************************************************************************
    ** One API that exposes the table.
    **
    ** @param name           the API's name (stable key)
    ** @param label          label to show users
    ** @param path           root-relative base path of the API, e.g. /api/
    ** @param currentVersion the version the API calls current (may be null)
    ** @param versions       every version the API serves
    ***************************************************************************/
   record TableApi(String name, String label, String path, String currentVersion, List<TableApiVersion> versions)
   {
   }



   /***************************************************************************
    ** One version of an API.
    **
    ** @param version         the version, as it appears in URLs
    ** @param semanticVersion its semantic version, when the API declares one
    ** @param tableSpecPath   root-relative path of the table's OpenAPI document (JSON) in this version
    ***************************************************************************/
   record TableApiVersion(String version, String semanticVersion, String tableSpecPath)
   {
   }

}
