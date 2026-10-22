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

package com.kingsrook.qqq.backend.core.jobs.progress;


import java.io.Serializable;


/*******************************************************************************
 ** What a tracking job (JobTracking.start) is: its owner, label, the process
 ** name that identifies its kind (for example "assistant.turn"), the client
 ** channel, and optionally the record it is about.
 *******************************************************************************/
public class TrackedJobRequest
{
   private String       userId;
   private String       label;
   private String       processName;
   private String       clientChannel;
   private String       tableName;
   private Serializable recordId;



   /*******************************************************************************
    ** Getter for userId
    *******************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    ** Fluent setter for userId
    *******************************************************************************/
   public TrackedJobRequest withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public TrackedJobRequest withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public TrackedJobRequest withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for clientChannel
    *******************************************************************************/
   public String getClientChannel()
   {
      return (this.clientChannel);
   }



   /*******************************************************************************
    ** Fluent setter for clientChannel
    *******************************************************************************/
   public TrackedJobRequest withClientChannel(String clientChannel)
   {
      this.clientChannel = clientChannel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public TrackedJobRequest withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordId
    *******************************************************************************/
   public Serializable getRecordId()
   {
      return (this.recordId);
   }



   /*******************************************************************************
    ** Fluent setter for recordId
    *******************************************************************************/
   public TrackedJobRequest withRecordId(Serializable recordId)
   {
      this.recordId = recordId;
      return (this);
   }

}
