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

package com.kingsrook.qqq.backend.core.model.dashboard.widgets;


import java.io.Serializable;


/*******************************************************************************
 ** Model containing data required for a cron UI widget
 **
 *******************************************************************************/
public class CronUISetupData implements Serializable
{
   private String tableName;
   private String cronExpressionFieldName;
   private String timeZoneFieldName;



   /*******************************************************************************
    **
    *******************************************************************************/
   public CronUISetupData()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public CronUISetupData(String tableName, String cronExpressionFieldName, String timeZoneFieldName)
   {
      this.tableName = tableName;
      this.cronExpressionFieldName = cronExpressionFieldName;
      this.timeZoneFieldName = timeZoneFieldName;
   }



   /*******************************************************************************
    * Getter for cronExpressionFieldName
    * @see #withCronExpressionFieldName(String)
    *******************************************************************************/
   public String getCronExpressionFieldName()
   {
      return (this.cronExpressionFieldName);
   }



   /*******************************************************************************
    * Setter for cronExpressionFieldName
    * @see #withCronExpressionFieldName(String)
    *******************************************************************************/
   public void setCronExpressionFieldName(String cronExpressionFieldName)
   {
      this.cronExpressionFieldName = cronExpressionFieldName;
   }



   /*******************************************************************************
    * Fluent setter for cronExpressionFieldName
    *
    * @param cronExpressionFieldName
    * The field in the table that stores the cron expression
    * @return this
    *******************************************************************************/
   public CronUISetupData withCronExpressionFieldName(String cronExpressionFieldName)
   {
      this.cronExpressionFieldName = cronExpressionFieldName;
      return (this);
   }



   /*******************************************************************************
    * Getter for timeZoneFieldName
    * @see #withTimeZoneFieldName(String)
    *******************************************************************************/
   public String getTimeZoneFieldName()
   {
      return (this.timeZoneFieldName);
   }



   /*******************************************************************************
    * Setter for timeZoneFieldName
    * @see #withTimeZoneFieldName(String)
    *******************************************************************************/
   public void setTimeZoneFieldName(String timeZoneFieldName)
   {
      this.timeZoneFieldName = timeZoneFieldName;
   }



   /*******************************************************************************
    * Fluent setter for timeZoneFieldName
    *
    * @param timeZoneFieldName
    * Optional - the field in the table that stores a time zone id
    * @return this
    *******************************************************************************/
   public CronUISetupData withTimeZoneFieldName(String timeZoneFieldName)
   {
      this.timeZoneFieldName = timeZoneFieldName;
      return (this);
   }



   /*******************************************************************************
    * Getter for tableName
    * @see #withTableName(String)
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    * Setter for tableName
    * @see #withTableName(String)
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    * Fluent setter for tableName
    *
    * @param tableName
    * Name of the table that the widget is applied to.
    * @return this
    *******************************************************************************/
   public CronUISetupData withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }

}
