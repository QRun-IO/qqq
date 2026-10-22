/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions.io;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/***************************************************************************
 ** implementation of ApiOutputRecordWrapperInterface that wraps a Map
 ***************************************************************************/
public class ApiOutputMapWrapper implements ApiOutputRecordWrapperInterface<Map<String, Serializable>, ApiOutputMapWrapper>
{
   private Map<String, Serializable> apiMap;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ApiOutputMapWrapper(Map<String, Serializable> apiMap)
   {
      this.apiMap = apiMap;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void putValue(String key, Serializable value)
   {
      apiMap.put(key, value);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void putAssociation(String key, List<Map<String, Serializable>> values)
   {
      ArrayList<Map<String, Serializable>> arrayList = CollectionUtils.useOrWrap(values, new TypeToken<>() {});
      apiMap.put(key, arrayList);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ApiOutputMapWrapper newSibling(String tableName)
   {
      return new ApiOutputMapWrapper(new LinkedHashMap<>());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Serializable> getContents()
   {
      return this.apiMap;
   }

}
