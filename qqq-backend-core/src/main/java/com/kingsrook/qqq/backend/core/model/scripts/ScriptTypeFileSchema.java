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

package com.kingsrook.qqq.backend.core.model.scripts;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 **
 *******************************************************************************/
public class ScriptTypeFileSchema extends QRecordEntity
{
   public static final String TABLE_NAME = "scriptTypeFileSchema";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField(possibleValueSourceName = "scriptType")
   private Integer scriptTypeId;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String name;

   @QField(maxLength = 50, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String fileType;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ScriptTypeFileSchema()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ScriptTypeFileSchema(QRecord qRecord) throws QException
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
   public ScriptTypeFileSchema withId(Integer id)
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
   public ScriptTypeFileSchema withCreateDate(Instant createDate)
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
   public ScriptTypeFileSchema withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptTypeId
    *******************************************************************************/
   public Integer getScriptTypeId()
   {
      return (this.scriptTypeId);
   }



   /*******************************************************************************
    ** Setter for scriptTypeId
    *******************************************************************************/
   public void setScriptTypeId(Integer scriptTypeId)
   {
      this.scriptTypeId = scriptTypeId;
   }



   /*******************************************************************************
    ** Fluent setter for scriptTypeId
    *******************************************************************************/
   public ScriptTypeFileSchema withScriptTypeId(Integer scriptTypeId)
   {
      this.scriptTypeId = scriptTypeId;
      return (this);
   }



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
   public ScriptTypeFileSchema withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fileType
    *******************************************************************************/
   public String getFileType()
   {
      return (this.fileType);
   }



   /*******************************************************************************
    ** Setter for fileType
    *******************************************************************************/
   public void setFileType(String fileType)
   {
      this.fileType = fileType;
   }



   /*******************************************************************************
    ** Fluent setter for fileType
    *******************************************************************************/
   public ScriptTypeFileSchema withFileType(String fileType)
   {
      this.fileType = fileType;
      return (this);
   }

}