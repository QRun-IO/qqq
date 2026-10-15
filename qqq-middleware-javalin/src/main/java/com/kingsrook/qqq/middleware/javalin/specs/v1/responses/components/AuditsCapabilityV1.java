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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** A record's audit history.  Read it with processName (preferred) or, without it, by\nquerying tableName joined to detailTableName.  Since UI API 1.6.0.
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditsCapabilityV1 implements ToSchema
{
   @OpenAPIDescription("True when the table is audited and the user can read its audits one of the two ways.")
   private Boolean available;

   @OpenAPIDescription("The table's audit level: NONE, RECORD or FIELD.")
   private String  level;

   @OpenAPIDescription("The process that returns a record's audits (inputs tableName, recordId); absent when the user may not run it.")
   private String  processName;

   @OpenAPIDescription("The audit table, when the user may query it directly.")
   private String  tableName;

   @OpenAPIDescription("The audit detail table (field changes), joined to tableName.")
   private String  detailTableName;



   /*******************************************************************************
    ** Getter for available
    *******************************************************************************/
   public Boolean getAvailable()
   {
      return (this.available);
   }



   /*******************************************************************************
    ** Fluent setter for available
    *******************************************************************************/
   public AuditsCapabilityV1 withAvailable(Boolean available)
   {
      this.available = available;
      return (this);
   }



   /*******************************************************************************
    ** Getter for level
    *******************************************************************************/
   public String getLevel()
   {
      return (this.level);
   }



   /*******************************************************************************
    ** Fluent setter for level
    *******************************************************************************/
   public AuditsCapabilityV1 withLevel(String level)
   {
      this.level = level;
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
    ** Fluent setter for processName
    *******************************************************************************/
   public AuditsCapabilityV1 withProcessName(String processName)
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
    ** Fluent setter for tableName
    *******************************************************************************/
   public AuditsCapabilityV1 withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for detailTableName
    *******************************************************************************/
   public String getDetailTableName()
   {
      return (this.detailTableName);
   }



   /*******************************************************************************
    ** Fluent setter for detailTableName
    *******************************************************************************/
   public AuditsCapabilityV1 withDetailTableName(String detailTableName)
   {
      this.detailTableName = detailTableName;
      return (this);
   }
}
