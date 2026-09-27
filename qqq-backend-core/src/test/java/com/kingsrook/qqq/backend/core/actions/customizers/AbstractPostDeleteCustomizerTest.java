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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostDeleteCustomizer
 *******************************************************************************/
class AbstractPostDeleteCustomizerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withCustomizer(TableCustomizers.POST_DELETE_RECORD.getRole(), new QCodeReference(AbstractPostDeleteCustomizerTest.PostDelete.class));

      TestUtils.insertRecords(table, List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Homer"),
         new QRecord().withValue("id", 2).withValue("firstName", "Marge"),
         new QRecord().withValue("id", 3).withValue("firstName", "Bart")
      ));

      ////////////////////////////////////////////////////////
      // try a delete that the post-customizer should reject //
      ////////////////////////////////////////////////////////
      {
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         deleteInput.setPrimaryKeys(List.of(1, 2));
         DeleteOutput deleteOutput = new DeleteAction().execute(deleteInput);
         assertEquals(0, deleteOutput.getRecordsWithErrors().size());
         assertEquals(1, deleteOutput.getRecordsWithWarnings().size());
         assertEquals(1, deleteOutput.getRecordsWithWarnings().get(0).getValue("id"));
         assertEquals(2, deleteOutput.getDeletedRecordCount());
         assertEquals("You shouldn't have deleted Homer...", deleteOutput.getRecordsWithWarnings().get(0).getWarnings().get(0).getMessage());

         GetInput getInput = new GetInput();
         getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         getInput.setPrimaryKey(1);
         GetOutput getOutput = new GetAction().execute(getInput);
         assertNull(getOutput.getRecord());

         getInput.setPrimaryKey(2);
         getOutput = new GetAction().execute(getInput);
         assertNull(getOutput.getRecord());
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PostDelete extends AbstractPostDeleteCustomizer
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         for(QRecord record : records)
         {
            if(record.getValue("firstName").equals("Homer"))
            {
               record.addWarning(new QWarningMessage("You shouldn't have deleted Homer..."));
            }
         }

         return (records);
      }

   }

}