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

package com.kingsrook.qqq.api.javalin;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.api.model.metadata.ApiHttpPolicy;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.Auth0AuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;


/*******************************************************************************
 ** Applies an {@link ApiHttpPolicy} to requests and responses of one API
 ** instance: CORS before the request is handled, cache headers and validators
 ** after.
 *******************************************************************************/
public final class ApiHttpPolicyHandler
{
   public static final String CACHE_CONTROL     = "Cache-Control";
   public static final String CDN_CACHE_CONTROL = "CDN-Cache-Control";
   public static final String PRIVATE_NO_STORE  = "private, no-store";

   private static final Pattern PLAIN_PUBLIC = Pattern.compile("^public,\\s*max-age=(\\d+)$");



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private ApiHttpPolicyHandler()
   {
   }



   /*******************************************************************************
    ** Whether the request carries any credential QQQ would authenticate.
    *******************************************************************************/
   public static boolean hasCredentials(Context context)
   {
      if(StringUtils.hasContent(context.header("Authorization")) || StringUtils.hasContent(context.header("x-api-key")))
      {
         return (true);
      }
      Map<String, String> cookies = context.cookieMap();
      return (cookies.containsKey(QJavalinImplementation.SESSION_ID_COOKIE_NAME) || cookies.containsKey(Auth0AuthenticationModule.SESSION_UUID_KEY));
   }



   /*******************************************************************************
    ** A GET or HEAD request without credentials.
    *******************************************************************************/
   public static boolean isPublicRead(Context context)
   {
      HandlerType method = context.method();
      return ((HandlerType.GET.equals(method) || HandlerType.HEAD.equals(method)) && !hasCredentials(context));
   }



   /*******************************************************************************
    ** Before-handler: CORS headers.
    *******************************************************************************/
   public static void applyCors(Context context, ApiHttpPolicy policy)
   {
      if(isPublicRead(context))
      {
         context.header("Access-Control-Allow-Origin", "*");
         context.header("Access-Control-Expose-Headers", String.join(", ", ApiHttpPolicy.CORS_EXPOSED_HEADERS));
         return;
      }

      context.header("Vary", "Origin");
      String origin = context.header("Origin");
      if(policy.isCorsAllowedOrigin(origin))
      {
         context.header("Access-Control-Allow-Origin", origin);
         context.header("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS");
         context.header("Access-Control-Allow-Headers", String.join(", ", ApiHttpPolicy.CORS_ALLOWED_HEADERS));
         context.header("Access-Control-Expose-Headers", String.join(", ", ApiHttpPolicy.CORS_EXPOSED_HEADERS));
         context.header("Access-Control-Max-Age", String.valueOf(ApiHttpPolicy.CORS_MAX_AGE_SECONDS));
      }

      //////////////////////////////////////////////////////////////////
      // preflight: answered here (the API registers no OPTIONS routes) //
      //////////////////////////////////////////////////////////////////
      if(HandlerType.OPTIONS.equals(context.method()))
      {
         context.status(204);
         context.skipRemainingHandlers();
      }
   }



   /*******************************************************************************
    ** After-handler: cache headers, ETag and 304 for public reads; private,
    ** no-store for everything else.
    *******************************************************************************/
   public static void applyCaching(Context context, ApiHttpPolicy policy)
   {
      String routeValue = context.res().getHeader(CACHE_CONTROL);
      if(!isPublicRead(context) || context.status().getCode() != 200 || policy.getPublicReadMaxAgeSeconds() == null)
      {
         if(routeValue == null || routeValue.startsWith("public"))
         {
            context.header(CACHE_CONTROL, PRIVATE_NO_STORE);
            context.res().setHeader(CDN_CACHE_CONTROL, null);
         }
         return;
      }

      Integer maxAge = routeValue == null ? policy.getPublicReadMaxAgeSeconds() : plainPublicMaxAge(routeValue);
      if(maxAge != null)
      {
         context.header(CACHE_CONTROL, policy.publicCacheControl(maxAge));
         context.header(CDN_CACHE_CONTROL, policy.cdnCacheControl(maxAge));
      }
      else if(!routeValue.startsWith("public"))
      {
         ///////////////////////////////////////////////////////////////
         // the route chose not to be shared-cacheable: no validators //
         ///////////////////////////////////////////////////////////////
         return;
      }

      if(policy.getEtags() && context.res().getHeader("ETag") == null)
      {
         byte[] body = bufferedBody(context, policy.getMaxEtagBodyBytes());
         if(body != null)
         {
            String etag = weakEtag(body);
            context.header("ETag", etag);
            if(ifNoneMatchMatches(context.header("If-None-Match"), etag))
            {
               context.status(304);
               context.result(new byte[0]);
            }
         }
      }
   }



   /*******************************************************************************
    ** max-age of an exact "public, max-age=N" value, else null.
    *******************************************************************************/
   static Integer plainPublicMaxAge(String value)
   {
      Matcher matcher = PLAIN_PUBLIC.matcher(value.trim());
      return (matcher.matches() ? Integer.valueOf(matcher.group(1)) : null);
   }



   /*******************************************************************************
    ** Weak entity tag over the body: W/"base64url(first 16 bytes of SHA-256)".
    *******************************************************************************/
   public static String weakEtag(byte[] body)
   {
      try
      {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(body);
         return ("W/\"" + Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, 16)) + "\"");
      }
      catch(NoSuchAlgorithmException e)
      {
         throw (new IllegalStateException("SHA-256 is not available", e));
      }
   }



   /*******************************************************************************
    ** Weak comparison of an If-None-Match header against an entity tag (RFC 9110
    ** 13.1.2): any listed tag, ignoring the W/ prefix, or "*".
    *******************************************************************************/
   public static boolean ifNoneMatchMatches(String ifNoneMatch, String etag)
   {
      if(!StringUtils.hasContent(ifNoneMatch))
      {
         return (false);
      }
      String opaque = stripWeak(etag);
      for(String candidate : ifNoneMatch.split(","))
      {
         String tag = candidate.trim();
         if("*".equals(tag) || stripWeak(tag).equals(opaque))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** Remove a W/ prefix.
    *******************************************************************************/
   private static String stripWeak(String tag)
   {
      return (tag.startsWith("W/") ? tag.substring(2) : tag);
   }



   /*******************************************************************************
    ** The response body when it is an in-memory buffer within the size limit
    ** (null for streamed results, which are left alone).  The buffer is put back.
    *******************************************************************************/
   private static byte[] bufferedBody(Context context, int maxBytes)
   {
      InputStream stream = context.resultInputStream();
      if(stream == null)
      {
         return ("".getBytes(StandardCharsets.UTF_8));
      }
      if(!(stream instanceof ByteArrayInputStream buffer) || buffer.available() > maxBytes)
      {
         return (null);
      }
      byte[] body = buffer.readAllBytes();
      context.result(body);
      return (body);
   }
}
