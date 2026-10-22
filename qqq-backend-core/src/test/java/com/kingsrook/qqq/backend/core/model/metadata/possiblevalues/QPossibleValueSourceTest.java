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

package com.kingsrook.qqq.backend.core.model.metadata.possiblevalues;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for QPossibleValueSource 
 *******************************************************************************/
class QPossibleValueSourceTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithValuesFromEnum()
   {
      assertThatThrownBy(() -> new QPossibleValueSource().withValuesFromEnum(DupeIds.values()))
         .isInstanceOf(QRuntimeException.class)
         .hasMessageContaining("Duplicated id(s)")
         .hasMessageMatching(".*: \\[1]$");
   }


   /***************************************************************************
    **
    ***************************************************************************/
   private enum DupeIds implements PossibleValueEnum<Integer>
   {
      ONE_A(1, "A"),
      TWO_B(2, "B"),
      ONE_C(1, "C");


      private final int id;
      private final String label;



      /***************************************************************************
       **
       ***************************************************************************/
      DupeIds(int id, String label)
      {
         this.id = id;
         this.label = label;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Integer getPossibleValueId()
      {
         return id;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         return label;
      }
   }
}