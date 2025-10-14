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


/*******************************************************************************
 ** Model containing data required for a cron UI widget
 **
 *******************************************************************************/
public class CronUIWidgetData extends QWidgetData
{
   private String cronDescription;
   private String error;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public CronUIWidgetData()
   {
   }



   /*******************************************************************************
    ** Getter for type
    **
    *******************************************************************************/
   @Override
   public String getType()
   {
      return WidgetType.CRON_UI.getType();
   }



   /*******************************************************************************
    * Getter for cronDescription
    * @see #withCronDescription(String)
    *******************************************************************************/
   public String getCronDescription()
   {
      return (this.cronDescription);
   }


   /*******************************************************************************
    * Setter for cronDescription
    * @see #withCronDescription(String)
    *******************************************************************************/
   public void setCronDescription(String cronDescription)
   {
      this.cronDescription = cronDescription;
   }



   /*******************************************************************************
    * Fluent setter for cronDescription
    *
    * @param cronDescription
    * Human-readable string describing the cron expression
    * @return this
    *******************************************************************************/
   public CronUIWidgetData withCronDescription(String cronDescription)
   {
      this.cronDescription = cronDescription;
      return (this);
   }


   /*******************************************************************************
    * Getter for error
    * @see #withError(String)
    *******************************************************************************/
   public String getError()
   {
      return (this.error);
   }



   /*******************************************************************************
    * Setter for error
    * @see #withError(String)
    *******************************************************************************/
   public void setError(String error)
   {
      this.error = error;
   }



   /*******************************************************************************
    * Fluent setter for error
    *
    * @param error
    * Error message, if the cron string isn't valid.
    * @return this
    *******************************************************************************/
   public CronUIWidgetData withError(String error)
   {
      this.error = error;
      return (this);
   }


}
