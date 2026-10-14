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
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;


/*******************************************************************************
 ** UserPreferenceSectionMetaData for the session user's preferences.
 *******************************************************************************/
public class UserPreferenceSectionMetaData implements QMetaDataObject
{
   private String name;
   private String label;
   private String description;
   private QIcon icon;
   private int order = 100;
   private String appName;
   private String permissionName;
   private List<QFieldMetaData> fields = new ArrayList<>();
   private List<UserPreferenceListMetaData> lists = new ArrayList<>();
   private QCodeReference provider;



   /*******************************************************************************
    ** Add a field.
    *******************************************************************************/
   public UserPreferenceSectionMetaData withField(QFieldMetaData field)
   {
      fields.add(field);
      return (this);
   }



   /*******************************************************************************
    ** Add a list.
    *******************************************************************************/
   public UserPreferenceSectionMetaData withList(UserPreferenceListMetaData list)
   {
      lists.add(list);
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
   public UserPreferenceSectionMetaData withName(String name)
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
   public UserPreferenceSectionMetaData withLabel(String label)
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
   public UserPreferenceSectionMetaData withDescription(String description)
   {
      this.description = description;
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
   public UserPreferenceSectionMetaData withIcon(QIcon icon)
   {
      this.icon = icon;
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
   public UserPreferenceSectionMetaData withOrder(int order)
   {
      this.order = order;
      return (this);
   }



   /*******************************************************************************
    ** Get appName.
    *******************************************************************************/
   public String getAppName()
   {
      return (appName);
   }



   /*******************************************************************************
    ** Set appName.
    *******************************************************************************/
   public void setAppName(String appName)
   {
      this.appName = appName;
   }



   /*******************************************************************************
    ** With appName.
    *******************************************************************************/
   public UserPreferenceSectionMetaData withAppName(String appName)
   {
      this.appName = appName;
      return (this);
   }



   /*******************************************************************************
    ** Get permissionName.
    *******************************************************************************/
   public String getPermissionName()
   {
      return (permissionName);
   }



   /*******************************************************************************
    ** Set permissionName.
    *******************************************************************************/
   public void setPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
   }



   /*******************************************************************************
    ** With permissionName.
    *******************************************************************************/
   public UserPreferenceSectionMetaData withPermissionName(String permissionName)
   {
      this.permissionName = permissionName;
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
   public UserPreferenceSectionMetaData withFields(List<QFieldMetaData> fields)
   {
      this.fields = fields;
      return (this);
   }



   /*******************************************************************************
    ** Get lists.
    *******************************************************************************/
   public List<UserPreferenceListMetaData> getLists()
   {
      return (lists);
   }



   /*******************************************************************************
    ** Set lists.
    *******************************************************************************/
   public void setLists(List<UserPreferenceListMetaData> lists)
   {
      this.lists = lists;
   }



   /*******************************************************************************
    ** With lists.
    *******************************************************************************/
   public UserPreferenceSectionMetaData withLists(List<UserPreferenceListMetaData> lists)
   {
      this.lists = lists;
      return (this);
   }



   /*******************************************************************************
    ** Get provider.
    *******************************************************************************/
   public QCodeReference getProvider()
   {
      return (provider);
   }



   /*******************************************************************************
    ** Set provider.
    *******************************************************************************/
   public void setProvider(QCodeReference provider)
   {
      this.provider = provider;
   }



   /*******************************************************************************
    ** With provider.
    *******************************************************************************/
   public UserPreferenceSectionMetaData withProvider(QCodeReference provider)
   {
      this.provider = provider;
      return (this);
   }
}
