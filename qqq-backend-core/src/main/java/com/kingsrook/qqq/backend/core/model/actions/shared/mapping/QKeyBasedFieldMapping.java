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

package com.kingsrook.qqq.backend.core.model.actions.shared.mapping;


import java.util.LinkedHashMap;
import java.util.Map;


/*******************************************************************************
 ** Field Mapping implementation that uses string keys (e.g., from a CSV file
 ** with a header row, or from one JSON object to the proper qqq field names)
 **
 *******************************************************************************/
public class QKeyBasedFieldMapping extends AbstractQFieldMapping<String>
{
   private Map<String, String> mapping;



   /*******************************************************************************
    ** Get the source field (e.g., name that's in the CSV header or the input json
    ** object) corresponding to a proper qqq table fieldName.
    **
    *******************************************************************************/
   @Override
   public String getFieldSource(String fieldName)
   {
      if(mapping == null)
      {
         return (null);
      }

      return (mapping.get(fieldName));
   }



   /*******************************************************************************
    ** Tell framework what kind of keys this mapping class uses (KEY)
    **
    *******************************************************************************/
   @Override
   public SourceType getSourceType()
   {
      return (SourceType.KEY);
   }



   /*******************************************************************************
    ** Add a single mapping to this mapping object.  fieldName = qqq metaData fieldName,
    ** key = field name in the CSV or source-json, for example.
    **
    *******************************************************************************/
   public void addMapping(String fieldName, String key)
   {
      if(mapping == null)
      {
         mapping = new LinkedHashMap<>();
      }
      mapping.put(fieldName, key);
   }



   /*******************************************************************************
    ** Fluently add a single mapping to this mapping object.  fieldName = qqq metaData fieldName,
    ** key = field name in the CSV or source-json, for example.
    **
    *******************************************************************************/
   public QKeyBasedFieldMapping withMapping(String fieldName, String key)
   {
      addMapping(fieldName, key);
      return (this);
   }



   /*******************************************************************************
    ** Getter for mapping
    **
    *******************************************************************************/
   public Map<String, String> getMapping()
   {
      return mapping;
   }



   /*******************************************************************************
    ** Setter for mapping
    **
    *******************************************************************************/
   public void setMapping(Map<String, String> mapping)
   {
      this.mapping = mapping;
   }

}
