/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
