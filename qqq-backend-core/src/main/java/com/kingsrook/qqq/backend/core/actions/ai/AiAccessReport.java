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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** What the AI tool kernel may do with a table (and optionally one record) for
 ** one user - built by {@link AiToolKernel#describeAccess}, e.g. for the record
 ** Developer screen. Field entries map the shared FieldAccessHelper decisions.
 ** Permission-scoped: for a table the user can't see (or
 ** read), or a record the user can't read, it says "not available" and holds
 ** nothing else about the table or record.
 *******************************************************************************/
public class AiAccessReport implements Serializable
{
   private String                    tableName;
   private String                    tableLabel;
   private Serializable              primaryKey;
   private String                    recordLabel;
   private Boolean                   recordReadable;
   private AiExposure                tableExposure;
   private String                    reason;
   private List<AiToolApplicability> tools                = new ArrayList<>();
   private List<AiFieldAccess>       fields               = new ArrayList<>();
   private Map<String, Boolean>      userTablePermissions = new LinkedHashMap<>();


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
   public AiAccessReport withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableLabel
    *******************************************************************************/
   public String getTableLabel()
   {
      return (this.tableLabel);
   }



   /*******************************************************************************
    ** Setter for tableLabel
    *******************************************************************************/
   public void setTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
   }



   /*******************************************************************************
    ** Fluent setter for tableLabel
    *******************************************************************************/
   public AiAccessReport withTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for primaryKey
    *******************************************************************************/
   public Serializable getPrimaryKey()
   {
      return (this.primaryKey);
   }



   /*******************************************************************************
    ** Setter for primaryKey
    *******************************************************************************/
   public void setPrimaryKey(Serializable primaryKey)
   {
      this.primaryKey = primaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for primaryKey
    *******************************************************************************/
   public AiAccessReport withPrimaryKey(Serializable primaryKey)
   {
      this.primaryKey = primaryKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordLabel
    *******************************************************************************/
   public String getRecordLabel()
   {
      return (this.recordLabel);
   }



   /*******************************************************************************
    ** Setter for recordLabel
    *******************************************************************************/
   public void setRecordLabel(String recordLabel)
   {
      this.recordLabel = recordLabel;
   }



   /*******************************************************************************
    ** Fluent setter for recordLabel
    *******************************************************************************/
   public AiAccessReport withRecordLabel(String recordLabel)
   {
      this.recordLabel = recordLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordReadable
    *******************************************************************************/
   public Boolean getRecordReadable()
   {
      return (this.recordReadable);
   }



   /*******************************************************************************
    ** Setter for recordReadable
    *******************************************************************************/
   public void setRecordReadable(Boolean recordReadable)
   {
      this.recordReadable = recordReadable;
   }



   /*******************************************************************************
    ** Fluent setter for recordReadable
    *******************************************************************************/
   public AiAccessReport withRecordReadable(Boolean recordReadable)
   {
      this.recordReadable = recordReadable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableExposure
    *******************************************************************************/
   public AiExposure getTableExposure()
   {
      return (this.tableExposure);
   }



   /*******************************************************************************
    ** Setter for tableExposure
    *******************************************************************************/
   public void setTableExposure(AiExposure tableExposure)
   {
      this.tableExposure = tableExposure;
   }



   /*******************************************************************************
    ** Fluent setter for tableExposure
    *******************************************************************************/
   public AiAccessReport withTableExposure(AiExposure tableExposure)
   {
      this.tableExposure = tableExposure;
      return (this);
   }



   /*******************************************************************************
    ** Getter for reason
    *******************************************************************************/
   public String getReason()
   {
      return (this.reason);
   }



   /*******************************************************************************
    ** Setter for reason
    *******************************************************************************/
   public void setReason(String reason)
   {
      this.reason = reason;
   }



   /*******************************************************************************
    ** Fluent setter for reason
    *******************************************************************************/
   public AiAccessReport withReason(String reason)
   {
      this.reason = reason;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tools
    *******************************************************************************/
   public List<AiToolApplicability> getTools()
   {
      return (this.tools);
   }



   /*******************************************************************************
    ** Setter for tools
    *******************************************************************************/
   public void setTools(List<AiToolApplicability> tools)
   {
      this.tools = tools;
   }



   /*******************************************************************************
    ** Fluent setter for tools
    *******************************************************************************/
   public AiAccessReport withTools(List<AiToolApplicability> tools)
   {
      this.tools = tools;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fields
    *******************************************************************************/
   public List<AiFieldAccess> getFields()
   {
      return (this.fields);
   }



   /*******************************************************************************
    ** Setter for fields
    *******************************************************************************/
   public void setFields(List<AiFieldAccess> fields)
   {
      this.fields = fields;
   }



   /*******************************************************************************
    ** Fluent setter for fields
    *******************************************************************************/
   public AiAccessReport withFields(List<AiFieldAccess> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    ** Getter for userTablePermissions
    *******************************************************************************/
   public Map<String, Boolean> getUserTablePermissions()
   {
      return (this.userTablePermissions);
   }



   /*******************************************************************************
    ** Setter for userTablePermissions
    *******************************************************************************/
   public void setUserTablePermissions(Map<String, Boolean> userTablePermissions)
   {
      this.userTablePermissions = userTablePermissions;
   }



   /*******************************************************************************
    ** Fluent setter for userTablePermissions
    *******************************************************************************/
   public AiAccessReport withUserTablePermissions(Map<String, Boolean> userTablePermissions)
   {
      this.userTablePermissions = userTablePermissions;
      return (this);
   }
}
