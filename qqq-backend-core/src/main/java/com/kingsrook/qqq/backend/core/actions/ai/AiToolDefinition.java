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


import java.util.Map;


/*******************************************************************************
 ** Definition of one AI tool, as advertised to a model: its wire name, a
 ** description, its kind, and a strict JSON Schema for its input (an object
 ** schema with additionalProperties=false and a required list).
 **
 ** Definitions are the same for every user (permissions are applied when a tool
 ** runs, not by changing the advertised list), so they can sit in a cached
 ** prompt prefix.
 *******************************************************************************/
public class AiToolDefinition
{
   private String              name;
   private String              description;
   private AiToolKind          kind;
   private Boolean             strict = true;
   private Map<String, Object> inputSchema;


   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public AiToolDefinition withName(String name)
   {
      this.name = name;
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
   public AiToolDefinition withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   public AiToolKind getKind()
   {
      return (this.kind);
   }



   /*******************************************************************************
    ** Setter for kind
    *******************************************************************************/
   public void setKind(AiToolKind kind)
   {
      this.kind = kind;
   }



   /*******************************************************************************
    ** Fluent setter for kind
    *******************************************************************************/
   public AiToolDefinition withKind(AiToolKind kind)
   {
      this.kind = kind;
      return (this);
   }



   /*******************************************************************************
    ** Getter for strict
    *******************************************************************************/
   public Boolean getStrict()
   {
      return (this.strict);
   }



   /*******************************************************************************
    ** Setter for strict
    *******************************************************************************/
   public void setStrict(Boolean strict)
   {
      this.strict = strict;
   }



   /*******************************************************************************
    ** Fluent setter for strict
    *******************************************************************************/
   public AiToolDefinition withStrict(Boolean strict)
   {
      this.strict = strict;
      return (this);
   }



   /*******************************************************************************
    ** Getter for inputSchema
    *******************************************************************************/
   public Map<String, Object> getInputSchema()
   {
      return (this.inputSchema);
   }



   /*******************************************************************************
    ** Setter for inputSchema
    *******************************************************************************/
   public void setInputSchema(Map<String, Object> inputSchema)
   {
      this.inputSchema = inputSchema;
   }



   /*******************************************************************************
    ** Fluent setter for inputSchema
    *******************************************************************************/
   public AiToolDefinition withInputSchema(Map<String, Object> inputSchema)
   {
      this.inputSchema = inputSchema;
      return (this);
   }
}
