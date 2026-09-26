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

package com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareScopePossibleValueMetaDataProducer;


/*******************************************************************************
 ** Entity bean for the shared saved bulk load profile table
 *******************************************************************************/
public class SharedSavedBulkLoadProfile extends QRecordEntity
{
   public static final String TABLE_NAME = "sharedSavedBulkLoadProfile";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(possibleValueSourceName = SavedBulkLoadProfile.TABLE_NAME, label = "Bulk Load Profile")
   private Integer savedBulkLoadProfileId;

   @QField(label = "User")
   private String userId;

   @QField(possibleValueSourceName = ShareScopePossibleValueMetaDataProducer.NAME)
   private String scope;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SharedSavedBulkLoadProfile()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SharedSavedBulkLoadProfile(QRecord qRecord) throws QException
   {
      populateFromQRecord(qRecord);
   }




   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public SharedSavedBulkLoadProfile withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for createDate
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /*******************************************************************************
    ** Setter for createDate
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    *******************************************************************************/
   public SharedSavedBulkLoadProfile withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modifyDate
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /*******************************************************************************
    ** Setter for modifyDate
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    *******************************************************************************/
   public SharedSavedBulkLoadProfile withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }




   /*******************************************************************************
    ** Getter for userId
    *******************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    ** Setter for userId
    *******************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    ** Fluent setter for userId
    *******************************************************************************/
   public SharedSavedBulkLoadProfile withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scope
    *******************************************************************************/
   public String getScope()
   {
      return (this.scope);
   }



   /*******************************************************************************
    ** Setter for scope
    *******************************************************************************/
   public void setScope(String scope)
   {
      this.scope = scope;
   }



   /*******************************************************************************
    ** Fluent setter for scope
    *******************************************************************************/
   public SharedSavedBulkLoadProfile withScope(String scope)
   {
      this.scope = scope;
      return (this);
   }



   /*******************************************************************************
    ** Getter for savedBulkLoadProfileId
    *******************************************************************************/
   public Integer getSavedBulkLoadProfileId()
   {
      return (this.savedBulkLoadProfileId);
   }



   /*******************************************************************************
    ** Setter for savedBulkLoadProfileId
    *******************************************************************************/
   public void setSavedBulkLoadProfileId(Integer savedBulkLoadProfileId)
   {
      this.savedBulkLoadProfileId = savedBulkLoadProfileId;
   }



   /*******************************************************************************
    ** Fluent setter for savedBulkLoadProfileId
    *******************************************************************************/
   public SharedSavedBulkLoadProfile withSavedBulkLoadProfileId(Integer savedBulkLoadProfileId)
   {
      this.savedBulkLoadProfileId = savedBulkLoadProfileId;
      return (this);
   }


}
