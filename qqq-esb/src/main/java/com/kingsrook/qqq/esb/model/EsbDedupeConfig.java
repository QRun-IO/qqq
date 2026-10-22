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

package com.kingsrook.qqq.esb.model;


import java.io.Serializable;


/*******************************************************************************
 * Settings for idempotent ESB triggers (EsbInstanceMetaData.withDedupe).
 *
 * Off by default.  When enabled, every trigger records the message ids it has
 * processed in the processed-message table (defined by
 * EsbProcessedMessageTable.defineTable, created by the application's
 * migrations), and skips a message it has already processed - so a
 * redelivery (after a broker failover, a lost commit, or an outbox re-send)
 * does not run the process twice.  See docs/esb/index.adoc.
 *******************************************************************************/
public class EsbDedupeConfig implements Serializable
{
   private static final long serialVersionUID = 1L;

   private Boolean enabled = false;
   private String tableName = "esb_processed_message";
   private Integer retentionDays = 7;



   /*******************************************************************************
    ** Getter for enabled: whether triggers skip messages they already processed.
    *******************************************************************************/
   public Boolean getEnabled()
   {
      return (this.enabled);
   }



   /*******************************************************************************
    ** Setter for enabled
    *******************************************************************************/
   public void setEnabled(Boolean enabled)
   {
      this.enabled = enabled;
   }



   /*******************************************************************************
    ** Fluent setter for enabled
    *******************************************************************************/
   public EsbDedupeConfig withEnabled(Boolean enabled)
   {
      this.enabled = enabled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName: the QQQ name of the processed-message table (default esb_processed_message).
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public EsbDedupeConfig withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for retentionDays: how many days processed message ids are remembered (a redelivery after that runs again).
    *******************************************************************************/
   public Integer getRetentionDays()
   {
      return (this.retentionDays);
   }



   /*******************************************************************************
    ** Setter for retentionDays
    *******************************************************************************/
   public void setRetentionDays(Integer retentionDays)
   {
      this.retentionDays = retentionDays;
   }



   /*******************************************************************************
    ** Fluent setter for retentionDays
    *******************************************************************************/
   public EsbDedupeConfig withRetentionDays(Integer retentionDays)
   {
      this.retentionDays = retentionDays;
      return (this);
   }

}
