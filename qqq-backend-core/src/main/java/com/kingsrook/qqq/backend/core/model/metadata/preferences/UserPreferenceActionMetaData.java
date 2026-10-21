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
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 ** UserPreferenceActionMetaData for session-scoped preference actions.
 *******************************************************************************/
public class UserPreferenceActionMetaData implements QMetaDataObject
{
   private String name;
   private String label;
   private QIcon icon;
   private String description;
   private UserPreferenceActionScope scope;
   private String listName;
   private String confirmationText;
   private boolean isDestructive;
   private List<QFieldMetaData> inputFields = new ArrayList<>();
   private int order = 100;



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
   public UserPreferenceActionMetaData withName(String name)
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
   public UserPreferenceActionMetaData withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Get icon.
    *******************************************************************************/
   public QIcon getIcon()
   {
      return (icon);
   }



   /*******************************************************************************
    ** Set icon.
    *******************************************************************************/
   public void setIcon(QIcon icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** With icon.
    *******************************************************************************/
   public UserPreferenceActionMetaData withIcon(QIcon icon)
   {
      this.icon = icon;
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
   public UserPreferenceActionMetaData withDescription(String description)
   {
      this.description = description;
      return (this);
   }



   /*******************************************************************************
    ** Get scope.
    *******************************************************************************/
   public UserPreferenceActionScope getScope()
   {
      return (scope);
   }



   /*******************************************************************************
    ** Set scope.
    *******************************************************************************/
   public void setScope(UserPreferenceActionScope scope)
   {
      this.scope = scope;
   }



   /*******************************************************************************
    ** With scope.
    *******************************************************************************/
   public UserPreferenceActionMetaData withScope(UserPreferenceActionScope scope)
   {
      this.scope = scope;
      return (this);
   }



   /*******************************************************************************
    ** Get listName.
    *******************************************************************************/
   public String getListName()
   {
      return (listName);
   }



   /*******************************************************************************
    ** Set listName.
    *******************************************************************************/
   public void setListName(String listName)
   {
      this.listName = listName;
   }



   /*******************************************************************************
    ** With listName.
    *******************************************************************************/
   public UserPreferenceActionMetaData withListName(String listName)
   {
      this.listName = listName;
      return (this);
   }



   /*******************************************************************************
    ** Get confirmationText.
    *******************************************************************************/
   public String getConfirmationText()
   {
      return (confirmationText);
   }



   /*******************************************************************************
    ** Set confirmationText.
    *******************************************************************************/
   public void setConfirmationText(String confirmationText)
   {
      this.confirmationText = confirmationText;
   }



   /*******************************************************************************
    ** With confirmationText.
    *******************************************************************************/
   public UserPreferenceActionMetaData withConfirmationText(String confirmationText)
   {
      this.confirmationText = confirmationText;
      return (this);
   }



   /*******************************************************************************
    ** Get isDestructive.
    *******************************************************************************/
   public boolean getIsDestructive()
   {
      return (isDestructive);
   }



   /*******************************************************************************
    ** Set isDestructive.
    *******************************************************************************/
   public void setIsDestructive(boolean isDestructive)
   {
      this.isDestructive = isDestructive;
   }



   /*******************************************************************************
    ** With isDestructive.
    *******************************************************************************/
   public UserPreferenceActionMetaData withIsDestructive(boolean isDestructive)
   {
      this.isDestructive = isDestructive;
      return (this);
   }



   /*******************************************************************************
    ** Get inputFields.
    *******************************************************************************/
   public List<QFieldMetaData> getInputFields()
   {
      return (inputFields);
   }



   /*******************************************************************************
    ** Set inputFields.
    *******************************************************************************/
   public void setInputFields(List<QFieldMetaData> inputFields)
   {
      this.inputFields = inputFields;
   }



   /*******************************************************************************
    ** With inputFields.
    *******************************************************************************/
   public UserPreferenceActionMetaData withInputFields(List<QFieldMetaData> inputFields)
   {
      this.inputFields = inputFields;
      return (this);
   }



   /*******************************************************************************
    ** Get order.
    *******************************************************************************/
   public int getOrder()
   {
      return (order);
   }



   /*******************************************************************************
    ** Set order.
    *******************************************************************************/
   public void setOrder(int order)
   {
      this.order = order;
   }



   /*******************************************************************************
    ** With order.
    *******************************************************************************/
   public UserPreferenceActionMetaData withOrder(int order)
   {
      this.order = order;
      return (this);
   }


   /*******************************************************************************
    ** Append an action input field.
    *******************************************************************************/
   public UserPreferenceActionMetaData withInputField(QFieldMetaData field)
   {
      inputFields.add(field);
      return (this);
   }
}
