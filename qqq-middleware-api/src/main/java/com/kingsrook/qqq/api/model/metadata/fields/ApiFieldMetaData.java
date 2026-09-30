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

package com.kingsrook.qqq.api.model.metadata.fields;


import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.openapi.model.Example;


/*******************************************************************************
 **
 *******************************************************************************/
public class ApiFieldMetaData
{
   private String initialVersion;
   private String finalVersion;

   private String apiFieldName;
   private String description;
   private boolean listEnumPossibleValues = true;

   private Boolean        isExcluded;
   private String         replacedByFieldName;
   private QCodeReference customValueMapper;

   private Example              example;
   private Map<String, Example> examples;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getEffectiveApiFieldName(String apiName, QFieldMetaData field)
   {
      ApiFieldMetaDataContainer apiFieldMetaDataContainer = ApiFieldMetaDataContainer.of(field);
      if(apiFieldMetaDataContainer != null)
      {
         ApiFieldMetaData apiFieldMetaData = apiFieldMetaDataContainer.getApiFieldMetaData(apiName);
         if(apiFieldMetaData != null && StringUtils.hasContent(apiFieldMetaData.apiFieldName))
         {
            return (apiFieldMetaData.apiFieldName);
         }
      }

      return (field.getName());
   }



   /*******************************************************************************
    ** Getter for initialVersion
    *******************************************************************************/
   public String getInitialVersion()
   {
      return (this.initialVersion);
   }



   /*******************************************************************************
    ** Setter for initialVersion
    *******************************************************************************/
   public void setInitialVersion(String initialVersion)
   {
      this.initialVersion = initialVersion;
   }



   /*******************************************************************************
    ** Fluent setter for initialVersion
    *******************************************************************************/
   public ApiFieldMetaData withInitialVersion(String initialVersion)
   {
      this.initialVersion = initialVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for finalVersion
    *******************************************************************************/
   public String getFinalVersion()
   {
      return (this.finalVersion);
   }



   /*******************************************************************************
    ** Setter for finalVersion
    *******************************************************************************/
   public void setFinalVersion(String finalVersion)
   {
      this.finalVersion = finalVersion;
   }



   /*******************************************************************************
    ** Fluent setter for finalVersion
    *******************************************************************************/
   public ApiFieldMetaData withFinalVersion(String finalVersion)
   {
      this.finalVersion = finalVersion;
      return (this);
   }



   /*******************************************************************************
    ** Getter for replacedByFieldName
    *******************************************************************************/
   public String getReplacedByFieldName()
   {
      return (this.replacedByFieldName);
   }



   /*******************************************************************************
    ** Setter for replacedByFieldName
    *******************************************************************************/
   public void setReplacedByFieldName(String replacedByFieldName)
   {
      this.replacedByFieldName = replacedByFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for replacedByFieldName
    *******************************************************************************/
   public ApiFieldMetaData withReplacedByFieldName(String replacedByFieldName)
   {
      this.replacedByFieldName = replacedByFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isExcluded
    *******************************************************************************/
   public Boolean getIsExcluded()
   {
      return (this.isExcluded);
   }



   /*******************************************************************************
    ** Setter for isExcluded
    *******************************************************************************/
   public void setIsExcluded(Boolean isExcluded)
   {
      this.isExcluded = isExcluded;
   }



   /*******************************************************************************
    ** Fluent setter for isExcluded
    *******************************************************************************/
   public ApiFieldMetaData withIsExcluded(Boolean isExcluded)
   {
      this.isExcluded = isExcluded;
      return (this);
   }



   /*******************************************************************************
    ** Getter for apiFieldName
    *******************************************************************************/
   public String getApiFieldName()
   {
      return (this.apiFieldName);
   }



   /*******************************************************************************
    ** Setter for apiFieldName
    *******************************************************************************/
   public void setApiFieldName(String apiFieldName)
   {
      this.apiFieldName = apiFieldName;
   }



   /*******************************************************************************
    ** Fluent setter for apiFieldName
    *******************************************************************************/
   public ApiFieldMetaData withApiFieldName(String apiFieldName)
   {
      this.apiFieldName = apiFieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public ApiFieldMetaData withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Getter for example
    *******************************************************************************/
   public Example getExample()
   {
      return (this.example);
   }



   /*******************************************************************************
    ** Setter for example
    *******************************************************************************/
   public void setExample(Example example)
   {
      this.example = example;
   }



   /*******************************************************************************
    ** Fluent setter for example
    *******************************************************************************/
   public ApiFieldMetaData withExample(Example example)
   {
      this.example = example;
      return (this);
   }



   /*******************************************************************************
    ** Getter for examples
    *******************************************************************************/
   public Map<String, Example> getExamples()
   {
      return (this.examples);
   }



   /*******************************************************************************
    ** Setter for examples
    *******************************************************************************/
   public void setExamples(Map<String, Example> examples)
   {
      this.examples = examples;
   }



   /*******************************************************************************
    ** Fluent setter for examples
    *******************************************************************************/
   public ApiFieldMetaData withExamples(Map<String, Example> examples)
   {
      this.examples = examples;
      return (this);
   }



   /*******************************************************************************
    ** Getter for customValueMapper
    *******************************************************************************/
   public QCodeReference getCustomValueMapper()
   {
      return (this.customValueMapper);
   }



   /*******************************************************************************
    ** Setter for customValueMapper
    *******************************************************************************/
   public void setCustomValueMapper(QCodeReference customValueMapper)
   {
      this.customValueMapper = customValueMapper;
   }



   /*******************************************************************************
    ** Fluent setter for customValueMapper
    *******************************************************************************/
   public ApiFieldMetaData withCustomValueMapper(QCodeReference customValueMapper)
   {
      this.customValueMapper = customValueMapper;
      return (this);
   }



   /*******************************************************************************
    ** Getter for listEnumPossibleValues
    *******************************************************************************/
   public boolean getListEnumPossibleValues()
   {
      return (this.listEnumPossibleValues);
   }



   /*******************************************************************************
    ** Setter for listEnumPossibleValues
    *******************************************************************************/
   public void setListEnumPossibleValues(boolean listEnumPossibleValues)
   {
      this.listEnumPossibleValues = listEnumPossibleValues;
   }



   /*******************************************************************************
    ** Fluent setter for listEnumPossibleValues
    *******************************************************************************/
   public ApiFieldMetaData withListEnumPossibleValues(boolean listEnumPossibleValues)
   {
      this.listEnumPossibleValues = listEnumPossibleValues;
      return (this);
   }

}
