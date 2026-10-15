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

package com.kingsrook.qqq.backend.core.actions.profile;


import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** A user's chosen Gravatar, fetched by the server and re-encoded like an
 ** uploaded photo: browsers never contact Gravatar (no viewer IP, no email hash
 ** in any URL), and nothing Gravatar returns is served as-is.
 *******************************************************************************/
public final class GravatarImages
{
   private static final QLogger LOG = QLogger.getLogger(GravatarImages.class);

   private static final long     MAX_BYTES = 1024 * 1024;
   private static final Duration TIMEOUT   = Duration.ofSeconds(3);

   private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();

   /////////////////////////////////////////////////////////////////
   // the raw fetch (hash -> image bytes or null); tests replace it //
   /////////////////////////////////////////////////////////////////
   private static volatile Fetcher fetcher = GravatarImages::fetchFromGravatar;



   /*******************************************************************************
    ** Fetches a Gravatar's raw image bytes, or null when there is none.
    *******************************************************************************/
   @FunctionalInterface
   public interface Fetcher
   {
      /***************************************************************************
       **
       ***************************************************************************/
      byte[] fetch(String gravatarHash) throws Exception;
   }



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private GravatarImages()
   {
   }



   /*******************************************************************************
    ** The Gravatar as a PNG of the given size, or null when the user has none
    ** there (or Gravatar can't be reached; clients then draw initials).
    *******************************************************************************/
   public static byte[] png(String gravatarHash, int size)
   {
      try
      {
         byte[] raw = fetcher.fetch(gravatarHash);
         if(raw == null)
         {
            return (null);
         }
         Map<Integer, byte[]> sizes = AvatarImageProcessor.process(raw);
         return (sizes.get(size));
      }
      catch(Exception e)
      {
         LOG.info("Could not load a Gravatar", e, logPair("size", size));
         return (null);
      }
   }



   /*******************************************************************************
    ** GET the largest size from Gravatar (d=404: no default image), bounded.
    *******************************************************************************/
   private static byte[] fetchFromGravatar(String gravatarHash) throws Exception
   {
      int                 size     = UserProfilesMetaData.AVATAR_SIZES.get(UserProfilesMetaData.AVATAR_SIZES.size() - 1);
      HttpRequest         request  = HttpRequest.newBuilder(URI.create("https://www.gravatar.com/avatar/" + gravatarHash + "?s=" + size + "&d=404")).timeout(TIMEOUT).GET().build();
      HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try(InputStream body = response.body())
      {
         if(response.statusCode() != 200)
         {
            return (null);
         }
         return (AvatarImageProcessor.readBounded(body, MAX_BYTES));
      }
   }



   /*******************************************************************************
    ** Replace the raw fetch (tests); null restores Gravatar.
    *******************************************************************************/
   public static void setFetcher(Fetcher replacement)
   {
      fetcher = replacement == null ? GravatarImages::fetchFromGravatar : replacement;
   }
}
