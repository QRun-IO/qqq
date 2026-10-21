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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.math.BigDecimal;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TypeTolerantKeyMap 
 *******************************************************************************/
class TypeTolerantKeyMapTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      TypeTolerantKeyMap<QRecord> map = new TypeTolerantKeyMap<>(QFieldType.INTEGER);
      map.put(1, new QRecord().withValue("id", 1));
      map.put("2", new QRecord().withValue("id", 2));
      map.put(3.0, new QRecord().withValue("id", 3));
      map.put(new BigDecimal("4.00"), new QRecord().withValue("id", 4));

      for(int i=1; i<=4; i++)
      {
         assertTrue(map.containsKey(i));
         assertEquals(i, map.get(i).getValueInteger("id"));
      }
   }

}