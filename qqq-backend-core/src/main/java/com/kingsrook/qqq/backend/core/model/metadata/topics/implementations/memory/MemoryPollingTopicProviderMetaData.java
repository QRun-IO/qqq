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

package com.kingsrook.qqq.backend.core.model.metadata.topics.implementations.memory;


import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.TopicType;
import com.kingsrook.qqq.backend.core.modules.topics.QTopicProviderDispatcher;
import com.kingsrook.qqq.backend.core.modules.topics.implementations.memory.MemoryPollingTopicProvider;


/*******************************************************************************
 * Implementation of a topic-provider that runs purely in-memory - and uses
 * polling for its message delivery.
 *
 * <p>Some obvious limitations to this are:</p>
 * <ul>
 *    <li>Messages are only sent within a single process (no server clusters)</li>
 *    <li>Messages are lost upon process restart</li>
 * </ul>
 *******************************************************************************/
public class MemoryPollingTopicProviderMetaData extends QTopicProviderMetaData
{

   static
   {
      QTopicProviderDispatcher.registerTopicProvider(new MemoryPollingTopicProvider());
   }

   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public MemoryPollingTopicProviderMetaData()
   {
      super();
      setType(TopicType.IN_MEMORY_POLLING);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public MemoryPollingTopicProviderMetaData withName(String name)
   {
      super.withName(name);
      return (this);
   }

}
