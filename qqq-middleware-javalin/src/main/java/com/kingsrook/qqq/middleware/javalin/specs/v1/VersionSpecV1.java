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
import com.kingsrook.qqq.middleware.javalin.executors.VersionExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.VersionInput;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.VersionResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.UiApiExtensionInfo;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Example;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;


/*******************************************************************************
 ** GET /version - the UI API semantic version, features and extensions.
 *******************************************************************************/
public class VersionSpecV1 extends AbstractEndpointSpec<VersionInput, VersionResponseV1, VersionExecutor>
{
   private AbstractMiddlewareVersion middlewareVersion;



   /***************************************************************************
    **
    ***************************************************************************/
   public BasicOperation defineBasicOperation()
   {
      return new BasicOperation()
         .withPath("/version")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.GENERAL)
         .withShortSummary("Get the UI API version")
         .withLongDescription("""
            Describes the semantic version of this UI API (MAJOR.MINOR.PATCH, where MAJOR matches the URL
            version), the optional core features the server supports, and the extensions (modules and qbits)
            registered on it, each with its own semantic version.

            A client should call this first, before authentication, and stop with a clear message when
            apiVersion is outside the range it supports.  Not secured.""");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean isSecured()
   {
      return (false);
   }



   /***************************************************************************
    ** remember which version this spec is bound into, for the executor
    ***************************************************************************/
   @Override
   public void defineRoute(AbstractMiddlewareVersion abstractMiddlewareVersion, String versionBasePath)
   {
      this.middlewareVersion = abstractMiddlewareVersion;
      super.defineRoute(abstractMiddlewareVersion, versionBasePath);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public VersionInput buildInput(Context context) throws Exception
   {
      return (new VersionInput().withMiddlewareVersion(middlewareVersion == null ? new MiddlewareVersionV1() : middlewareVersion));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return Map.of(VersionResponseV1.class.getSimpleName(), new VersionResponseV1().toSchema());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      VersionResponseV1 example = new VersionResponseV1();
      example.setApiVersion("1.1.0");
      example.setBasePath("/qqq/v1");
      example.setSupportedMajorVersions(List.of(1));
      example.setFeatures(List.of("recordSearch", "tableVariants", "version"));
      example.setExtensions(List.of(new UiApiExtensionInfo()
         .withName("esb")
         .withVersion("1.0.0")
         .withPathPrefix("/esb")
         .withDescription("ESB overview, destinations, messages and dead letters")));

      Map<String, Example> examples = new LinkedHashMap<>();
      examples.put("Server with one extension", new Example().withValue(example));
      return new BasicResponse("Successful Response", VersionResponseV1.class.getSimpleName(), examples);
   }
}
