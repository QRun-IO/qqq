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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.PreferenceSectionPatchRequestV1;


/*******************************************************************************
 ** UserPreferencesMiddlewareInput in the user preferences API.
 *******************************************************************************/
public class UserPreferencesMiddlewareInput extends AbstractMiddlewareInput
{
   private Operation operation;
   private String sectionName;
   private PreferenceSectionPatchRequestV1 request;



   /*******************************************************************************
    ** The three session-scoped preference operations.
    *******************************************************************************/
   public enum Operation
   {
      LIST,
      GET,
      PATCH
   }



   /*******************************************************************************
    ** Get operation.
    *******************************************************************************/
   public Operation getOperation()
   {
      return (operation);
   }



   /*******************************************************************************
    ** Set operation.
    *******************************************************************************/
   public void setOperation(Operation operation)
   {
      this.operation = operation;
   }



   /*******************************************************************************
    ** With operation.
    *******************************************************************************/
   public UserPreferencesMiddlewareInput withOperation(Operation operation)
   {
      this.operation = operation;
      return (this);
   }



   /*******************************************************************************
    ** Get sectionName.
    *******************************************************************************/
   public String getSectionName()
   {
      return (sectionName);
   }



   /*******************************************************************************
    ** Set sectionName.
    *******************************************************************************/
   public void setSectionName(String sectionName)
   {
      this.sectionName = sectionName;
   }



   /*******************************************************************************
    ** With sectionName.
    *******************************************************************************/
   public UserPreferencesMiddlewareInput withSectionName(String sectionName)
   {
      this.sectionName = sectionName;
      return (this);
   }



   /*******************************************************************************
    ** Get request.
    *******************************************************************************/
   public PreferenceSectionPatchRequestV1 getRequest()
   {
      return (request);
   }



   /*******************************************************************************
    ** Set request.
    *******************************************************************************/
   public void setRequest(PreferenceSectionPatchRequestV1 request)
   {
      this.request = request;
   }



   /*******************************************************************************
    ** With request.
    *******************************************************************************/
   public UserPreferencesMiddlewareInput withRequest(PreferenceSectionPatchRequestV1 request)
   {
      this.request = request;
      return (this);
   }
}
