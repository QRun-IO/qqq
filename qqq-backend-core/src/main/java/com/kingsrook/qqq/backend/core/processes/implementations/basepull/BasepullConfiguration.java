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

package com.kingsrook.qqq.backend.core.processes.implementations.basepull;


import java.io.Serializable;


/*******************************************************************************
 ** Class for storing all basepull configuration data
 **
 *******************************************************************************/
public class BasepullConfiguration implements Serializable
{
   private String tableName; // the table that stores the basepull timestamps
   private String keyField; // the field in the basepull timestamps table that stores the key of the basepull (e.g., a process name)
   private String keyValue; // the key applied to the keyField - optional - if not set, process.getName is used.

   private String  lastRunTimeFieldName; // the field in the basepull timestamps table that stores the last-run time for the job.
   private Integer hoursBackForInitialTimestamp; // for the first-run use-case (where there is no row in the timestamps table), how many hours back in time to look.

   private String timestampField; // the name of the field in the table being queried against the last-run timestamp.

   private Integer secondsToSubtractFromLastRunTimeForTimestampQuery; // option to adjust the query's start-time (based on last run time) by a number of seconds.
   private Integer secondsToSubtractFromThisRunTimeForTimestampQuery; // option to adjust the query's end-time (based on this run time) by a number of seconds.



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    **
    *******************************************************************************/
   public BasepullConfiguration withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for keyField
    **
    *******************************************************************************/
   public String getKeyField()
   {
      return keyField;
   }



   /*******************************************************************************
    ** Setter for keyField
    **
    *******************************************************************************/
   public void setKeyField(String keyField)
   {
      this.keyField = keyField;
   }



   /*******************************************************************************
    ** Fluent setter for keyField
    **
    *******************************************************************************/
   public BasepullConfiguration withKeyField(String keyField)
   {
      this.keyField = keyField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for keyValue
    **
    *******************************************************************************/
   public String getKeyValue()
   {
      return keyValue;
   }



   /*******************************************************************************
    ** Setter for keyValue
    **
    *******************************************************************************/
   public void setKeyValue(String keyValue)
   {
      this.keyValue = keyValue;
   }



   /*******************************************************************************
    ** Fluent setter for keyValue
    **
    *******************************************************************************/
   public BasepullConfiguration withKeyValue(String keyValue)
   {
      this.keyValue = keyValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for lastRunTimeFieldName
    **
    *******************************************************************************/
   public String getLastRunTimeFieldName()
   {
      return lastRunTimeFieldName;
   }



   /*******************************************************************************
    ** Setter for lastRunTimeFieldName
    **
    *******************************************************************************/
   public void setLastRunTimeFieldName(String lastRunTimeFieldName)
   {
      this.lastRunTimeFieldName = lastRunTimeFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for lastRunTimeFieldName
    **
    *******************************************************************************/
   public BasepullConfiguration withLastRunTimeFieldName(String lastRunTimeFieldName)
   {
      this.lastRunTimeFieldName = lastRunTimeFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hoursBackForInitialTimestamp
    **
    *******************************************************************************/
   public Integer getHoursBackForInitialTimestamp()
   {
      return hoursBackForInitialTimestamp;
   }



   /*******************************************************************************
    ** Setter for hoursBackForInitialTimestamp
    **
    *******************************************************************************/
   public void setHoursBackForInitialTimestamp(Integer hoursBackForInitialTimestamp)
   {
      this.hoursBackForInitialTimestamp = hoursBackForInitialTimestamp;
   }



   /*******************************************************************************
    ** Fluent setter for hoursBackForInitialTimestamp
    **
    *******************************************************************************/
   public BasepullConfiguration withHoursBackForInitialTimestamp(Integer hoursBackForInitialTimestamp)
   {
      this.hoursBackForInitialTimestamp = hoursBackForInitialTimestamp;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timestampField
    **
    *******************************************************************************/
   public String getTimestampField()
   {
      return timestampField;
   }



   /*******************************************************************************
    ** Setter for timestampField
    **
    *******************************************************************************/
   public void setTimestampField(String timestampField)
   {
      this.timestampField = timestampField;
   }



   /*******************************************************************************
    ** Fluent setter for timestampField
    **
    *******************************************************************************/
   public BasepullConfiguration withTimestampField(String timestampField)
   {
      this.timestampField = timestampField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for secondsToSubtractFromLastRunTimeForTimestampQuery
    *******************************************************************************/
   public Integer getSecondsToSubtractFromLastRunTimeForTimestampQuery()
   {
      return (this.secondsToSubtractFromLastRunTimeForTimestampQuery);
   }



   /*******************************************************************************
    ** Setter for secondsToSubtractFromLastRunTimeForTimestampQuery
    *******************************************************************************/
   public void setSecondsToSubtractFromLastRunTimeForTimestampQuery(Integer secondsToSubtractFromLastRunTimeForTimestampQuery)
   {
      this.secondsToSubtractFromLastRunTimeForTimestampQuery = secondsToSubtractFromLastRunTimeForTimestampQuery;
   }



   /*******************************************************************************
    ** Fluent setter for secondsToSubtractFromLastRunTimeForTimestampQuery
    *******************************************************************************/
   public BasepullConfiguration withSecondsToSubtractFromLastRunTimeForTimestampQuery(Integer secondsToSubtractFromLastRunTimeForTimestampQuery)
   {
      this.secondsToSubtractFromLastRunTimeForTimestampQuery = secondsToSubtractFromLastRunTimeForTimestampQuery;
      return (this);
   }



   /*******************************************************************************
    ** Getter for secondsToSubtractFromThisRunTimeForTimestampQuery
    *******************************************************************************/
   public Integer getSecondsToSubtractFromThisRunTimeForTimestampQuery()
   {
      return (this.secondsToSubtractFromThisRunTimeForTimestampQuery);
   }



   /*******************************************************************************
    ** Setter for secondsToSubtractFromThisRunTimeForTimestampQuery
    *******************************************************************************/
   public void setSecondsToSubtractFromThisRunTimeForTimestampQuery(Integer secondsToSubtractFromThisRunTimeForTimestampQuery)
   {
      this.secondsToSubtractFromThisRunTimeForTimestampQuery = secondsToSubtractFromThisRunTimeForTimestampQuery;
   }



   /*******************************************************************************
    ** Fluent setter for secondsToSubtractFromThisRunTimeForTimestampQuery
    *******************************************************************************/
   public BasepullConfiguration withSecondsToSubtractFromThisRunTimeForTimestampQuery(Integer secondsToSubtractFromThisRunTimeForTimestampQuery)
   {
      this.secondsToSubtractFromThisRunTimeForTimestampQuery = secondsToSubtractFromThisRunTimeForTimestampQuery;
      return (this);
   }

}
