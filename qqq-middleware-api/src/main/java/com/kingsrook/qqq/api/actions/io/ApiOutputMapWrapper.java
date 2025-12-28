/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
