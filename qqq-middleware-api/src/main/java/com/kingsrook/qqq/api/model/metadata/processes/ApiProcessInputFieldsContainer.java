/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata.processes;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiProcessInputFieldsContainer
{
   private QFieldMetaData       recordIdsField;
   private List<QFieldMetaData> fields;



   /*******************************************************************************
    ** find all input fields in frontend steps of the process, and add them as fields
    ** in this container.
    *******************************************************************************/
   public ApiProcessInputFieldsContainer withInferredInputFields(QProcessMetaData processMetaData)
   {
      return (withInferredInputFieldsExcluding(processMetaData, Collections.emptySet()));
   }



   /*******************************************************************************
    ** find all input fields in frontend steps of the process, and add them as fields
    ** in this container, unless they're in the collection to exclude.
    *******************************************************************************/
   public ApiProcessInputFieldsContainer withInferredInputFieldsExcluding(QProcessMetaData processMetaData, Collection<String> minusFieldNames)
   {
      if(fields == null)
      {
         fields = new ArrayList<>();
      }

      for(QStepMetaData stepMetaData : CollectionUtils.nonNullList(processMetaData.getStepList()))
      {
         if(stepMetaData instanceof QFrontendStepMetaData frontendStep)
         {
            for(QFieldMetaData inputField : frontendStep.getInputFields())
            {
               if(minusFieldNames != null && !minusFieldNames.contains(inputField.getName()))
               {
                  fields.add(inputField);
               }
            }
         }
      }

      return (this);
   }



   /*******************************************************************************
    ** Getter for recordIdsField
    *******************************************************************************/
   public QFieldMetaData getRecordIdsField()
   {
      return (this.recordIdsField);
   }



   /*******************************************************************************
    ** Setter for recordIdsField
    *******************************************************************************/
   public void setRecordIdsField(QFieldMetaData recordIdsField)
   {
      this.recordIdsField = recordIdsField;
   }



   /*******************************************************************************
    ** Fluent setter for recordIdsField
    *******************************************************************************/
   public ApiProcessInputFieldsContainer withRecordIdsField(QFieldMetaData recordIdsField)
   {
      this.recordIdsField = recordIdsField;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fields
    *******************************************************************************/
   public List<QFieldMetaData> getFields()
   {
      return (this.fields);
   }



   /*******************************************************************************
    ** Setter for fields
    *******************************************************************************/
   public void setFields(List<QFieldMetaData> fields)
   {
      this.fields = fields;
   }



   /*******************************************************************************
    ** Fluent setter for fields
    *******************************************************************************/
   public ApiProcessInputFieldsContainer withField(QFieldMetaData field)
   {
      if(this.fields == null)
      {
         this.fields = new ArrayList<>();
      }
      this.fields.add(field);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for fields
    *******************************************************************************/
   public ApiProcessInputFieldsContainer withFields(List<QFieldMetaData> fields)
   {
      this.fields = fields;
      return (this);
   }
}
