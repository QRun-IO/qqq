/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.tables.automation;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;


/*******************************************************************************
 ** Details about how this table's record automations are set up.
 *******************************************************************************/
public class QTableAutomationDetails implements QMetaDataObject, Cloneable
{
   private AutomationStatusTracking    statusTracking;
   private String                      providerName;
   private List<TableAutomationAction> actions;

   private Integer overrideBatchSize;

   private QScheduleMetaData schedule;

   private String shardByFieldName; // field in "this" table, to use for sharding
   private String shardSourceTableName; // name of the table where the shards are defined as rows
   private String shardLabelFieldName; // field in shard-source-table to use for labeling shards
   private String shardIdFieldName; // field in shard-source-table to identify shards (e.g., joins to this table's shardByFieldName)



   /*******************************************************************************
    ** Getter for statusTracking
    **
    *******************************************************************************/
   public AutomationStatusTracking getStatusTracking()
   {
      return statusTracking;
   }



   /*******************************************************************************
    ** Setter for statusTracking
    **
    *******************************************************************************/
   public void setStatusTracking(AutomationStatusTracking statusTracking)
   {
      this.statusTracking = statusTracking;
   }



   /*******************************************************************************
    ** Fluent setter for statusTracking
    **
    *******************************************************************************/
   public QTableAutomationDetails withStatusTracking(AutomationStatusTracking statusTracking)
   {
      this.statusTracking = statusTracking;
      return (this);
   }



   /*******************************************************************************
    ** Getter for providerName
    **
    *******************************************************************************/
   public String getProviderName()
   {
      return providerName;
   }



   /*******************************************************************************
    ** Setter for providerName
    **
    *******************************************************************************/
   public void setProviderName(String providerName)
   {
      this.providerName = providerName;
   }



   /*******************************************************************************
    ** Fluent setter for providerName
    **
    *******************************************************************************/
   public QTableAutomationDetails withProviderName(String providerName)
   {
      this.providerName = providerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for actions
    **
    *******************************************************************************/
   public List<TableAutomationAction> getActions()
   {
      return actions;
   }



   /*******************************************************************************
    ** Setter for actions
    **
    *******************************************************************************/
   public void setActions(List<TableAutomationAction> actions)
   {
      this.actions = actions;
   }



   /*******************************************************************************
    ** Fluent setter for actions
    **
    *******************************************************************************/
   public QTableAutomationDetails withActions(List<TableAutomationAction> actions)
   {
      this.actions = actions;
      return (this);
   }



   /*******************************************************************************
    ** Fluently add an action to this table's automations.
    *******************************************************************************/
   public QTableAutomationDetails withAction(TableAutomationAction action)
   {
      if(this.actions == null)
      {
         this.actions = new ArrayList<>();
      }
      this.actions.add(action);
      return (this);
   }



   /*******************************************************************************
    ** Getter for overrideBatchSize
    **
    *******************************************************************************/
   public Integer getOverrideBatchSize()
   {
      return overrideBatchSize;
   }



   /*******************************************************************************
    ** Setter for overrideBatchSize
    **
    *******************************************************************************/
   public void setOverrideBatchSize(Integer overrideBatchSize)
   {
      this.overrideBatchSize = overrideBatchSize;
   }



   /*******************************************************************************
    ** Fluent setter for overrideBatchSize
    **
    *******************************************************************************/
   public QTableAutomationDetails withOverrideBatchSize(Integer overrideBatchSize)
   {
      this.overrideBatchSize = overrideBatchSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for shardByFieldName
    *******************************************************************************/
   public String getShardByFieldName()
   {
      return (this.shardByFieldName);
   }



   /*******************************************************************************
    ** Setter for shardByFieldName
    *******************************************************************************/
   public void setShardByFieldName(String shardByFieldName)
   {
      this.shardByFieldName = shardByFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for shardByFieldName
    *******************************************************************************/
   public QTableAutomationDetails withShardByFieldName(String shardByFieldName)
   {
      this.shardByFieldName = shardByFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for shardSourceTableName
    *******************************************************************************/
   public String getShardSourceTableName()
   {
      return (this.shardSourceTableName);
   }



   /*******************************************************************************
    ** Setter for shardSourceTableName
    *******************************************************************************/
   public void setShardSourceTableName(String shardSourceTableName)
   {
      this.shardSourceTableName = shardSourceTableName;
   }



   /*******************************************************************************
    ** Fluent setter for shardSourceTableName
    *******************************************************************************/
   public QTableAutomationDetails withShardSourceTableName(String shardSourceTableName)
   {
      this.shardSourceTableName = shardSourceTableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for shardLabelFieldName
    *******************************************************************************/
   public String getShardLabelFieldName()
   {
      return (this.shardLabelFieldName);
   }



   /*******************************************************************************
    ** Setter for shardLabelFieldName
    *******************************************************************************/
   public void setShardLabelFieldName(String shardLabelFieldName)
   {
      this.shardLabelFieldName = shardLabelFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for shardLabelFieldName
    *******************************************************************************/
   public QTableAutomationDetails withShardLabelFieldName(String shardLabelFieldName)
   {
      this.shardLabelFieldName = shardLabelFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for shardIdFieldName
    *******************************************************************************/
   public String getShardIdFieldName()
   {
      return (this.shardIdFieldName);
   }



   /*******************************************************************************
    ** Setter for shardIdFieldName
    *******************************************************************************/
   public void setShardIdFieldName(String shardIdFieldName)
   {
      this.shardIdFieldName = shardIdFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for shardIdFieldName
    *******************************************************************************/
   public QTableAutomationDetails withShardIdFieldName(String shardIdFieldName)
   {
      this.shardIdFieldName = shardIdFieldName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for schedule
    *******************************************************************************/
   public QScheduleMetaData getSchedule()
   {
      return (this.schedule);
   }



   /*******************************************************************************
    ** Setter for schedule
    *******************************************************************************/
   public void setSchedule(QScheduleMetaData schedule)
   {
      this.schedule = schedule;
   }



   /*******************************************************************************
    ** Fluent setter for schedule
    *******************************************************************************/
   public QTableAutomationDetails withSchedule(QScheduleMetaData schedule)
   {
      this.schedule = schedule;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public QTableAutomationDetails clone()
   {
      try
      {
         QTableAutomationDetails clone = (QTableAutomationDetails) super.clone();

         if(statusTracking != null)
         {
            clone.statusTracking = statusTracking.clone();
         }

         if(actions != null)
         {
            clone.actions = new ArrayList<>();
            for(TableAutomationAction action : actions)
            {
               clone.actions.add(action.clone());
            }
         }

         if(schedule != null)
         {
            clone.schedule = schedule.clone();
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
