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

package com.kingsrook.qqq.backend.core.processes.implementations.savedviews;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for all saved view processes
 *******************************************************************************/
class SavedViewProcessTests extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   @Test
   void test() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new SavedViewsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;

      {
         /////////////////////////////////////////
         // query - should be no views to start //
         /////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(QuerySavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("tableName", tableName);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         assertEquals(0, ((List<?>) runProcessOutput.getValues().get("savedViewList")).size());
      }

      Integer savedViewId;
      {
         //////////////////////
         // store a new view //
         //////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("label", "My View");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("viewJson", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 47))));
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedViewList    = (List<QRecord>) runProcessOutput.getValues().get("savedViewList");
         assertEquals(1, savedViewList.size());
         savedViewId = savedViewList.get(0).getValueInteger("id");
         assertNotNull(savedViewId);

         //////////////////////////////////////////////////////////////////
         // try to store it again - should throw a "duplicate" exception //
         //////////////////////////////////////////////////////////////////
         assertThatThrownBy(() -> new RunProcessAction().execute(runProcessInput))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("already have a saved view");
      }

      {
         ///////////////////////////////////
         // query - should find our views //
         ///////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(QuerySavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("tableName", tableName);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedViewList    = (List<QRecord>) runProcessOutput.getValues().get("savedViewList");
         assertEquals(1, savedViewList.size());
         assertEquals(1, savedViewList.get(0).getValueInteger("id"));
         assertEquals("My View", savedViewList.get(0).getValueString("label"));
      }

      {
         /////////////////////
         // update our view //
         /////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("id", savedViewId);
         runProcessInput.addValue("label", "My Updated View");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("viewJson", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 47))));
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedViewList    = (List<QRecord>) runProcessOutput.getValues().get("savedViewList");
         assertEquals(1, savedViewList.size());
         assertEquals(1, savedViewList.get(0).getValueInteger("id"));
         assertEquals("My Updated View", savedViewList.get(0).getValueString("label"));
      }

      Integer anotherSavedViewId;
      {
         /////////////////////////////////////////////////////////////////////////////////////////////
         // store a second one w/ different name (will be used below in update-dupe-check use-case) //
         /////////////////////////////////////////////////////////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("label", "My Second View");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("viewJson", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 47))));
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedViewList    = (List<QRecord>) runProcessOutput.getValues().get("savedViewList");
         anotherSavedViewId = savedViewList.get(0).getValueInteger("id");
      }

      {
         /////////////////////////////////////////////////
         // try to rename the second to match the first //
         /////////////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("id", anotherSavedViewId);
         runProcessInput.addValue("label", "My Updated View");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("viewJson", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 47))));

         //////////////////////////////////////////
         // should throw a "duplicate" exception //
         //////////////////////////////////////////
         assertThatThrownBy(() -> new RunProcessAction().execute(runProcessInput))
            .isInstanceOf(QUserFacingException.class)
            .hasMessageContaining("already have a saved view");
      }

      {
         //////////////////////
         // delete our views //
         //////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(DeleteSavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("id", savedViewId);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

         runProcessInput.addValue("id", anotherSavedViewId);
         runProcessOutput = new RunProcessAction().execute(runProcessInput);
      }

      {
         //////////////////////////////////////
         // query - should be no views again //
         //////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(QuerySavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("tableName", tableName);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         assertEquals(0, ((List<?>) runProcessOutput.getValues().get("savedViewList")).size());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotFoundThrowsProperly() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new SavedViewsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;

      {
         ////////////////////////////////////////////////////////
         // get one by id when it doesn't exist - should throw //
         ////////////////////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(QuerySavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("id", -1);
         assertThatThrownBy(() -> new RunProcessAction().execute(runProcessInput))
            .hasMessageContaining("view was not found")
            .isInstanceOf(QUserFacingException.class);
      }

      Integer savedViewId;
      {
         //////////////////////
         // store a new view //
         //////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(StoreSavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("label", "My View");
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("viewJson", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 47))));
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedViewList    = (List<QRecord>) runProcessOutput.getValues().get("savedViewList");
         assertEquals(1, savedViewList.size());
         savedViewId = savedViewList.get(0).getValueInteger("id");
         assertNotNull(savedViewId);
      }

      {
         ////////////////////////////////////////
         // get now with valid id, should work //
         ////////////////////////////////////////
         RunProcessInput runProcessInput = new RunProcessInput();
         runProcessInput.setProcessName(QuerySavedViewProcess.getProcessMetaData().getName());
         runProcessInput.addValue("tableName", tableName);
         runProcessInput.addValue("id", savedViewId);
         RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
         List<QRecord>    savedViewList    = (List<QRecord>) runProcessOutput.getValues().get("savedViewList");
         assertEquals(1, savedViewList.size());
         assertEquals(1, savedViewList.get(0).getValueInteger("id"));
         assertEquals("My View", savedViewList.get(0).getValueString("label"));
      }
   }

}