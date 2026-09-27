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

package com.kingsrook.sampleapp.fixturemetadata;


import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;


/*******************************************************************************
 ** Annotated child entity for the sample parent association.
 *******************************************************************************/
@QMetaDataProducingEntity(produceTableMetaData = true)
public class LabChild extends QRecordEntity
{
   public static final String TABLE_NAME = "metadataLabChild";

   @QField(isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = LabParent.TABLE_NAME)
   private Integer parentId;

   @QField
   private String note;



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return id;
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
   public LabChild withId(Integer id)
   {
      this.id = id;
      return this;
   }



   /*******************************************************************************
    ** Getter for parentId
    *******************************************************************************/
   public Integer getParentId()
   {
      return parentId;
   }



   /*******************************************************************************
    ** Setter for parentId
    *******************************************************************************/
   public void setParentId(Integer parentId)
   {
      this.parentId = parentId;
   }



   /*******************************************************************************
    ** Fluent setter for parentId
    *******************************************************************************/
   public LabChild withParentId(Integer parentId)
   {
      this.parentId = parentId;
      return this;
   }



   /*******************************************************************************
    ** Getter for note
    *******************************************************************************/
   public String getNote()
   {
      return note;
   }



   /*******************************************************************************
    ** Setter for note
    *******************************************************************************/
   public void setNote(String note)
   {
      this.note = note;
   }



   /*******************************************************************************
    ** Fluent setter for note
    *******************************************************************************/
   public LabChild withNote(String note)
   {
      this.note = note;
      return this;
   }
}
