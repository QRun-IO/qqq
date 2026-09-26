/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableVariant;


/*******************************************************************************
 **
 *******************************************************************************/
public class ProcessStatusInput extends AbstractMiddlewareInput
{
   private String processName;
   private String processUUID;
   private String jobUUID;

   private TableVariant tableVariant;



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Setter for processName
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public ProcessStatusInput withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processUUID
    *******************************************************************************/
   public String getProcessUUID()
   {
      return (this.processUUID);
   }



   /*******************************************************************************
    ** Setter for processUUID
    *******************************************************************************/
   public void setProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
   }



   /*******************************************************************************
    ** Fluent setter for processUUID
    *******************************************************************************/
   public ProcessStatusInput withProcessUUID(String processUUID)
   {
      this.processUUID = processUUID;
      return (this);
   }



   /*******************************************************************************
    ** Getter for jobUUID
    *******************************************************************************/
   public String getJobUUID()
   {
      return (this.jobUUID);
   }



   /*******************************************************************************
    ** Setter for jobUUID
    *******************************************************************************/
   public void setJobUUID(String jobUUID)
   {
      this.jobUUID = jobUUID;
   }



   /*******************************************************************************
    ** Fluent setter for jobUUID
    *******************************************************************************/
   public ProcessStatusInput withJobUUID(String jobUUID)
   {
      this.jobUUID = jobUUID;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableVariant
    *******************************************************************************/
   public TableVariant getTableVariant()
   {
      return (this.tableVariant);
   }



   /*******************************************************************************
    ** Setter for tableVariant
    *******************************************************************************/
   public void setTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
   }



   /*******************************************************************************
    ** Fluent setter for tableVariant
    *******************************************************************************/
   public ProcessStatusInput withTableVariant(TableVariant tableVariant)
   {
      this.tableVariant = tableVariant;
      return (this);
   }

}
