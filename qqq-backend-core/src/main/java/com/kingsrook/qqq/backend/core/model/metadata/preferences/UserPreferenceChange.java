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
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** UserPreferenceChange for the session user's preferences.
 *******************************************************************************/
public class UserPreferenceChange
{
   private Map<String, Serializable> values = new LinkedHashMap<>();
   private Map<String, List<UserPreferenceRowChange>> lists = new LinkedHashMap<>();



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
   public UserPreferenceChange withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }



   /*******************************************************************************
    ** Get lists.
    *******************************************************************************/
   public Map<String, List<UserPreferenceRowChange>> getLists()
   {
      return (lists);
   }



   /*******************************************************************************
    ** Set lists.
    *******************************************************************************/
   public void setLists(Map<String, List<UserPreferenceRowChange>> lists)
   {
      this.lists = lists;
   }



   /*******************************************************************************
    ** With lists.
    *******************************************************************************/
   public UserPreferenceChange withLists(Map<String, List<UserPreferenceRowChange>> lists)
   {
      this.lists = lists;
      return (this);
   }
}
