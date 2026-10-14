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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.FollowEvent;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFollower;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Best-effort access to the instance-level record followers provider.
 *******************************************************************************/
public class RecordFollowers
{
   private static final QLogger LOG = QLogger.getLogger(RecordFollowers.class);



   /*******************************************************************************
    ** Return event subscribers, or an empty list when unavailable or FOLLOW is disabled.
    ** Provider failures are logged and return an empty list. Callers must check each
    ** recipient's read access before sending record data; see RecordFollowersProviderInterface.
    *******************************************************************************/
   public static List<RecordFollower> get(QInstance instance, RecordReference reference, FollowEvent event)
   {
      RecordFeaturesMetaData metadata = RecordFeaturesMetaData.of(instance);
      if(metadata == null || metadata.getFollowersProvider() == null)
      {
         return (List.of());
      }
      QTableMetaData table = instance.getTable(reference.tableName());
      if(table == null || !RecordFeaturesResolver.resolve(instance, table).isEnabled(RecordFeature.FOLLOW))
      {
         return (List.of());
      }
      try
      {
         List<RecordFollower> followers = QCodeLoader.getAdHoc(RecordFollowersProviderInterface.class, metadata.getFollowersProvider()).getFollowers(instance, reference, event);
         return (followers == null ? List.of() : followers);
      }
      catch(Exception e)
      {
         LOG.warn("Unable to get record followers", e);
         return (List.of());
      }
   }

}
