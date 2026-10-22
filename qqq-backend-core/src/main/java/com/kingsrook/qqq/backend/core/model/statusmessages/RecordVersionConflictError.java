/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.statusmessages;


import java.io.Serializable;
import java.util.List;


/*******************************************************************************
 ** A stale optimistic version. Includes hints, never stored field values.
 *******************************************************************************/
public class RecordVersionConflictError extends QErrorMessage
{
   private Serializable currentVersion;
   private Serializable modifiedByUserId;
   private Serializable modifiedAt;
   private List<String> changedFields;



   /*******************************************************************************
    ** Create a per-record conflict that existing backend error handling will skip.
    *******************************************************************************/
   public RecordVersionConflictError(Serializable currentVersion, Serializable modifiedByUserId, Serializable modifiedAt, List<String> changedFields)
   {
      super("The record has changed since it was read. Refresh it before saving.");
      this.currentVersion = currentVersion;
      this.modifiedByUserId = modifiedByUserId;
      this.modifiedAt = modifiedAt;
      this.changedFields = List.copyOf(changedFields);
   }




   /*******************************************************************************
    ** Getter for currentVersion
    *******************************************************************************/
   public Serializable getCurrentVersion()
   {
      return (currentVersion);
   }




   /*******************************************************************************
    ** Setter for currentVersion
    *******************************************************************************/
   public void setCurrentVersion(Serializable currentVersion)
   {
      this.currentVersion = currentVersion;
   }




   /*******************************************************************************
    ** Fluent setter for currentVersion
    *******************************************************************************/
   public RecordVersionConflictError withCurrentVersion(Serializable currentVersion)
   {
      setCurrentVersion(currentVersion);
      return (this);
   }




   /*******************************************************************************
    ** Getter for modifiedByUserId
    *******************************************************************************/
   public Serializable getModifiedByUserId()
   {
      return (modifiedByUserId);
   }




   /*******************************************************************************
    ** Setter for modifiedByUserId
    *******************************************************************************/
   public void setModifiedByUserId(Serializable modifiedByUserId)
   {
      this.modifiedByUserId = modifiedByUserId;
   }




   /*******************************************************************************
    ** Fluent setter for modifiedByUserId
    *******************************************************************************/
   public RecordVersionConflictError withModifiedByUserId(Serializable modifiedByUserId)
   {
      setModifiedByUserId(modifiedByUserId);
      return (this);
   }




   /*******************************************************************************
    ** Getter for modifiedAt
    *******************************************************************************/
   public Serializable getModifiedAt()
   {
      return (modifiedAt);
   }




   /*******************************************************************************
    ** Setter for modifiedAt
    *******************************************************************************/
   public void setModifiedAt(Serializable modifiedAt)
   {
      this.modifiedAt = modifiedAt;
   }




   /*******************************************************************************
    ** Fluent setter for modifiedAt
    *******************************************************************************/
   public RecordVersionConflictError withModifiedAt(Serializable modifiedAt)
   {
      setModifiedAt(modifiedAt);
      return (this);
   }




   /*******************************************************************************
    ** Getter for changedFields
    *******************************************************************************/
   public List<String> getChangedFields()
   {
      return (changedFields);
   }




   /*******************************************************************************
    ** Setter for changedFields
    *******************************************************************************/
   public void setChangedFields(List<String> changedFields)
   {
      this.changedFields = changedFields;
   }




   /*******************************************************************************
    ** Fluent setter for changedFields
    *******************************************************************************/
   public RecordVersionConflictError withChangedFields(List<String> changedFields)
   {
      setChangedFields(changedFields);
      return (this);
   }
}

