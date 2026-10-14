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

package com.kingsrook.qqq.backend.core.model.actions.messaging;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;


/*******************************************************************************
 ** Provider result. Optional fields are null until supplied by a provider or caller.
 ** See SendMessageStatus for delivery semantics; SENT does not mean inbox delivery.
 *******************************************************************************/
public class SendMessageOutput extends AbstractActionOutput
{
   private Serializable      messageId;
   private SendMessageStatus status;
   private List<String>      reasons;



   /*******************************************************************************
    ** Getter for messageId. Provider message identifier; for DUPLICATE, identifies the earlier message.
    *******************************************************************************/
   public Serializable getMessageId()
   {
      return (this.messageId);
   }



   /*******************************************************************************
    ** Setter for messageId
    *******************************************************************************/
   public void setMessageId(Serializable messageId)
   {
      this.messageId = messageId;
   }



   /*******************************************************************************
    ** Fluent setter for messageId
    *******************************************************************************/
   public SendMessageOutput withMessageId(Serializable messageId)
   {
      this.messageId = messageId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for status. Delivery outcome; core defaults an unset provider status to SENT.
    *******************************************************************************/
   public SendMessageStatus getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(SendMessageStatus status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public SendMessageOutput withStatus(SendMessageStatus status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for reasons. Provider explanations, particularly when SUPPRESSED or REFUSED.
    *******************************************************************************/
   public List<String> getReasons()
   {
      return (this.reasons);
   }



   /*******************************************************************************
    ** Setter for reasons
    *******************************************************************************/
   public void setReasons(List<String> reasons)
   {
      this.reasons = reasons;
   }



   /*******************************************************************************
    ** Fluent setter for reasons
    *******************************************************************************/
   public SendMessageOutput withReasons(List<String> reasons)
   {
      this.reasons = reasons;
      return (this);
   }

}
