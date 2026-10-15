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


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.metadata.profile.UserProfilesMetaData;


/*******************************************************************************
 ** Per-user limit on photo conversions (MAX_UPLOADS_PER_MINUTE in a sliding
 ** minute, per node): a user cannot keep the image decoder busy.
 *******************************************************************************/
final class UploadRateLimit
{
   private static final long WINDOW_MILLIS = 60_000;

   private static final Map<String, Deque<Long>> RECENT = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private UploadRateLimit()
   {
   }



   /*******************************************************************************
    ** Count one conversion for the user, or refuse it when over the limit.
    *******************************************************************************/
   static void check(String userId) throws QBadRequestException
   {
      long        now    = System.currentTimeMillis();
      Deque<Long> recent = RECENT.computeIfAbsent(userId, k -> new ArrayDeque<>());
      synchronized(recent)
      {
         while(!recent.isEmpty() && now - recent.peekFirst() > WINDOW_MILLIS)
         {
            recent.pollFirst();
         }
         if(recent.size() >= UserProfilesMetaData.MAX_UPLOADS_PER_MINUTE)
         {
            throw (new QBadRequestException("Photo: too many changes in a minute. Try again shortly."));
         }
         recent.addLast(now);
      }
   }



   /*******************************************************************************
    ** Forget every count (tests).
    *******************************************************************************/
   static void reset()
   {
      RECENT.clear();
   }
}
