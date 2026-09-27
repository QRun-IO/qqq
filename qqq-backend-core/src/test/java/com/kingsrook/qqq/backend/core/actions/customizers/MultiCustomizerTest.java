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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceWithProperties;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for MultiCustomizer 
 *******************************************************************************/
class MultiCustomizerTest extends BaseTest
{
   private static List<String> events = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      events.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, MultiCustomizer.of(
            new QCodeReference(CustomizerA.class),
            new QCodeReference(CustomizerB.class)
         ));
      reInitInstanceInContext(qInstance);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(2)
         .contains("CustomizerA.preInsert")
         .contains("CustomizerB.preInsert");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAddingMore() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();

      QCodeReferenceWithProperties multiCustomizer = MultiCustomizer.of(new QCodeReference(CustomizerA.class));
      MultiCustomizer.addTableCustomizer(multiCustomizer, new QCodeReference(CustomizerB.class));

      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withCustomizer(TableCustomizers.PRE_INSERT_RECORD, multiCustomizer);
      reInitInstanceInContext(qInstance);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord()));
      assertThat(events).hasSize(2)
         .contains("CustomizerA.preInsert")
         .contains("CustomizerB.preInsert");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class CustomizerA implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
      {
         events.add("CustomizerA.preInsert");
         return (records);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class CustomizerB implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
      {
         events.add("CustomizerB.preInsert");
         return (records);
      }
   }

}