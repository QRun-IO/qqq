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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/*******************************************************************************
 ** Value-free audit notification. Primary key samples contain at most 50 keys.
 *******************************************************************************/
public class FieldAccessEvent
{
   private FieldAccessEventType type;
   private String tableName;
   private List<String> fieldNames;
   private List<Serializable> primaryKeys;
   private Integer recordCount;
   private String actionName;
   private FieldAccessOutcome outcome;
   private List<String> searchValueFingerprints;



   /*******************************************************************************
    ** Getter for type
    *******************************************************************************/
   public FieldAccessEventType getType()
   {
      return (this.type);
   }



   /*******************************************************************************
    ** Setter for type
    *******************************************************************************/
   public void setType(FieldAccessEventType type)
   {
      this.type = type;
   }



   /*******************************************************************************
    ** Fluent setter for type
    *******************************************************************************/
   public FieldAccessEvent withType(FieldAccessEventType type)
   {
      setType(type);
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
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public FieldAccessEvent withTableName(String tableName)
   {
      setTableName(tableName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldNames
    *******************************************************************************/
   public List<String> getFieldNames()
   {
      return (this.fieldNames);
   }



   /*******************************************************************************
    ** Setter for fieldNames
    *******************************************************************************/
   public void setFieldNames(List<String> fieldNames)
   {
      this.fieldNames = fieldNames == null ? null : new ArrayList<>(fieldNames);
   }



   /*******************************************************************************
    ** Fluent setter for fieldNames
    *******************************************************************************/
   public FieldAccessEvent withFieldNames(List<String> fieldNames)
   {
      setFieldNames(fieldNames);
      return (this);
   }



   /*******************************************************************************
    ** Getter for primaryKeys
    *******************************************************************************/
   public List<Serializable> getPrimaryKeys()
   {
      return (this.primaryKeys == null ? null : Collections.unmodifiableList(this.primaryKeys));
   }



   /*******************************************************************************
    ** Setter for primaryKeys
    *******************************************************************************/
   public void setPrimaryKeys(List<Serializable> primaryKeys)
   {
      this.primaryKeys = primaryKeys == null ? null : new ArrayList<>(primaryKeys.subList(0, Math.min(50, primaryKeys.size())));
   }



   /*******************************************************************************
    ** Fluent setter for primaryKeys
    *******************************************************************************/
   public FieldAccessEvent withPrimaryKeys(List<Serializable> primaryKeys)
   {
      setPrimaryKeys(primaryKeys);
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordCount
    *******************************************************************************/
   public Integer getRecordCount()
   {
      return (this.recordCount);
   }



   /*******************************************************************************
    ** Setter for recordCount
    *******************************************************************************/
   public void setRecordCount(Integer recordCount)
   {
      this.recordCount = recordCount;
   }



   /*******************************************************************************
    ** Fluent setter for recordCount
    *******************************************************************************/
   public FieldAccessEvent withRecordCount(Integer recordCount)
   {
      setRecordCount(recordCount);
      return (this);
   }



   /*******************************************************************************
    ** Getter for actionName
    *******************************************************************************/
   public String getActionName()
   {
      return (this.actionName);
   }



   /*******************************************************************************
    ** Setter for actionName
    *******************************************************************************/
   public void setActionName(String actionName)
   {
      this.actionName = actionName;
   }



   /*******************************************************************************
    ** Fluent setter for actionName
    *******************************************************************************/
   public FieldAccessEvent withActionName(String actionName)
   {
      setActionName(actionName);
      return (this);
   }



   /*******************************************************************************
    ** Getter for outcome
    *******************************************************************************/
   public FieldAccessOutcome getOutcome()
   {
      return (outcome);
   }



   /*******************************************************************************
    ** Fluent setter for outcome
    *******************************************************************************/
   public FieldAccessEvent withOutcome(FieldAccessOutcome outcome)
   {
      this.outcome = outcome;
      return (this);
   }



   /*******************************************************************************
    ** Getter for searchValueFingerprints
    *******************************************************************************/
   public List<String> getSearchValueFingerprints()
   {
      return (searchValueFingerprints);
   }



   /*******************************************************************************
    ** Fluent setter for searchValueFingerprints
    *******************************************************************************/
   public FieldAccessEvent withSearchValueFingerprints(List<String> searchValueFingerprints)
   {
      this.searchValueFingerprints = searchValueFingerprints == null ? null : List.copyOf(searchValueFingerprints);
      return (this);
   }
}
