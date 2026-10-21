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
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;


/*******************************************************************************
 **
 *******************************************************************************/
public class Script extends QRecordEntity
{
   public static final String TABLE_NAME = "script";

   @QField(isEditable = false)
   private Integer id;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;

   @QField()
   private String name;

   @QField(possibleValueSourceName = "scriptType")
   private Integer scriptTypeId;

   @QField(possibleValueSourceName = TablesPossibleValueSourceMetaDataProvider.NAME)
   private String tableName;

   @QField()
   private Integer maxBatchSize;

   @QField(possibleValueSourceName = "scriptRevision")
   private Integer currentScriptRevisionId;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Script()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Script(QRecord qRecord) throws QException
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
    ** Fluent setter for id
    **
    *******************************************************************************/
   public Script withId(Integer id)
   {
      this.id = id;
      return (this);
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
    ** Fluent setter for createDate
    **
    *******************************************************************************/
   public Script withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
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
    ** Fluent setter for modifyDate
    **
    *******************************************************************************/
   public Script withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public Script withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptTypeId
    **
    *******************************************************************************/
   public Integer getScriptTypeId()
   {
      return scriptTypeId;
   }



   /*******************************************************************************
    ** Setter for scriptTypeId
    **
    *******************************************************************************/
   public void setScriptTypeId(Integer scriptTypeId)
   {
      this.scriptTypeId = scriptTypeId;
   }



   /*******************************************************************************
    ** Fluent setter for scriptTypeId
    **
    *******************************************************************************/
   public Script withScriptTypeId(Integer scriptTypeId)
   {
      this.scriptTypeId = scriptTypeId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for currentScriptRevisionId
    **
    *******************************************************************************/
   public Integer getCurrentScriptRevisionId()
   {
      return currentScriptRevisionId;
   }



   /*******************************************************************************
    ** Setter for currentScriptRevisionId
    **
    *******************************************************************************/
   public void setCurrentScriptRevisionId(Integer currentScriptRevisionId)
   {
      this.currentScriptRevisionId = currentScriptRevisionId;
   }



   /*******************************************************************************
    ** Fluent setter for currentScriptRevisionId
    **
    *******************************************************************************/
   public Script withCurrentScriptRevisionId(Integer currentScriptRevisionId)
   {
      this.currentScriptRevisionId = currentScriptRevisionId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public Script withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxBatchSize
    *******************************************************************************/
   public Integer getMaxBatchSize()
   {
      return (this.maxBatchSize);
   }



   /*******************************************************************************
    ** Setter for maxBatchSize
    *******************************************************************************/
   public void setMaxBatchSize(Integer maxBatchSize)
   {
      this.maxBatchSize = maxBatchSize;
   }



   /*******************************************************************************
    ** Fluent setter for maxBatchSize
    *******************************************************************************/
   public Script withMaxBatchSize(Integer maxBatchSize)
   {
      this.maxBatchSize = maxBatchSize;
      return (this);
   }

}
