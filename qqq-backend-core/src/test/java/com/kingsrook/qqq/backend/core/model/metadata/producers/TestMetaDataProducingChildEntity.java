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

package com.kingsrook.qqq.backend.core.model.metadata.producers;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** QRecord Entity for TestMetaDataProducingEntity table
 *******************************************************************************/
public class TestMetaDataProducingChildEntity extends QRecordEntity implements MetaDataProducerInterface<QTableMetaData>
{
   public static final String TABLE_NAME = "testMetaDataProducingChildEntity";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = TestMetaDataProducingEntity.TABLE_NAME)
   private Integer parentId;


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QTableMetaData produce(QInstance qInstance) throws QException
   {
      return new QTableMetaData()
         .withName(TABLE_NAME)
         .withFieldsFromEntity(TestMetaDataProducingChildEntity.class);
   }



   /*******************************************************************************
    ** Default constructor
    *******************************************************************************/
   public TestMetaDataProducingChildEntity()
   {
   }



   /*******************************************************************************
    ** Constructor that takes a QRecord
    *******************************************************************************/
   public TestMetaDataProducingChildEntity(QRecord record)
   {
      populateFromQRecord(record);
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
   public TestMetaDataProducingChildEntity withId(Integer id)
   {
      this.id = id;
      return (this);
   }



   /*******************************************************************************
    ** Getter for parentId
    *******************************************************************************/
   public Integer getParentId()
   {
      return (this.parentId);
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
   public TestMetaDataProducingChildEntity withParentId(Integer parentId)
   {
      this.parentId = parentId;
      return (this);
   }


}
