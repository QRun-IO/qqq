/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.api.utils;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Help parse query strings into maps.
 *******************************************************************************/
public class QueryStringParser
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<String, String> parseQueryStringSingleValuePerKey(String queryString)
   {
      Map<String, String> rs = new LinkedHashMap<>();
      if(StringUtils.hasContent(queryString))
      {
         for(String nameValuePair : queryString.split("&"))
         {
            String[] nameAndValue = nameValuePair.split("=", 2);
            String   name         = nameAndValue[0];
            String   value        = nameAndValue.length > 1 ? nameAndValue[1] : "";
            rs.put(name, value);
         }
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<String, List<String>> parseQueryStringMultiValuePerKey(String queryString)
   {
      Map<String, List<String>> rs = new LinkedHashMap<>();
      if(StringUtils.hasContent(queryString))
      {
         for(String nameValuePair : queryString.split("&"))
         {
            String[] nameAndValue = nameValuePair.split("=", 2);
            String   name         = nameAndValue[0];
            String   value        = nameAndValue.length > 1 ? nameAndValue[1] : "";
            rs.computeIfAbsent(name, (key) -> new ArrayList<>());
            rs.get(name).add(value);
         }
      }

      return (rs);
   }

}
