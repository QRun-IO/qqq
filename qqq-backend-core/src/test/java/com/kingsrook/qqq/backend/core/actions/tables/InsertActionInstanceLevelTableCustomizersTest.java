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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 **
 *******************************************************************************/
public class InsertActionInstanceLevelTableCustomizersTest extends BaseTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInstanceLevelCustomizers() throws QException
   {
      QContext.getQInstance().withTableCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(BreaksEverythingCustomizer.class));
      QRecord record = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SHAPE).withRecord(new QRecord().withValue("name", "octogon"))).getRecords().get(0);
      assertEquals("Everything is broken", record.getErrorsAsString());
      assertNull(record.getValueInteger("id"));

      QContext.getQInstance().setTableCustomizers(new ListingHash<>());
      QContext.getQInstance().withTableCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(SetsFirstName.class));
      QContext.getQInstance().withTableCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(SetsLastName.class));
      QContext.getQInstance().withTableCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(DoesNothing.class));
      DoesNothing.callCount = 0;
      record = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecord(new QRecord().withValue("noOfShoes", 3))).getRecords().get(0);
      assertEquals("Jeff", record.getValueString("firstName"));
      assertEquals("Smith", record.getValueString("lastName"));
      assertNotNull(record.getValueInteger("id"));
      assertEquals(1, DoesNothing.callCount);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class BreaksEverythingCustomizer implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
      {
         records.forEach(r -> r.addError(new SystemErrorStatusMessage("Everything is broken")));
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class SetsFirstName implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
      {
         records.forEach(r -> r.setValue("firstName", "Jeff"));
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class SetsLastName implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsertOrUpdate(AbstractActionInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
      {
         records.forEach(r -> r.setValue("lastName", "Smith"));
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class DoesNothing implements TableCustomizerInterface
   {
      static int callCount = 0;



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public List<QRecord> postInsertOrUpdate(AbstractActionInput input, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
      {
         callCount++;
         return records;
      }
   }
}
