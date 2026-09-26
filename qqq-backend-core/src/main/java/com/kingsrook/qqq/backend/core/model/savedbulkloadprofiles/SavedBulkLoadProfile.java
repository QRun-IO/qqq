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
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;


/*******************************************************************************
 ** Entity bean for the savedBulkLoadProfile table
 *******************************************************************************/
public class SavedBulkLoadProfile extends QRecordEntity
{
   public static final String TABLE_NAME = "savedBulkLoadProfile";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(isRequired = true, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.TRUNCATE_ELLIPSIS, label = "Profile Name")
   private String label;

   @QField(possibleValueSourceName = TablesPossibleValueSourceMetaDataProvider.NAME, maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR, label = "Table", isRequired = true)
   private String tableName;

   @QField(maxLength = 250, valueTooLongBehavior = ValueTooLongBehavior.ERROR, dynamicDefaultValueBehavior = DynamicDefaultValueBehavior.USER_ID, label = "Owner")
   private String userId;

   @QField(label = "Mapping JSON")
   private String mappingJson;

   @QField()
   private Boolean isBulkEdit;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SavedBulkLoadProfile()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SavedBulkLoadProfile(QRecord qRecord) throws QException
   {
      populateFromQRecord(qRecord);
   }



   /*******************************************************************************
    ** Getter for id
    **
    *******************************************************************************/
   public Integer getId()
   {
      return id;
   }



   /*******************************************************************************
    ** Setter for id
    **
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Getter for createDate
    **
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return createDate;
   }



   /*******************************************************************************
    ** Setter for createDate
    **
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Getter for modifyDate
    **
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return modifyDate;
   }



   /*******************************************************************************
    ** Setter for modifyDate
    **
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Getter for label
    **
    *******************************************************************************/
   public String getLabel()
   {
      return label;
   }



   /*******************************************************************************
    ** Setter for label
    **
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    **
    *******************************************************************************/
   public SavedBulkLoadProfile withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    **
    *******************************************************************************/
   public SavedBulkLoadProfile withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for userId
    **
    *******************************************************************************/
   public String getUserId()
   {
      return userId;
   }



   /*******************************************************************************
    ** Setter for userId
    **
    *******************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    ** Fluent setter for userId
    **
    *******************************************************************************/
   public SavedBulkLoadProfile withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mappingJson
    *******************************************************************************/
   public String getMappingJson()
   {
      return (this.mappingJson);
   }



   /*******************************************************************************
    ** Setter for mappingJson
    *******************************************************************************/
   public void setMappingJson(String mappingJson)
   {
      this.mappingJson = mappingJson;
   }



   /*******************************************************************************
    ** Fluent setter for mappingJson
    *******************************************************************************/
   public SavedBulkLoadProfile withMappingJson(String mappingJson)
   {
      this.mappingJson = mappingJson;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isBulkEdit
    *******************************************************************************/
   public Boolean getIsBulkEdit()
   {
      return (this.isBulkEdit);
   }



   /*******************************************************************************
    ** Setter for isBulkEdit
    *******************************************************************************/
   public void setIsBulkEdit(Boolean isBulkEdit)
   {
      this.isBulkEdit = isBulkEdit;
   }



   /*******************************************************************************
    ** Fluent setter for isBulkEdit
    *******************************************************************************/
   public SavedBulkLoadProfile withIsBulkEdit(Boolean isBulkEdit)
   {
      this.isBulkEdit = isBulkEdit;
      return (this);
   }

}
