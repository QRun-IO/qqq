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
import com.kingsrook.qqq.middleware.javalin.executors.AbstractMiddlewareExecutor;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.routeproviders.BrandingAssetsRouteProvider;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractMiddlewareVersion;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Response;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;


/*******************************************************************************
 ** GET /branding/{role}: the app's declared branding image (logo or icon),
 ** before sign-in, so a login screen (or a standalone frontend server) can show
 ** it through the UI API (QRun-IO/qqq#1035).  Fixed roles only: no path comes
 ** from the caller.  Since UI API 1.6.0.
 *******************************************************************************/
public class BrandingAssetSpecV1 extends AbstractEndpointSpec<AbstractMiddlewareInput, AbstractMiddlewareOutputInterface, AbstractMiddlewareExecutor<AbstractMiddlewareInput, AbstractMiddlewareOutputInterface>>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation()
         .withPath("/branding/{role}")
         .withHttpMethod(HttpMethod.GET)
         .withTag(TagsV1.AUTHENTICATION)
         .withShortSummary("Get a branding image")
         .withLongDescription("""
            The image the app declares for a branding role (`logo` or `icon`), from the application's
            static overlay, available before sign-in. 404 when the app declares none for the role, or
            declares a data: URI or an external URL (those are in `branding` of GET /metaData/authentication).
            Since 1.6.0."""));
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
    **
    ***************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(new Parameter()
         .withName("role")
         .withDescription("logo or icon.")
         .withRequired(true)
         .withSchema(new Schema().withType(Type.STRING).withEnumValues(List.of("logo", "icon")))
         .withIn(In.PATH)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<Integer, Response> defineResponses()
   {
      return (Map.of(200, new Response()
         .withDescription("The image (png, jpeg, gif, webp, avif, ico or svg).")
         .withContent(Map.of("image/*", new Content().withSchema(new Schema().withType(Type.STRING).withFormat("binary"))))));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (Map.of());
   }



   /***************************************************************************
    ** No input: serveRequest is overridden.
    ***************************************************************************/
   @Override
   public AbstractMiddlewareInput buildInput(Context context)
   {
      return (null);
   }



   /***************************************************************************
    ** Serve the image directly (no executor, no session).
    ***************************************************************************/
   @Override
   public AbstractMiddlewareOutputInterface serveRequest(AbstractMiddlewareVersion abstractMiddlewareVersion, Context context) throws Exception
   {
      BrandingAssetsRouteProvider.serve(context, qInstance);
      return (null);
   }
}
