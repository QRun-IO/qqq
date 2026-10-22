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


/*******************************************************************************
 ** Whether one AI tool applies to a table (or record) for a user, and why not.
 *******************************************************************************/
public class AiToolApplicability implements Serializable
{
   private String     toolName;
   private AiToolKind kind;
   private Boolean    applies;
   private String     reason;


   /*******************************************************************************
    ** Getter for toolName
    *******************************************************************************/
   public String getToolName()
   {
      return (this.toolName);
   }



   /*******************************************************************************
    ** Setter for toolName
    *******************************************************************************/
   public void setToolName(String toolName)
   {
      this.toolName = toolName;
   }



   /*******************************************************************************
    ** Fluent setter for toolName
    *******************************************************************************/
   public AiToolApplicability withToolName(String toolName)
   {
      this.toolName = toolName;
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
   public AiToolApplicability withKind(AiToolKind kind)
   {
      this.kind = kind;
      return (this);
   }



   /*******************************************************************************
    ** Getter for applies
    *******************************************************************************/
   public Boolean getApplies()
   {
      return (this.applies);
   }



   /*******************************************************************************
    ** Setter for applies
    *******************************************************************************/
   public void setApplies(Boolean applies)
   {
      this.applies = applies;
   }



   /*******************************************************************************
    ** Fluent setter for applies
    *******************************************************************************/
   public AiToolApplicability withApplies(Boolean applies)
   {
      this.applies = applies;
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
   public AiToolApplicability withReason(String reason)
   {
      this.reason = reason;
      return (this);
   }
}
