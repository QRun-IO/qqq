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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.serialization.QStepMetaDataDeserializer;


/*******************************************************************************
 ** Meta-Data to define a step in a process in a QQQ instance.
 **
 ** Specifically, this is a base-class for QFrontendStepMetaData and QBackendStepMetaData.
 **
 *******************************************************************************/
@JsonDeserialize(using = QStepMetaDataDeserializer.class)
public abstract class QStepMetaData implements QMetaDataObject, Cloneable
{
   private String name;
   private String label;
   private String stepType;



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
    ** Setter for name
    **
    *******************************************************************************/
   public QStepMetaData withName(String name)
   {
      this.name = name;
      return (this);
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
    ** Setter for label
    **
    *******************************************************************************/
   public QStepMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Get a list of all of the input fields used by this function
    *******************************************************************************/
   @JsonIgnore
   public List<QFieldMetaData> getInputFields()
   {
      return (new ArrayList<>());
   }



   /*******************************************************************************
    ** Get a list of all of the output fields used by this function
    *******************************************************************************/
   @JsonIgnore
   public List<QFieldMetaData> getOutputFields()
   {
      return (new ArrayList<>());
   }



   /*******************************************************************************
    ** Getter for stepType
    **
    *******************************************************************************/
   public String getStepType()
   {
      return stepType;
   }



   /*******************************************************************************
    ** Setter for stepType
    **
    *******************************************************************************/
   public void setStepType(String stepType)
   {
      this.stepType = stepType;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public QStepMetaData clone()
   {
      try
      {
         QStepMetaData clone = (QStepMetaData) super.clone();
         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }
}
