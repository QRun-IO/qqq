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

package com.kingsrook.qqq.backend.core.model.actions.tables.search;


import java.io.Serializable;


/*******************************************************************************
 ** How the instance's record search provider fared in one search.
 *******************************************************************************/
public class RecordSearchProviderStatus implements Serializable
{
   private static final long serialVersionUID = 1L;

   private RecordSearchProviderState status;
   private String                    failureClass;
   private String                    message;



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public RecordSearchProviderState getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(RecordSearchProviderState status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public RecordSearchProviderStatus withStatus(RecordSearchProviderState status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for failureClass
    *******************************************************************************/
   public String getFailureClass()
   {
      return (this.failureClass);
   }



   /*******************************************************************************
    ** Setter for failureClass
    *******************************************************************************/
   public void setFailureClass(String failureClass)
   {
      this.failureClass = failureClass;
   }



   /*******************************************************************************
    ** Fluent setter for failureClass - simple class name of the root cause, when the provider failed
    *******************************************************************************/
   public RecordSearchProviderStatus withFailureClass(String failureClass)
   {
      this.failureClass = failureClass;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message - short, single-line failure message, when the provider failed
    *******************************************************************************/
   public RecordSearchProviderStatus withMessage(String message)
   {
      this.message = message;
      return (this);
   }

}
