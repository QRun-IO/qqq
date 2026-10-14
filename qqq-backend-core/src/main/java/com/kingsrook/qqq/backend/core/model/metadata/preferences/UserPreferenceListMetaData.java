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

package com.kingsrook.qqq.backend.core.model.metadata.preferences;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 ** UserPreferenceListMetaData for the session user's preferences.
 *******************************************************************************/
public class UserPreferenceListMetaData implements QMetaDataObject
{
   private String name;
   private String label;
   private String description;
   private String rowLabel;
   private List<QFieldMetaData> fields = new ArrayList<>();



   /*******************************************************************************
    ** Add a field.
    *******************************************************************************/
   public UserPreferenceListMetaData withField(QFieldMetaData field)
   {
      fields.add(field);
      return (this);
   }



   /*******************************************************************************
    ** Get name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Set name.
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** With name.
    *******************************************************************************/
   public UserPreferenceListMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Get label.
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    ** Set label.
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** With label.
    *******************************************************************************/
   public UserPreferenceListMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Get description.
    *******************************************************************************/
   public String getDescription()
   {
      return (description);
   }



   /*******************************************************************************
    ** Set description.
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** With description.
    *******************************************************************************/
   public UserPreferenceListMetaData withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Get rowLabel.
    *******************************************************************************/
   public String getRowLabel()
   {
      return (rowLabel);
   }



   /*******************************************************************************
    ** Set rowLabel.
    *******************************************************************************/
   public void setRowLabel(String rowLabel)
   {
      this.rowLabel = rowLabel;
   }



   /*******************************************************************************
    ** With rowLabel.
    *******************************************************************************/
   public UserPreferenceListMetaData withRowLabel(String rowLabel)
   {
      this.rowLabel = rowLabel;
      return (this);
   }



   /*******************************************************************************
    ** Get fields.
    *******************************************************************************/
   public List<QFieldMetaData> getFields()
   {
      return (fields);
   }



   /*******************************************************************************
    ** Set fields.
    *******************************************************************************/
   public void setFields(List<QFieldMetaData> fields)
   {
      this.fields = fields;
   }



   /*******************************************************************************
    ** With fields.
    *******************************************************************************/
   public UserPreferenceListMetaData withFields(List<QFieldMetaData> fields)
   {
      this.fields = fields;
      return (this);
   }
}
