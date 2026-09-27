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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for AbstractPreUpdateCustomizer
 *******************************************************************************/
class AbstractPostUpdateCustomizerTest extends BaseTest
{
   private static final String NAME_CHANGES_TABLE = "nameChanges";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withCustomizer(TableCustomizers.POST_UPDATE_RECORD.getRole(), new QCodeReference(PostUpdate.class));

      qInstance.addTable(new QTableMetaData()
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withName(NAME_CHANGES_TABLE)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("personId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("message", QFieldType.STRING)));

      TestUtils.insertRecords(table, List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Homer"),
         new QRecord().withValue("id", 2).withValue("firstName", "Marge"),
         new QRecord().withValue("id", 3).withValue("firstName", "Bart")
      ));

      ///////////////////////////////////////////////////////////////////////////////
      // try an update where the post-update customizer will insert another record //
      ///////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("firstName", "Homer J.")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(CollectionUtils.nullSafeIsEmpty(updateOutput.getRecords().get(0).getErrors()));

         GetInput getInput = new GetInput();
         getInput.setTableName(NAME_CHANGES_TABLE);
         getInput.setPrimaryKey(1);
         GetOutput getOutput = new GetAction().execute(getInput);
         assertEquals(1, getOutput.getRecord().getValueInteger("personId"));
         assertEquals("Changed first name from [Homer] to [Homer J.]", getOutput.getRecord().getValueString("message"));
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try an update where the post-update customizer will issue a warning (though will have updated the record too) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("firstName", "Warning")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(CollectionUtils.nullSafeIsEmpty(updateOutput.getRecords().get(0).getErrors()));
         assertTrue(updateOutput.getRecords().get(0).getWarnings().stream().anyMatch(s -> s.getMessage().contains("updated to a warning value")));

         GetInput getInput = new GetInput();
         getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         getInput.setPrimaryKey(1);
         GetOutput getOutput = new GetAction().execute(getInput);
         assertEquals("Warning", getOutput.getRecord().getValueString("firstName"));
      }

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // try an update where the post-update customizer will throw an error (resulting in an updated record with a warning) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         updateInput.setRecords(List.of(new QRecord().withValue("id", 1).withValue("firstName", "throw")));
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         assertTrue(CollectionUtils.nullSafeIsEmpty(updateOutput.getRecords().get(0).getErrors()));
         assertTrue(updateOutput.getRecords().get(0).getWarnings().stream().anyMatch(s -> s.getMessage().contains("Forced Exception")));

         GetInput getInput = new GetInput();
         getInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
         getInput.setPrimaryKey(1);
         GetOutput getOutput = new GetAction().execute(getInput);
         assertEquals("throw", getOutput.getRecord().getValueString("firstName"));
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PostUpdate extends AbstractPostUpdateCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         List<QRecord> nameChangeRecordsToInsert = new ArrayList<>();

         for(QRecord record : records)
         {
            boolean recordHadError          = CollectionUtils.nullSafeHasContents(record.getErrors());
            boolean inputRecordHadFirstName = record.getValues().containsKey("firstName");
            boolean inputRecordHadLastName  = record.getValues().containsKey("lastName");

            if(recordHadError)
            {
               continue;
            }

            if(inputRecordHadFirstName)
            {
               QRecord oldRecord = getOldRecordMap().get(record.getValue("id"));
               if(oldRecord != null && oldRecord.getValue("firstName") != null)
               {
                  nameChangeRecordsToInsert.add(new QRecord()
                     .withValue("personId", record.getValue("id"))
                     .withValue("message", "Changed first name from [" + oldRecord.getValueString("firstName") + "] to [" + record.getValueString("firstName") + "]")
                  );
               }

               if("warning".equalsIgnoreCase(record.getValueString("firstName")))
               {
                  record.addWarning(new QWarningMessage("Record was updated to a warning value"));
               }

               if("throw".equalsIgnoreCase(record.getValueString("firstName")))
               {
                  throw (new QException("Forced Exception"));
               }
            }

            if(inputRecordHadLastName)
            {
               QRecord oldRecord = getOldRecordMap().get(record.getValue("id"));
               if(oldRecord != null && oldRecord.getValue("lastName") != null)
               {
                  nameChangeRecordsToInsert.add(new QRecord()
                     .withValue("personId", record.getValue("id"))
                     .withValue("message", "Changed last name from [" + oldRecord.getValueString("lastName") + "] to [" + record.getValueString("lastName") + "]")
                  );
               }
            }
         }

         if(CollectionUtils.nullSafeHasContents(nameChangeRecordsToInsert))
         {
            InsertInput insertInput = new InsertInput();
            insertInput.setTableName(NAME_CHANGES_TABLE);
            insertInput.setRecords(nameChangeRecordsToInsert);
            InsertOutput insertOutput = new InsertAction().execute(insertInput);
         }

         return (records);
      }
   }

}