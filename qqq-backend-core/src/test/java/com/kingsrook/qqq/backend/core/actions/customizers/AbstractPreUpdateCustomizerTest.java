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
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for AbstractPreUpdateCustomizer
 *******************************************************************************/
class AbstractPreUpdateCustomizerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD.getRole(), new QCodeReference(PreUpdate.class));

      TestUtils.insertRecords(table, List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Homer"),
         new QRecord().withValue("id", 2).withValue("firstName", "Marge"),
         new QRecord().withValue("id", 3).withValue("firstName", "Bart")
      ));

      /////////////////////////////////////////////////////////
      // try an update that the pre-customizer should reject //
      /////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("firstName", "--")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(updateOutput.getRecords().get(0).getErrors().stream().anyMatch(s -> s.getMessage().contains("must contain at least one letter")));

         GetInput getInput = new GetInput();
         getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         getInput.setPrimaryKey(1);
         GetOutput getOutput = new GetAction().execute(getInput);
         assertEquals("Homer", getOutput.getRecord().getValueString("firstName"));
      }

      //////////////////////////////////////////////
      // try an update that gets its data changed //
      //////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 2).withValue("firstName", "Ms.")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(updateOutput.getRecords().get(0).getErrors().isEmpty());
         assertEquals("Ms.", updateOutput.getRecords().get(0).getValueString("firstName"));
         assertEquals("Simpson", updateOutput.getRecords().get(0).getValueString("lastName"));

         GetInput getInput = new GetInput();
         getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         getInput.setPrimaryKey(2);
         GetOutput getOutput = new GetAction().execute(getInput);
         assertEquals("Ms.", getOutput.getRecord().getValueString("firstName"));
         assertEquals("Simpson", getOutput.getRecord().getValueString("lastName"));
      }

      //////////////////////////////////////////////////////////////////////////
      // try an update that uses data from the previous version of the record //
      //////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 3).withValue("lastName", "Simpson")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(updateOutput.getRecords().get(0).getErrors().isEmpty());
         assertEquals("BART", updateOutput.getRecords().get(0).getValueString("firstName"));
         assertEquals("Simpson", updateOutput.getRecords().get(0).getValueString("lastName"));

         GetInput getInput = new GetInput();
         getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         getInput.setPrimaryKey(3);
         GetOutput getOutput = new GetAction().execute(getInput);
         assertEquals("BART", getOutput.getRecord().getValueString("firstName"));
         assertEquals("Simpson", getOutput.getRecord().getValueString("lastName"));
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PreUpdate extends AbstractPreUpdateCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         for(QRecord record : records)
         {
            boolean inputRecordHadFirstName = record.getValues().containsKey("firstName");
            boolean inputRecordHadLastName  = record.getValues().containsKey("lastName");

            if(inputRecordHadFirstName)
            {
               ////////////////////////////////////////////////////////////////
               // if updating first name, give an error if it has no letters //
               ////////////////////////////////////////////////////////////////
               if(!record.getValueString("firstName").matches(".*\\w.*"))
               {
                  record.addError(new BadInputStatusMessage("First name must contain at least one letter."));
               }

               //////////////////////////////////////////////////////////////
               // if setting firstname to Ms., update last name to Simpson //
               //////////////////////////////////////////////////////////////
               if(record.getValueString("firstName").equals("Ms."))
               {
                  record.setValue("lastName", "Simpson");
               }
            }

            //////////////////////////////////////////////////////////////////////////
            // if updating the person's last name, set their first name to all caps //
            //////////////////////////////////////////////////////////////////////////
            if(inputRecordHadLastName)
            {
               QRecord oldRecord = getOldRecordMap().get(record.getValue("id"));
               if(oldRecord != null && oldRecord.getValue("firstName") != null)
               {
                  record.setValue("firstName", oldRecord.getValueString("firstName").toUpperCase());
               }
            }
         }

         return (records);
      }
   }

}