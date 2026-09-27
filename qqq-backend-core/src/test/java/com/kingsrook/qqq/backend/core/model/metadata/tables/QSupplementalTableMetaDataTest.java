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

package com.kingsrook.qqq.backend.core.model.metadata.tables;


import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QSupplementalTableMetaData 
 *******************************************************************************/
class QSupplementalTableMetaDataTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      QTableMetaData table = new QTableMetaData();
      String         type  = TestSupplementalTableMetaData.class.getName();

      ///////////////////////////////////////////
      // without anything set, of returns null //
      ///////////////////////////////////////////
      assertNull(QSupplementalTableMetaData.of(table, type));

      ////////////////////////////////////////////////////////////////////////
      // without anything set, ofOrWithNew constructs, assigns, and returns //
      ////////////////////////////////////////////////////////////////////////
      TestSupplementalTableMetaData ofOrWithNewOutput1 = QSupplementalTableMetaData.ofOrWithNew(table, type, () -> new TestSupplementalTableMetaData("A"));
      assertThat(ofOrWithNewOutput1)
         .isNotNull()
         .hasFieldOrPropertyWithValue("value", "A");

      /////////////////////////////////////
      // with a value set, of returns it //
      /////////////////////////////////////
      TestSupplementalTableMetaData ofOutput2;
      ofOutput2 = QSupplementalTableMetaData.of(table, type);
      assertThat(ofOutput2)
         .isNotNull()
         .isSameAs(ofOrWithNewOutput1)
         .hasFieldOrPropertyWithValue("value", "A");

      /////////////////////////////////////////////////////////////////////
      // with a value set, ofOrWithNew returns that value, not a new one //
      /////////////////////////////////////////////////////////////////////
      assertThat(QSupplementalTableMetaData.ofOrWithNew(table, type, () -> new TestSupplementalTableMetaData("B")))
         .isNotNull()
         .isSameAs(ofOrWithNewOutput1)
         .hasFieldOrPropertyWithValue("value", "A");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static class TestSupplementalTableMetaData extends QSupplementalTableMetaData
   {
      private final String value;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public TestSupplementalTableMetaData(String value)
      {
         this.value = value;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public String getType()
      {
         return getClass().getName();
      }

   }
}