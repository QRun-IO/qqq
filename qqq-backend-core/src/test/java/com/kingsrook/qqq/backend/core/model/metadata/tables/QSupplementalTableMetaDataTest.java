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