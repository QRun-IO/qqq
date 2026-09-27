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

package com.kingsrook.qqq.backend.core.adapters;


import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.AbstractQFieldMapping;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QIndexBasedFieldMapping;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QKeyBasedFieldMapping;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;


/*******************************************************************************
 ** Adapter class to convert a JSON string into a QFieldMapping object
 **
 *******************************************************************************/
public class JsonToQFieldMappingAdapter
{

   /*******************************************************************************
    ** adapts a json string into an AbstractQFieldMapping.
    **
    ** The mapping will be a QKeyBasedFieldMapping if the keys in the json object are
    ** Strings.  It will be a QIndexBasedFieldMapping if the keys are integers.
    **
    *******************************************************************************/
   public AbstractQFieldMapping<?> buildMappingFromJson(String json)
   {
      if(!StringUtils.hasContent(json))
      {
         throw (new IllegalArgumentException("Empty json value was provided."));
      }

      try
      {
         JSONObject jsonObject = JsonUtils.toJSONObject(json);
         jsonObject = promoteInnerMappingIfAppropriate(jsonObject);

         //////////////////////////////////////////////////////////////////////////////////////////////
         // look at the keys in the mapping - if they're strings, then we're doing key-based mapping //
         // if they're numbers, then we're doing index based -- and if they're a mix, that's illegal //
         //////////////////////////////////////////////////////////////////////////////////////////////
         AbstractQFieldMapping.SourceType sourceType = determineSourceType(jsonObject);

         @SuppressWarnings("rawtypes")
         AbstractQFieldMapping mapping = null;

         switch(sourceType)
         {
            case KEY:
            {
               mapping = new QKeyBasedFieldMapping();
               for(String fieldName : jsonObject.keySet())
               {
                  ((QKeyBasedFieldMapping) mapping).addMapping(fieldName, jsonObject.getString(fieldName));
               }
               break;
            }
            case INDEX:
            {
               mapping = new QIndexBasedFieldMapping();
               for(String fieldName : jsonObject.keySet())
               {
                  ((QIndexBasedFieldMapping) mapping).addMapping(fieldName, jsonObject.getInt(fieldName));
               }
               break;
            }
            default:
            {
               throw (new IllegalArgumentException("Unsupported sourceType: " + sourceType));
            }
         }

         return (mapping);
      }
      catch(JSONException je)
      {
         throw (new IllegalArgumentException("Malformed JSON value: " + je.getMessage(), je));
      }
   }



   /*******************************************************************************
    ** So - this class was first written assuming that the JSON it would take would
    ** just be a mapping - e.g., {a:b, c:d} or {a:0, b:1}.
    **
    ** But - it turns out, callers may expect that they can create an instance of
    ** AbstractQFieldMapping, then serialize it, then de-serialize it, and that seems sane.
    **
    ** So - this method tries to determine if the JSON Object we took in looks like
    ** a serialized from of a AbstractQFieldMapping - and if so, then it "promotes"
    ** the "mapping" object from within that outer json object, since the rest of
    ** this class knows how to (and expects to) handle that object.
    *******************************************************************************/
   private JSONObject promoteInnerMappingIfAppropriate(JSONObject jsonObject)
   {
      if(jsonObject.has("mapping") && jsonObject.has("sourceType") && jsonObject.keySet().size() == 2)
      {
         return (jsonObject.getJSONObject("mapping"));
      }
      return (jsonObject);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AbstractQFieldMapping.SourceType determineSourceType(JSONObject jsonObject)
   {
      for(String fieldName : jsonObject.keySet())
      {
         Object sourceObject = jsonObject.get(fieldName);
         if(sourceObject instanceof String)
         {
            return (AbstractQFieldMapping.SourceType.KEY);
         }
         else if(sourceObject instanceof Integer)
         {
            return (AbstractQFieldMapping.SourceType.INDEX);
         }
         else
         {
            throw new IllegalArgumentException("Source object is unsupported type: " + sourceObject.getClass().getSimpleName());
         }
      }
      throw new IllegalArgumentException("No fields were found in the mapping.");
   }

}
