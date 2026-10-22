/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.queues.GetQueueSize;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueProviderMetaData;


/*******************************************************************************
 **
 *******************************************************************************/
public class QueueSizeWidgetValue extends AbstractWidgetValueSource
{
   private static final QLogger LOG = QLogger.getLogger(QueueSizeWidgetValue.class);

   private String queueName;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Object evaluate(Map<String, Object> context, RenderWidgetInput input) throws QException
   {
      QQueueMetaData         queue         = QContext.getQInstance().getQueue(queueName);
      QQueueProviderMetaData queueProvider = QContext.getQInstance().getQueueProvider(queue.getProviderName());
      return (new GetQueueSize().getQueueSize(queueProvider, queue));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public QueueSizeWidgetValue withName(String name)
   {
      setName(name);
      return (this);
   }



   /*******************************************************************************
    ** Getter for queueName
    *******************************************************************************/
   public String getQueueName()
   {
      return (this.queueName);
   }



   /*******************************************************************************
    ** Setter for queueName
    *******************************************************************************/
   public void setQueueName(String queueName)
   {
      this.queueName = queueName;
   }



   /*******************************************************************************
    ** Fluent setter for queueName
    *******************************************************************************/
   public QueueSizeWidgetValue withQueueName(String queueName)
   {
      this.queueName = queueName;
      return (this);
   }

}
