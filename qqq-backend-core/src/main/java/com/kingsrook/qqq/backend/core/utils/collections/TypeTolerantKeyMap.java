/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Version of map where string keys are handled case-insensitively.  e.g.,
 ** map.put("One", 1); map.get("ONE") == 1.
 *******************************************************************************/
public class TypeTolerantKeyMap<V> extends TransformedKeyMap<Serializable, Serializable, V>
{

   /***************************************************************************
    *
    ***************************************************************************/
   public TypeTolerantKeyMap(QFieldType qFieldType)
   {
      super(key -> ValueUtils.getValueAsFieldType(qFieldType, key));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public TypeTolerantKeyMap(QFieldType qFieldType, Supplier<Map<Serializable, V>> supplier)
   {
      super(key -> ValueUtils.getValueAsFieldType(qFieldType, key), supplier);
   }


   /***************************************************************************
    *
    ***************************************************************************/
   public TypeTolerantKeyMap(Class<? extends Serializable> c)
   {
      super(key -> ValueUtils.getValueAsType(c, key));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public TypeTolerantKeyMap(Class<? extends Serializable> c, Supplier<Map<Serializable, V>> supplier)
   {
      super(key -> ValueUtils.getValueAsType(c, key), supplier);
   }

}
