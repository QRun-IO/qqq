/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.collaboration;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class RuntimeTableMetaDataContribution implements Serializable
{
   private List<QFieldMetaData> fields = new ArrayList<>();
   private Map<String, String> sectionPlacements = new LinkedHashMap<>();
   private List<String> searchFields = new ArrayList<>();
   private List<QVirtualFieldMetaData> virtualFields = new ArrayList<>();



   /*******************************************************************************
    ** Getter for fields
    *******************************************************************************/
   public List<QFieldMetaData> getFields()
   {
      return (fields);
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
   public RuntimeTableMetaDataContribution withFields(List<QFieldMetaData> fields)
   {
      setFields(fields);
      return (this);
   }



   /*******************************************************************************
    ** Getter for sectionPlacements
    *******************************************************************************/
   public Map<String, String> getSectionPlacements()
   {
      return (sectionPlacements);
   }



   /*******************************************************************************
    ** Setter for sectionPlacements
    *******************************************************************************/
   public void setSectionPlacements(Map<String, String> sectionPlacements)
   {
      this.sectionPlacements = sectionPlacements;
   }



   /*******************************************************************************
    ** Fluent setter for sectionPlacements
    *******************************************************************************/
   public RuntimeTableMetaDataContribution withSectionPlacements(Map<String, String> sectionPlacements)
   {
      setSectionPlacements(sectionPlacements);
      return (this);
   }



   /*******************************************************************************
    ** Getter for searchFields
    *******************************************************************************/
   public List<String> getSearchFields()
   {
      return (searchFields);
   }



   /*******************************************************************************
    ** Setter for searchFields
    *******************************************************************************/
   public void setSearchFields(List<String> searchFields)
   {
      this.searchFields = searchFields;
   }



   /*******************************************************************************
    ** Fluent setter for searchFields
    *******************************************************************************/
   public RuntimeTableMetaDataContribution withSearchFields(List<String> searchFields)
   {
      setSearchFields(searchFields);
      return (this);
   }



   /*******************************************************************************
    ** Getter for virtualFields
    *******************************************************************************/
   public List<QVirtualFieldMetaData> getVirtualFields()
   {
      return (virtualFields);
   }



   /*******************************************************************************
    ** Setter for virtualFields
    *******************************************************************************/
   public void setVirtualFields(List<QVirtualFieldMetaData> virtualFields)
   {
      this.virtualFields = virtualFields;
   }



   /*******************************************************************************
    ** Fluent setter for virtualFields
    *******************************************************************************/
   public RuntimeTableMetaDataContribution withVirtualFields(List<QVirtualFieldMetaData> virtualFields)
   {
      setVirtualFields(virtualFields);
      return (this);
   }
}

