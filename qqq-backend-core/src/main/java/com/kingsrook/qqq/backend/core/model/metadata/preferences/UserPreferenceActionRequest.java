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


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 ** UserPreferenceActionRequest for session-scoped preference actions.
 *******************************************************************************/
public class UserPreferenceActionRequest
{
   private String actionName;
   private String listName;
   private String rowKey;
   private Map<String, Serializable> values = new LinkedHashMap<>();



   /*******************************************************************************
    ** Get actionName.
    *******************************************************************************/
   public String getActionName()
   {
      return (actionName);
   }



   /*******************************************************************************
    ** Set actionName.
    *******************************************************************************/
   public void setActionName(String actionName)
   {
      this.actionName = actionName;
   }



   /*******************************************************************************
    ** With actionName.
    *******************************************************************************/
   public UserPreferenceActionRequest withActionName(String actionName)
   {
      this.actionName = actionName;
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
   public UserPreferenceActionRequest withListName(String listName)
   {
      this.listName = listName;
      return (this);
   }



   /*******************************************************************************
    ** Get rowKey.
    *******************************************************************************/
   public String getRowKey()
   {
      return (rowKey);
   }



   /*******************************************************************************
    ** Set rowKey.
    *******************************************************************************/
   public void setRowKey(String rowKey)
   {
      this.rowKey = rowKey;
   }



   /*******************************************************************************
    ** With rowKey.
    *******************************************************************************/
   public UserPreferenceActionRequest withRowKey(String rowKey)
   {
      this.rowKey = rowKey;
      return (this);
   }



   /*******************************************************************************
    ** Get values.
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (values);
   }



   /*******************************************************************************
    ** Set values.
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** With values.
    *******************************************************************************/
   public UserPreferenceActionRequest withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }
}
