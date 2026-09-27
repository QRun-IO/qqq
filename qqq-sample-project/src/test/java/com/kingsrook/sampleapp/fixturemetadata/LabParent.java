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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QAssociation;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildJoin;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildRecordListWidget;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.ChildTable;
import com.kingsrook.qqq.backend.core.model.metadata.producers.annotations.QMetaDataProducingEntity;


/*******************************************************************************
 ** Annotated parent used by the sample metadata and record conversion scenarios.
 *******************************************************************************/
@QMetaDataProducingEntity(produceTableMetaData = true, producePossibleValueSource = true,
   childTables = @ChildTable(childTableEntityClass = LabChild.class,
      childJoin = @ChildJoin(enabled = true),
      childRecordListWidget = @ChildRecordListWidget(enabled = true, label = "Lab Children", maxRows = 5)))
public class LabParent extends QRecordEntity
{
   public static final String TABLE_NAME = "metadataLabParent";

   @QField(isPrimaryKey = true, isEditable = false)
   private Integer id;

   @QField(isRequired = true, label = "Parent Name", maxLength = 60)
   private String name;

   @QField(possibleValueSourceName = "LabStatus")
   private Integer status;

   @QAssociation(name = "children")
   private List<LabChild> children;



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
   public LabParent withId(Integer id)
   {
      this.id = id;
      return this;
   }



   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return name;
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
   public LabParent withName(String name)
   {
      this.name = name;
      return this;
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public Integer getStatus()
   {
      return status;
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(Integer status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public LabParent withStatus(Integer status)
   {
      this.status = status;
      return this;
   }



   /*******************************************************************************
    ** Getter for children
    *******************************************************************************/
   public List<LabChild> getChildren()
   {
      return children;
   }



   /*******************************************************************************
    ** Setter for children
    *******************************************************************************/
   public void setChildren(List<LabChild> children)
   {
      this.children = children;
   }



   /*******************************************************************************
    ** Fluent setter for children
    *******************************************************************************/
   public LabParent withChildren(List<LabChild> children)
   {
      this.children = children;
      return this;
   }
}
