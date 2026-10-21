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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/*******************************************************************************
 ** UserPreferenceRow for the session user's preferences.
 *******************************************************************************/
public class UserPreferenceRow
{
   private Set<String> actionNames;
   private String key;
   private String label;
   private String description;
   private Map<String, Serializable> values = new LinkedHashMap<>();
   private List<String> readOnlyFieldNames = new ArrayList<>();
   private String readOnlyReason;



   /*******************************************************************************
    ** Get key.
    *******************************************************************************/
   public String getKey()
   {
      return (key);
   }



   /*******************************************************************************
    ** Set key.
    *******************************************************************************/
   public void setKey(String key)
   {
      this.key = key;
   }



   /*******************************************************************************
    ** With key.
    *******************************************************************************/
   public UserPreferenceRow withKey(String key)
   {
      this.key = key;
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
   public UserPreferenceRow withLabel(String label)
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
   public UserPreferenceRow withDescription(String description)
   {
      this.description = description;
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
   public UserPreferenceRow withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }



   /*******************************************************************************
    ** Get readOnlyFieldNames.
    *******************************************************************************/
   public List<String> getReadOnlyFieldNames()
   {
      return (readOnlyFieldNames);
   }



   /*******************************************************************************
    ** Set readOnlyFieldNames.
    *******************************************************************************/
   public void setReadOnlyFieldNames(List<String> readOnlyFieldNames)
   {
      this.readOnlyFieldNames = readOnlyFieldNames;
   }



   /*******************************************************************************
    ** With readOnlyFieldNames.
    *******************************************************************************/
   public UserPreferenceRow withReadOnlyFieldNames(List<String> readOnlyFieldNames)
   {
      this.readOnlyFieldNames = readOnlyFieldNames;
      return (this);
   }



   /*******************************************************************************
    ** Get readOnlyReason.
    *******************************************************************************/
   public String getReadOnlyReason()
   {
      return (readOnlyReason);
   }



   /*******************************************************************************
    ** Set readOnlyReason.
    *******************************************************************************/
   public void setReadOnlyReason(String readOnlyReason)
   {
      this.readOnlyReason = readOnlyReason;
   }



   /*******************************************************************************
    ** With readOnlyReason.
    *******************************************************************************/
   public UserPreferenceRow withReadOnlyReason(String readOnlyReason)
   {
      this.readOnlyReason = readOnlyReason;
      return (this);
   }



   /*******************************************************************************
    ** Null allows every ROW action for this list; an empty set allows none.
    *******************************************************************************/
   public Set<String> getActionNames()
   {
      return (actionNames);
   }



   /*******************************************************************************
    ** Set actionNames.
    *******************************************************************************/
   public void setActionNames(Set<String> actionNames)
   {
      this.actionNames = actionNames;
   }



   /*******************************************************************************
    ** With actionNames.
    *******************************************************************************/
   public UserPreferenceRow withActionNames(Set<String> actionNames)
   {
      this.actionNames = actionNames;
      return (this);
   }
}
