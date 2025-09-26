/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.modules.topics.implementations.memory;


import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import com.kingsrook.qqq.backend.core.model.actions.topics.TopicMessage;


/*******************************************************************************
 **
 *******************************************************************************/
public class MemoryTopicDataStore
{
   private static MemoryTopicDataStore memoryTopicDataStore = null;

   private static Map<String, Deque<String>> topicData = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Singleton constructor
    *******************************************************************************/
   private MemoryTopicDataStore()
   {

   }



   /*******************************************************************************
    ** Singleton accessor
    *******************************************************************************/
   public static MemoryTopicDataStore getInstance()
   {
      if(memoryTopicDataStore == null)
      {
         memoryTopicDataStore = new MemoryTopicDataStore();
      }
      return (memoryTopicDataStore);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public void post(String topicName, String message)
   {
      getQueueForTopic(topicName).offer(message);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public List<TopicMessage> poll(String topicName, Integer limit)
   {
      List<TopicMessage> rs = new ArrayList<>();

      while(true)
      {
         String message = getQueueForTopic(topicName).poll();
         if(message == null)
         {
            break;
         }

         rs.add(new TopicMessage().withTopicName(topicName).withMessage(message));
         if(limit != null && rs.size() >= limit)
         {
            break;
         }
      }

      return (rs);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private Deque<String> getQueueForTopic(String topicName)
   {
      return (topicData.computeIfAbsent(topicName, (k) -> new ConcurrentLinkedDeque<>()));
   }
}
