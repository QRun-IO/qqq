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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** A user's chosen Gravatar, fetched by the server and re-encoded like an
 ** uploaded photo: browsers never contact Gravatar (no viewer IP, no email hash
 ** in any URL), and nothing Gravatar returns is served as-is.
 **
 ** Fetched and converted once per avatar version: the sizes are kept in the
 ** avatar storage table (gravatars/{avatarId}/{version}/{size}.png) and served
 ** from there; a user with no Gravatar is remembered for ten minutes per node.
 ** Conversions use their own single permit, so they never take the upload
 ** decoders.
 *******************************************************************************/
public final class GravatarImages
{
   private static final QLogger LOG = QLogger.getLogger(GravatarImages.class);

   private static final long     MAX_BYTES       = 1024 * 1024;
   private static final Duration TIMEOUT         = Duration.ofSeconds(3);
   private static final long     MISS_TTL_MILLIS = 10 * 60 * 1000;
   private static final int      MAX_MISSES      = 10_000;

   private static final HttpClient CLIENT  = HttpClient.newBuilder().connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();
   private static final Semaphore  PERMITS = new Semaphore(1, true);

   private static final Map<String, Long> MISSES = new ConcurrentHashMap<>();

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
    ** The avatar's Gravatar as a PNG of the given size, or null when the user has
    ** none there (or Gravatar can't be reached; clients then draw initials).
    *******************************************************************************/
   public static byte[] png(UserProfiles.Avatar avatar, int size)
   {
      String version = avatar.avatarId() + "/" + avatar.version();
      byte[] cached  = readCached(version, size);
      if(cached != null)
      {
         return (cached);
      }

      Long missUntil = MISSES.get(version);
      if(missUntil != null && missUntil > System.currentTimeMillis())
      {
         return (null);
      }

      try
      {
         byte[] raw = fetcher.fetch(avatar.gravatarHash());
         if(raw == null)
         {
            rememberMiss(version);
            return (null);
         }
         Map<Integer, byte[]> sizes = AvatarImageProcessor.process(raw, PERMITS);
         for(Map.Entry<Integer, byte[]> entry : sizes.entrySet())
         {
            UserProfiles.writeObject(cacheObject(version, entry.getKey()), entry.getValue());
         }
         return (sizes.get(size));
      }
      catch(Exception e)
      {
         LOG.info("Could not load a Gravatar", e, logPair("size", size));
         rememberMiss(version);
         return (null);
      }
   }



   /*******************************************************************************
    ** Delete the cached sizes of one avatar version (when the user turns Gravatar
    ** off or on again). Best effort.
    *******************************************************************************/
   static void deleteCached(UserProfiles.Avatar avatar)
   {
      String version = avatar.avatarId() + "/" + avatar.version();
      MISSES.remove(version);
      for(Integer size : UserProfilesMetaData.AVATAR_SIZES)
      {
         try
         {
            new StorageAction().deleteObject(cacheObject(version, size));
         }
         catch(Exception e)
         {
            ////////////////////////////////////////////////////////////
            // never cached (or already gone): nothing to remove      //
            ////////////////////////////////////////////////////////////
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static byte[] readCached(String version, int size)
   {
      try(InputStream input = new StorageAction().getInputStream(cacheObject(version, size)))
      {
         return (input.readAllBytes());
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static StorageInput cacheObject(String version, int size)
   {
      return (new StorageInput(UserProfiles.settings().getAvatarStorageTableName()).withReference("gravatars/" + version + "/" + size + ".png").withContentType("image/png"));
   }



   /*******************************************************************************
    ** Remember a miss (bounded: past the cap, expired entries go, then all).
    *******************************************************************************/
   private static void rememberMiss(String version)
   {
      long now = System.currentTimeMillis();
      if(MISSES.size() >= MAX_MISSES)
      {
         MISSES.values().removeIf(until -> until <= now);
         if(MISSES.size() >= MAX_MISSES)
         {
            MISSES.clear();
         }
      }
      MISSES.put(version, now + MISS_TTL_MILLIS);
   }



   /*******************************************************************************
    ** GET the largest size from Gravatar (d=404: no default image), bounded.
    *******************************************************************************/
   private static byte[] fetchFromGravatar(String gravatarHash) throws Exception
   {
      int                       size     = UserProfilesMetaData.AVATAR_SIZES.get(UserProfilesMetaData.AVATAR_SIZES.size() - 1);
      HttpRequest               request  = HttpRequest.newBuilder(URI.create("https://www.gravatar.com/avatar/" + gravatarHash + "?s=" + size + "&d=404")).timeout(TIMEOUT).GET().build();
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
    ** Replace the raw fetch (tests); null restores Gravatar. Also forgets misses.
    *******************************************************************************/
   public static void setFetcher(Fetcher replacement)
   {
      fetcher = replacement == null ? GravatarImages::fetchFromGravatar : replacement;
      MISSES.clear();
   }
}
