/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors.io;




/*******************************************************************************
    ** HelpCoverageMiddlewareInput contract.
    *******************************************************************************/
public class HelpCoverageMiddlewareInput extends AbstractMiddlewareInput
{
   private String kind;
   private boolean missingOnly;
   private String parentKey;



   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   public String getKind()
   {
      return (this.kind);
   }



   /*******************************************************************************
    ** Setter for kind
    *******************************************************************************/
   public void setKind(String kind)
   {
      this.kind = kind;
   }



   /*******************************************************************************
    ** Fluent setter for kind
    *******************************************************************************/
   public HelpCoverageMiddlewareInput withKind(String kind)
   {
      this.kind = kind;
      return (this);
   }



   /*******************************************************************************
    ** Getter for missingOnly
    *******************************************************************************/
   public boolean getMissingOnly()
   {
      return (this.missingOnly);
   }



   /*******************************************************************************
    ** Setter for missingOnly
    *******************************************************************************/
   public void setMissingOnly(boolean missingOnly)
   {
      this.missingOnly = missingOnly;
   }



   /*******************************************************************************
    ** Fluent setter for missingOnly
    *******************************************************************************/
   public HelpCoverageMiddlewareInput withMissingOnly(boolean missingOnly)
   {
      this.missingOnly = missingOnly;
      return (this);
   }



   /*******************************************************************************
    ** Getter for parentKey
    *******************************************************************************/
   public String getParentKey()
   {
      return (this.parentKey);
   }



   /*******************************************************************************
    ** Setter for parentKey
    *******************************************************************************/
   public void setParentKey(String parentKey)
   {
      this.parentKey = parentKey;
   }



   /*******************************************************************************
    ** Fluent setter for parentKey
    *******************************************************************************/
   public HelpCoverageMiddlewareInput withParentKey(String parentKey)
   {
      this.parentKey = parentKey;
      return (this);
   }
}
