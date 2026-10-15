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
 * Settings for the transactional ESB outbox (EsbInstanceMetaData.withOutbox).
 *
 * Off by default.  When enabled, record change events are written to the
 * outbox table (defined by EsbOutboxTable.defineTable, created by the
 * application's migrations) in the same backend transaction as the record
 * write, and a relay on every node publishes them: in order per destination,
 * with retries and backoff, at-least-once.  See docs/esb/index.adoc.
 *******************************************************************************/
public class EsbOutboxConfig implements Serializable
{
   private static final long serialVersionUID = 1L;

   private Boolean enabled = false;
   private String tableName = "esb_outbox";
   private Integer batchSize = 100;
   private Integer pollIntervalMs = 1000;
   private Integer leaseMs = 60_000;
   private Integer maxAttempts = 10;
   private Integer initialBackoffMs = 1000;
   private Integer maxBackoffMs = 300_000;
   private Integer retentionDays = 7;
   private Integer purgeIntervalMs = 3_600_000;
   private Integer pendingAlertMs = 300_000;



   /*******************************************************************************
    ** Getter for enabled: whether record change events go through the outbox.
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
   public EsbOutboxConfig withEnabled(Boolean enabled)
   {
      this.enabled = enabled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName: the QQQ name of the outbox table (default esb_outbox).
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
   public EsbOutboxConfig withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for batchSize: the most rows the relay sends, per destination, in one broker transaction.
    *******************************************************************************/
   public Integer getBatchSize()
   {
      return (this.batchSize);
   }



   /*******************************************************************************
    ** Setter for batchSize
    *******************************************************************************/
   public void setBatchSize(Integer batchSize)
   {
      this.batchSize = batchSize;
   }



   /*******************************************************************************
    ** Fluent setter for batchSize
    *******************************************************************************/
   public EsbOutboxConfig withBatchSize(Integer batchSize)
   {
      this.batchSize = batchSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pollIntervalMs: how long the relay waits between passes when idle (a commit that writes outbox rows wakes it sooner on this node).
    *******************************************************************************/
   public Integer getPollIntervalMs()
   {
      return (this.pollIntervalMs);
   }



   /*******************************************************************************
    ** Setter for pollIntervalMs
    *******************************************************************************/
   public void setPollIntervalMs(Integer pollIntervalMs)
   {
      this.pollIntervalMs = pollIntervalMs;
   }



   /*******************************************************************************
    ** Fluent setter for pollIntervalMs
    *******************************************************************************/
   public EsbOutboxConfig withPollIntervalMs(Integer pollIntervalMs)
   {
      this.pollIntervalMs = pollIntervalMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for leaseMs: how long a relay's claim on a destination's rows lasts; another node may take them over after it expires.
    *******************************************************************************/
   public Integer getLeaseMs()
   {
      return (this.leaseMs);
   }



   /*******************************************************************************
    ** Setter for leaseMs
    *******************************************************************************/
   public void setLeaseMs(Integer leaseMs)
   {
      this.leaseMs = leaseMs;
   }



   /*******************************************************************************
    ** Fluent setter for leaseMs
    *******************************************************************************/
   public EsbOutboxConfig withLeaseMs(Integer leaseMs)
   {
      this.leaseMs = leaseMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxAttempts: how many failed sends (while the broker is connected) before a row is DEAD.  Sends that fail because the broker is unreachable do not count.
    *******************************************************************************/
   public Integer getMaxAttempts()
   {
      return (this.maxAttempts);
   }



   /*******************************************************************************
    ** Setter for maxAttempts
    *******************************************************************************/
   public void setMaxAttempts(Integer maxAttempts)
   {
      this.maxAttempts = maxAttempts;
   }



   /*******************************************************************************
    ** Fluent setter for maxAttempts
    *******************************************************************************/
   public EsbOutboxConfig withMaxAttempts(Integer maxAttempts)
   {
      this.maxAttempts = maxAttempts;
      return (this);
   }



   /*******************************************************************************
    ** Getter for initialBackoffMs: the wait after a row's first failed send; it doubles with each further failure.
    *******************************************************************************/
   public Integer getInitialBackoffMs()
   {
      return (this.initialBackoffMs);
   }



   /*******************************************************************************
    ** Setter for initialBackoffMs
    *******************************************************************************/
   public void setInitialBackoffMs(Integer initialBackoffMs)
   {
      this.initialBackoffMs = initialBackoffMs;
   }



   /*******************************************************************************
    ** Fluent setter for initialBackoffMs
    *******************************************************************************/
   public EsbOutboxConfig withInitialBackoffMs(Integer initialBackoffMs)
   {
      this.initialBackoffMs = initialBackoffMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxBackoffMs: the longest wait between a row's sends.
    *******************************************************************************/
   public Integer getMaxBackoffMs()
   {
      return (this.maxBackoffMs);
   }



   /*******************************************************************************
    ** Setter for maxBackoffMs
    *******************************************************************************/
   public void setMaxBackoffMs(Integer maxBackoffMs)
   {
      this.maxBackoffMs = maxBackoffMs;
   }



   /*******************************************************************************
    ** Fluent setter for maxBackoffMs
    *******************************************************************************/
   public EsbOutboxConfig withMaxBackoffMs(Integer maxBackoffMs)
   {
      this.maxBackoffMs = maxBackoffMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for retentionDays: how many days SENT rows are kept before the relay purges them (DEAD rows are kept until someone deletes them).
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
   public EsbOutboxConfig withRetentionDays(Integer retentionDays)
   {
      this.retentionDays = retentionDays;
      return (this);
   }



   /*******************************************************************************
    ** Getter for purgeIntervalMs: how often each node purges old SENT rows.
    *******************************************************************************/
   public Integer getPurgeIntervalMs()
   {
      return (this.purgeIntervalMs);
   }



   /*******************************************************************************
    ** Setter for purgeIntervalMs
    *******************************************************************************/
   public void setPurgeIntervalMs(Integer purgeIntervalMs)
   {
      this.purgeIntervalMs = purgeIntervalMs;
   }



   /*******************************************************************************
    ** Fluent setter for purgeIntervalMs
    *******************************************************************************/
   public EsbOutboxConfig withPurgeIntervalMs(Integer purgeIntervalMs)
   {
      this.purgeIntervalMs = purgeIntervalMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pendingAlertMs: the health check degrades when the oldest PENDING row is older than this.
    *******************************************************************************/
   public Integer getPendingAlertMs()
   {
      return (this.pendingAlertMs);
   }



   /*******************************************************************************
    ** Setter for pendingAlertMs
    *******************************************************************************/
   public void setPendingAlertMs(Integer pendingAlertMs)
   {
      this.pendingAlertMs = pendingAlertMs;
   }



   /*******************************************************************************
    ** Fluent setter for pendingAlertMs
    *******************************************************************************/
   public EsbOutboxConfig withPendingAlertMs(Integer pendingAlertMs)
   {
      this.pendingAlertMs = pendingAlertMs;
      return (this);
   }

}
