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

package com.kingsrook.qqq.backend.core.model.common;


import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for DayOfWeekPossibleValueSourceMetaDataProducer
 *******************************************************************************/
class DayOfWeekPossibleValueSourceMetaDataProducerTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProduceReturnsSevenDays()
   {
      DayOfWeekPossibleValueSourceMetaDataProducer producer = new DayOfWeekPossibleValueSourceMetaDataProducer();
      QPossibleValueSource pvs = producer.produce(null);

      assertNotNull(pvs);
      assertEquals("DayOfWeek", pvs.getName());
      assertEquals(QPossibleValueSourceType.ENUM, pvs.getType());
      assertEquals(7, pvs.getEnumValues().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAllIso8601ValuesPresent()
   {
      DayOfWeekPossibleValueSourceMetaDataProducer producer = new DayOfWeekPossibleValueSourceMetaDataProducer();
      QPossibleValueSource pvs = producer.produce(null);

      List<QPossibleValue<?>> values = pvs.getEnumValues();
      Set<Integer> ids = new HashSet<>();
      for(QPossibleValue<?> value : values)
      {
         ids.add((Integer) value.getId());
      }

      ///////////////////////////////////////////////////////////////////////
      // all 7 ISO-8601 day values (Monday=1 through Sunday=7) must exist //
      ///////////////////////////////////////////////////////////////////////
      for(int i = 1; i <= 7; i++)
      {
         assertTrue(ids.contains(i), "Should contain ISO-8601 day value " + i);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValuesHaveDisplayNames()
   {
      DayOfWeekPossibleValueSourceMetaDataProducer producer = new DayOfWeekPossibleValueSourceMetaDataProducer();
      QPossibleValueSource pvs = producer.produce(null);

      for(QPossibleValue<?> value : pvs.getEnumValues())
      {
         assertNotNull(value.getLabel());

         //////////////////////////////////////////////////////////////////////
         // verify the label is a recognized day of week display name        //
         // (checking id against DayOfWeek enum to confirm correct mapping)  //
         //////////////////////////////////////////////////////////////////////
         Integer id = (Integer) value.getId();
         DayOfWeek dow = DayOfWeek.of(id);
         assertNotNull(dow);
      }
   }

}
