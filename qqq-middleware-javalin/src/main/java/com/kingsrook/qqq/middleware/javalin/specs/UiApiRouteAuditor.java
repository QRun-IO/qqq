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

package com.kingsrook.qqq.middleware.javalin.specs;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtensionsMetaData;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import io.javalin.router.ParsedEndpoint;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Finds HTTP routes bound under a UI API version's base path (e.g. /qqq/v1)
 ** that no endpoint spec describes - raw handlers that a non-web client could
 ** not discover from the OpenAPI document.  QApplicationJavalinServer runs it
 ** after building the service: by default it logs a warning per route; in
 ** strict mode it fails startup.  See docs/UI-API-EXTENSIONS.md.
 *******************************************************************************/
public class UiApiRouteAuditor
{
   private static final QLogger LOG = QLogger.getLogger(UiApiRouteAuditor.class);

   private static final Set<HandlerType> HTTP_METHODS = Set.of(HandlerType.GET, HandlerType.POST, HandlerType.PUT, HandlerType.PATCH, HandlerType.DELETE);



   /*******************************************************************************
    ** The routes (as "METHOD /path") under the versions' base paths that no spec
    ** describes.
    *******************************************************************************/
   public static List<String> findUnspecifiedRoutes(Javalin service, List<AbstractMiddlewareVersion> versions)
   {
      Set<String> specified = new HashSet<>();
      Set<String> basePaths = new HashSet<>();
      for(AbstractMiddlewareVersion version : versions)
      {
         String basePath = MiddlewareSpecExtensionsMetaData.normalizePathTemplate("/qqq/" + version.getVersionBasePath());
         basePaths.add(basePath);

         for(AbstractEndpointSpec<?, ?, ?> spec : version.getAllEndpointSpecs())
         {
            CompleteOperation operation = spec.defineCompleteOperation();
            specified.add(operation.getHttpMethod().name() + " " + MiddlewareSpecExtensionsMetaData.normalizePathTemplate(basePath + "/" + operation.getPath()));
         }

         ///////////////////////////////////////////////////////////
         // documentation routes (QMiddlewareApiSpecHandler)      //
         ///////////////////////////////////////////////////////////
         for(String documentationBasePath : version.getDocumentationBasePaths())
         {
            String docs = MiddlewareSpecExtensionsMetaData.normalizePathTemplate("/qqq/" + documentationBasePath);
            for(String suffix : List.of("", "/openapi.yaml", "/openapi.json", "/openapi.html"))
            {
               specified.add("GET " + MiddlewareSpecExtensionsMetaData.normalizePathTemplate(docs + suffix));
            }
         }
      }

      List<String> rs = new ArrayList<>();
      for(ParsedEndpoint parsedEndpoint : service.unsafe.internalRouter.allHttpHandlers())
      {
         HandlerType method = parsedEndpoint.endpoint.method;
         String      path   = MiddlewareSpecExtensionsMetaData.normalizePathTemplate(parsedEndpoint.endpoint.path);
         if(!HTTP_METHODS.contains(method) || basePaths.stream().noneMatch(basePath -> path.startsWith(basePath + "/")))
         {
            continue;
         }

         String key = method.name() + " " + path;
         if(!specified.contains(key))
         {
            rs.add(method.name() + " " + parsedEndpoint.endpoint.path);
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** Audit a service: warn about each unspecified route, or throw if strict.
    *******************************************************************************/
   public static void audit(Javalin service, List<AbstractMiddlewareVersion> versions, boolean strict)
   {
      List<String> unspecified;
      try
      {
         unspecified = findUnspecifiedRoutes(service, versions);
      }
      catch(Exception e)
      {
         LOG.warn("Could not audit UI API routes", e);
         return;
      }

      if(unspecified.isEmpty())
      {
         return;
      }

      for(String route : unspecified)
      {
         LOG.warn("UI API route has no endpoint spec, so it is missing from the OpenAPI document; register it as a spec (docs/UI-API-EXTENSIONS.md)", logPair("route", route));
      }

      if(strict)
      {
         throw (new IllegalStateException("UI API routes without endpoint specs (strict mode): " + unspecified + ". See docs/UI-API-EXTENSIONS.md."));
      }
   }
}
