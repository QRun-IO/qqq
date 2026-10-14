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

package com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfile;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfileMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit tests for all saved-bulk-load-profile processes
 *******************************************************************************/
class SavedBulkLoadProfileProcessTests extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new SavedBulkLoadProfileMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;

      {
         ////////////////////////////////////////////
         // query - should be no profiles to start //
         ////////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(QuerySavedBulkLoadProfileProcess.getProcessMetaData().getName());
         runProcessInput.addValue("tableName", tableName);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         assertEquals(0, ((List<?>) runProcessOutput.getValues().get("savedBulkLoadProfileList")).size());
      }

      Integer savedBulkLoadProfileId;
      {
         /////////////////////////
         // store a new profile //
         /////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedBulkLoadProfileProcess.getProcessMetaData().getName());
         runProcessInput.addValue("label", "My Profile");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("mappingJson", JsonUtils.toJson(new BulkLoadProfile()));
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedBulkLoadProfileList    = (List<QRecord>) runProcessOutput.getValues().get("savedBulkLoadProfileList");
         assertEquals(1, savedBulkLoadProfileList.size());
         savedBulkLoadProfileId = savedBulkLoadProfileList.get(0).getValueInteger("id");
         assertNotNull(savedBulkLoadProfileId);

         //////////////////////////////////////////////////////////////////
         // try to store it again - should throw a "duplicate" exception //
         //////////////////////////////////////////////////////////////////
         assertThatThrownBy(() -> new RunProcessAction().execute(runProcessInput))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("already have a saved Bulk Load Profile");
      }

      {
         //////////////////////////////////////
         // query - should find our profiles //
         //////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(QuerySavedBulkLoadProfileProcess.getProcessMetaData().getName());
         runProcessInput.addValue("tableName", tableName);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedBulkLoadProfileList    = (List<QRecord>) runProcessOutput.getValues().get("savedBulkLoadProfileList");
         assertEquals(1, savedBulkLoadProfileList.size());
         assertEquals(1, savedBulkLoadProfileList.get(0).getValueInteger("id"));
         assertEquals("My Profile", savedBulkLoadProfileList.get(0).getValueString("label"));
      }

      {
         ////////////////////////
         // update our Profile //
         ////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedBulkLoadProfileProcess.getProcessMetaData().getName());
         runProcessInput.addValue("id", savedBulkLoadProfileId);
         runProcessInput.addValue("label", "My Updated Profile");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("mappingJson", JsonUtils.toJson(new BulkLoadProfile()));
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedBulkLoadProfileList    = (List<QRecord>) runProcessOutput.getValues().get("savedBulkLoadProfileList");
         assertEquals(1, savedBulkLoadProfileList.size());
         assertEquals(1, savedBulkLoadProfileList.get(0).getValueInteger("id"));
         assertEquals("My Updated Profile", savedBulkLoadProfileList.get(0).getValueString("label"));
      }

      Integer anotherSavedProfileId;
      {
         /////////////////////////////////////////////////////////////////////////////////////////////
         // store a second one w/ different name (will be used below in update-dupe-check use-case) //
         /////////////////////////////////////////////////////////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedBulkLoadProfileProcess.getProcessMetaData().getName());
         runProcessInput.addValue("label", "My Second Profile");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("mappingJson", JsonUtils.toJson(new BulkLoadProfile()));
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedBulkLoadProfileList    = (List<QRecord>) runProcessOutput.getValues().get("savedBulkLoadProfileList");
         anotherSavedProfileId = savedBulkLoadProfileList.get(0).getValueInteger("id");
      }

      {
         /////////////////////////////////////////////////
         // try to rename the second to match the first //
         /////////////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedBulkLoadProfileProcess.getProcessMetaData().getName());
         runProcessInput.addValue("id", anotherSavedProfileId);
         runProcessInput.addValue("label", "My Updated Profile");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("mappingJson", JsonUtils.toJson(new BulkLoadProfile()));

         //////////////////////////////////////////
         // should throw a "duplicate" exception //
         //////////////////////////////////////////
         assertThatThrownBy(() -> new RunProcessAction().execute(runProcessInput))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("already have a saved Bulk Load Profile");
      }

      {
         /////////////////////////
         // delete our profiles //
         /////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(DeleteSavedBulkLoadProfileProcess.getProcessMetaData().getName());
         runProcessInput.addValue("id", savedBulkLoadProfileId);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

         runProcessInput.addValue("id", anotherSavedProfileId);
         runProcessOutput = new RunProcessAction().execute(runProcessInput);
      }

      {
         /////////////////////////////////////////
         // query - should be no profiles again //
         /////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(QuerySavedBulkLoadProfileProcess.getProcessMetaData().getName());
         runProcessInput.addValue("tableName", tableName);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         assertEquals(0, ((List<?>) runProcessOutput.getValues().get("savedBulkLoadProfileList")).size());
      }
   }




   /*******************************************************************************
    ** An update keeps the profile's owner, whoever saves it (a user it is
    ** shared with must not take it over).
    *******************************************************************************/
   @Test
   void testUpdateKeepsOwner() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new SavedBulkLoadProfileMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;

      QContext.getQSession().setUser(new QUser().withIdReference("owner"));
      Integer id = store(null, "Owner's Profile", tableName).get(0).getValueInteger("id");

      QContext.getQSession().setUser(new QUser().withIdReference("someone-else"));
      store(id, "Renamed", tableName);

      QRecord record = new GetAction().executeForRecord(new GetInput(SavedBulkLoadProfile.TABLE_NAME).withPrimaryKey(id));
      assertEquals("Renamed", record.getValueString("label"));
      assertEquals("owner", record.getValueString("userId"));

      assertThatThrownBy(() -> store(9999, "Missing", tableName))
         .isInstanceOf(QNotFoundException.class);
   }



   /*******************************************************************************
    ** A record the backend refuses is an error, not a silent success.
    *******************************************************************************/
   @Test
   void testRecordErrorsAreThrown() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new SavedBulkLoadProfileMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      ////////////////////////////////////////////////////////////////
      // tableName is limited to 250 characters, with ERROR behavior //
      ////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> store(null, "Too Long", "x".repeat(300)))
         .isInstanceOf(QUserFacingException.class)
         .hasMessageContaining("Error saving Bulk Load Profile");
   }



   /*******************************************************************************
    ** Run the store process.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static List<QRecord> store(Integer id, String label, String tableName) throws QException
   {
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(StoreSavedBulkLoadProfileProcess.getProcessMetaData().getName());
      runProcessInput.addValue("id", id);
      runProcessInput.addValue("label", label);
      runProcessInput.addValue("tableName", tableName);
      runProcessInput.addValue("mappingJson", JsonUtils.toJson(new BulkLoadProfile()));
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      return ((List<QRecord>) runProcessOutput.getValues().get("savedBulkLoadProfileList"));
   }
}
