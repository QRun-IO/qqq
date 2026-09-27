/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.messaging;


/*******************************************************************************
 **
 *******************************************************************************/
public class Content
{
   private String      body;
   private ContentRole contentRole;



   /*******************************************************************************
    ** Getter for body
    *******************************************************************************/
   public String getBody()
   {
      return (this.body);
   }



   /*******************************************************************************
    ** Setter for body
    *******************************************************************************/
   public void setBody(String body)
   {
      this.body = body;
   }



   /*******************************************************************************
    ** Fluent setter for body
    *******************************************************************************/
   public Content withBody(String body)
   {
      this.body = body;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contentRole
    *******************************************************************************/
   public ContentRole getContentRole()
   {
      return (this.contentRole);
   }



   /*******************************************************************************
    ** Setter for contentRole
    *******************************************************************************/
   public void setContentRole(ContentRole contentRole)
   {
      this.contentRole = contentRole;
   }



   /*******************************************************************************
    ** Fluent setter for contentRole
    *******************************************************************************/
   public Content withContentRole(ContentRole contentRole)
   {
      this.contentRole = contentRole;
      return (this);
   }

}
