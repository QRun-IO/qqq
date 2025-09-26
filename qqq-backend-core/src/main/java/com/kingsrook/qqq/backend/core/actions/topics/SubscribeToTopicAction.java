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

package com.kingsrook.qqq.backend.core.actions.topics;


import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.topics.SubscribeToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.SubscribeToTopicOutput;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicProviderMetaData;
import com.kingsrook.qqq.backend.core.modules.topics.QTopicProviderDispatcher;
import com.kingsrook.qqq.backend.core.modules.topics.TopicProviderInterface;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * action to subscribe to a topic
 *******************************************************************************/
public class SubscribeToTopicAction extends AbstractQActionFunction<SubscribeToTopicInput, SubscribeToTopicOutput>
{
   private static final QLogger LOG = QLogger.getLogger(SubscribeToTopicAction.class);



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public SubscribeToTopicOutput execute(SubscribeToTopicInput input) throws QException
   {
      try
      {
         QTopicMetaData         topic                  = QContext.getQInstance().getTopic(input.getTopicName());
         QTopicProviderMetaData provider               = QContext.getQInstance().getTopicProvider(topic.getProviderName());
         TopicProviderInterface topicProviderInterface = new QTopicProviderDispatcher().getTopicProviderInterface(provider);

         return topicProviderInterface.subscribe(input);
      }
      catch(QException e)
      {
         LOG.warn("Error subscribing to topic", e, logPair("topic", () -> input.getTopicName()));
         throw (new QException("Error subscribing to topic", e));
      }
   }

}
