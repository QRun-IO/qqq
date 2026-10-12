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

package com.kingsrook.qqq.backend.core.model.developer;


import java.io.Serializable;


/*******************************************************************************
 ** A process the frontend may launch for the record, in an ACTIONS
 ** {@link DeveloperPanelBlock}.  Providers only add actions the current user has
 ** permission to run; the process itself still enforces its own permissions.
 *******************************************************************************/
public class DeveloperPanelAction implements Serializable
{
   /** Button text. */
   private String label;

   /** The process to launch. */
   private String processName;

   /** Table the process runs against (defaults to the record's table). */
   private String tableName;

   /** Launch the process with this record selected (default true). */
   private Boolean includeRecordId;

   /** Optional explanation shown with the action. */
   private String description;



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public DeveloperPanelAction withLabel(String label)
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
    ** Setter for processName
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public DeveloperPanelAction withProcessName(String processName)
   {
      this.processName = processName;
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
   public DeveloperPanelAction withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for includeRecordId
    *******************************************************************************/
   public Boolean getIncludeRecordId()
   {
      return (this.includeRecordId);
   }



   /*******************************************************************************
    ** Setter for includeRecordId
    *******************************************************************************/
   public void setIncludeRecordId(Boolean includeRecordId)
   {
      this.includeRecordId = includeRecordId;
   }



   /*******************************************************************************
    ** Fluent setter for includeRecordId
    *******************************************************************************/
   public DeveloperPanelAction withIncludeRecordId(Boolean includeRecordId)
   {
      this.includeRecordId = includeRecordId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public DeveloperPanelAction withDescription(String description)
   {
      this.description = description;
      return (this);
   }

}
