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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.middleware.javalin.QJavalinRouteProviderInterface;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;


/*******************************************************************************
 ** Public, fixed-role access to the images already declared in public branding.
 ** Does not mount the application overlay or enable a Java dashboard.
 *******************************************************************************/
public final class BrandingAssetsRouteProvider implements QJavalinRouteProviderInterface
{
   private static final Map<String, String> CONTENT_TYPES = Map.of(
      "png", "image/png", "jpg", "image/jpeg", "jpeg", "image/jpeg", "gif", "image/gif",
      "webp", "image/webp", "avif", "image/avif", "ico", "image/x-icon", "svg", "image/svg+xml");
   private static final Set<String> RESERVED = Set.of("app", "login", "callback", "_next", "qqq", "data", "widget", "metadata", "download", "processes", "possiblevalues", "reports", "managesession", "apis.json", "api");

   private volatile QInstance instance;



   /***************************************************************************
    ** Use the current instance, including supported development hot swaps.
    ***************************************************************************/
   @Override
   public void setQInstance(QInstance qInstance)
   {
      instance = qInstance;
   }



   /***************************************************************************
    ** Fixed roles only; no URL or resource path is supplied by the caller.
    ***************************************************************************/
   @Override
   public void acceptJavalinConfig(JavalinConfig config)
   {
      config.routes.get("/qqq/branding/{role}", this::handle);
      config.routes.head("/qqq/branding/{role}", this::handle);
   }



   /***************************************************************************
    ** Canonical root-relative image path, or null. Decode only once and reject
    ** traversal before any normalizing URL parser can erase it.
    ***************************************************************************/
   static String imagePath(String value)
   {
      if(value == null || !value.startsWith("/") || value.startsWith("//"))
      {
         return null;
      }
      String raw = value.split("[?#]", 2)[0];
      if(raw.matches("(?i).*%(2f|5c).*"))
      {
         return null;
      }
      final String decoded;
      try
      {
         decoded = URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
      }
      catch(IllegalArgumentException e)
      {
         return null;
      }
      if(decoded.chars().anyMatch(c -> c < 32 || c == 127 || c == 0xfffd || c == '\\' || c == '%' || c == '?' || c == '#'))
      {
         return null;
      }
      String[] segments = decoded.substring(1).split("/", -1);
      for(String segment : segments)
      {
         if(segment.isEmpty() || segment.equals(".") || segment.equals(".."))
         {
            return null;
         }
      }
      if(RESERVED.contains(segments[0].toLowerCase(Locale.ROOT)))
      {
         return null;
      }
      String suffix = decoded.substring(decoded.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
      return CONTENT_TYPES.containsKey(suffix) ? decoded : null;
   }



   /***************************************************************************
    ** Resolve only the current declared role inside the application overlay.
    ***************************************************************************/
   private void handle(Context context)
   {
      QInstance current = instance;
      QBrandingMetaData branding = current == null ? null : current.getBranding();
      String role = context.pathParam("role");
      String declared = branding == null ? null : switch(role)
      {
         case "logo" -> branding.getLogo();
         case "icon" -> branding.getIcon();
         default -> null;
      };
      String resource = imagePath(declared);
      InputStream stream = resource == null ? null : getClass().getClassLoader().getResourceAsStream("material-dashboard-overlay" + resource);
      context.header("X-Content-Type-Options", "nosniff");
      context.header("Cache-Control", "no-cache");
      context.header("Content-Security-Policy", "default-src 'none'; sandbox");
      if(stream == null)
      {
         context.status(404).contentType("text/plain").result("Not found");
         return;
      }
      context.contentType(CONTENT_TYPES.get(resource.substring(resource.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)));
      if(context.method() == HandlerType.HEAD)
      {
         try(stream)
         {
            context.status(200);
         }
         catch(java.io.IOException e)
         {
            context.status(500);
         }
         return;
      }
      context.result(stream);
   }
}
