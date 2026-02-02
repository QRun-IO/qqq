/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.savedviews;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.savedviews.QuickSavedView;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedviews.SharedSavedView;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QuerySavedViewProcess 
 *******************************************************************************/
class QuerySavedViewProcessTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      MemoryRecordStore.getInstance().setBuildJoinCrossProductFromJoinContext(true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      MemoryRecordStore.getInstance().setBuildJoinCrossProductFromJoinContext(MemoryRecordStore.BUILD_JOIN_CROSS_PRODUCT_FROM_JOIN_CONTEXT_DEFAULT);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuickSavedViews() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      new SavedViewsMetaDataProvider()
         .withIsQuickSavedViewEnabled(true)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;

      //////////////////////////////////////////////
      // start with nothing saved, empty use-case //
      //////////////////////////////////////////////
      {
         List<QRecord> savedViewList = runQuerySavedViewsProcess(tableName);
         assertEquals(0, savedViewList.size());
      }

      /////////////////////////////////////////////////////
      // insert one saved view, but no quick saved views //
      /////////////////////////////////////////////////////
      Integer savedView0Id;
      {
         new InsertAction().execute(new InsertInput(SavedView.TABLE_NAME).withRecordEntity(
            new SavedView().withTableName(tableName).withLabel("one").withUserId(QContext.getQSession().getUser().getIdReference())));
         List<QRecord> savedViewList = runQuerySavedViewsProcess(tableName);
         assertEquals(1, savedViewList.size());

         QRecord savedView0 = savedViewList.get(0);
         assertNull(savedView0.getValue("type"));
         savedView0Id = savedView0.getValueInteger("id");
      }

      ///////////////////////////////////////////////////////////
      // insert a quick saved view referencing that saved view //
      ///////////////////////////////////////////////////////////
      {
         new InsertAction().execute(new InsertInput(QuickSavedView.TABLE_NAME).withRecordEntity(
            new QuickSavedView().withSavedViewId(savedView0Id).withDoCount(true).withSortOrder(17)));
         List<QRecord> savedViewList = runQuerySavedViewsProcess(tableName);
         assertEquals(1, savedViewList.size());

         QRecord savedView0 = savedViewList.get(0);
         assertEquals("quickView", savedView0.getValue("type"));
         assertTrue(savedView0.getValueBoolean("doCount"));
         assertEquals(17, savedView0.getValue("sortOrder"));
      }

      //////////////////////////////////////////////////////////////////////////////////////////////
      // insert a 2nd quick saved view, referencing a non-existing saved view (should be ignored) //
      //////////////////////////////////////////////////////////////////////////////////////////////
      {
         new InsertAction().execute(new InsertInput(QuickSavedView.TABLE_NAME).withRecordEntity(
            new QuickSavedView().withSavedViewId(savedView0Id + 1).withDoCount(true).withSortOrder(17)));
         List<QRecord> savedViewList = runQuerySavedViewsProcess(tableName);
         assertEquals(1, savedViewList.size());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQuerySavedViewProcessWithoutQuickSavedViewsInInstance() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      new SavedViewsMetaDataProvider()
         .withIsQuickSavedViewEnabled(false)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;

      //////////////////////////////////////////////
      // start with nothing saved, empty use-case //
      //////////////////////////////////////////////
      {
         List<QRecord> savedViewList = runQuerySavedViewsProcess(tableName);
         assertEquals(0, savedViewList.size());
      }

      /////////////////////////////////////////////////
      // insert one saved view, make sure that works //
      /////////////////////////////////////////////////
      {
         new InsertAction().execute(new InsertInput(SavedView.TABLE_NAME).withRecordEntity(
            new SavedView().withTableName(tableName).withLabel("one").withUserId(QContext.getQSession().getUser().getIdReference())));
         List<QRecord> savedViewList = runQuerySavedViewsProcess(tableName);
         assertEquals(1, savedViewList.size());

         QRecord savedView0 = savedViewList.get(0);
         assertNull(savedView0.getValue("type"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testApplicationCanOverrideLookupQuickViewsMethod() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      new SavedViewsMetaDataProvider()
         .withIsQuickSavedViewEnabled(false)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);
      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;

      {
         /////////////////////////////////////
         // baseline - no saved views exist //
         /////////////////////////////////////
         List<QRecord> savedViewList = runQuerySavedViewsProcess(tableName);
         assertEquals(0, savedViewList.size());
      }

      ////////////////////////////////////////
      // update the process to use our code //
      ////////////////////////////////////////
      QStepMetaData step = QContext.getQInstance().getProcess(QuerySavedViewProcess.getProcessMetaData().getName())
         .getStepList().get(0);
      ((QBackendStepMetaData) step).setCode(new QCodeReference(CustomQuerySavedViewProcessStep.class));

      {
         //////////////////////////////////////////////////
         // now we get a custom quick-view in the output //
         //////////////////////////////////////////////////
         List<QRecord> savedViewList = runQuerySavedViewsProcess(tableName);
         assertEquals(1, savedViewList.size());
         QRecord savedView0 = savedViewList.get(0);
         assertEquals(tableName, savedView0.getValue("tableName"));
         assertEquals("Custom Quick View", savedView0.getValue("label"));
         assertEquals("quickView", savedView0.getValue("type"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUserIdLock() throws Exception
   {
      String tableName        = TestUtils.TABLE_NAME_PERSON_MEMORY;
      String keyType          = "userId";
      String allAccessKeyType = "userIdAllAccess";

      ////////////////////////////////////////////////////////////////////////////////////////////
      // define a key type and a lock - then send that lock into the SavedViewsMetaDataProvider //
      ////////////////////////////////////////////////////////////////////////////////////////////
      QInstance qInstance = QContext.getQInstance();

      qInstance.addSecurityKeyType(new QSecurityKeyType()
         .withName(keyType)
         .withAllAccessKeyName(allAccessKeyType));

      RecordSecurityLock securityLock = new RecordSecurityLock()
         .withFieldName("userId")
         .withSecurityKeyType(keyType);

      new SavedViewsMetaDataProvider()
         .withIsShareSavedViewEnabled(true)
         .withIsQuickSavedViewEnabled(true)
         .withUserLevelRecordSecurityLock(securityLock)
         .defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      ///////////////////////////////////////////////////////
      // insert 3 views, one for each of 3 different users //
      ///////////////////////////////////////////////////////
      QContext.getQSession().withSecurityKeyValues(Map.of(allAccessKeyType, List.of(true)));
      List<QRecord> insertedSavedViews = new InsertAction().execute(new InsertInput(SavedView.TABLE_NAME).withRecordEntities(List.of(
         new SavedView().withLabel("Jim's").withUserId("jim").withTableName(tableName),
         new SavedView().withLabel("Jean's").withUserId("jean").withTableName(tableName),
         new SavedView().withLabel("Ben's").withUserId("ben").withTableName(tableName)
      ))).getRecords();
      Integer jeansViewId = insertedSavedViews.get(1).getValueInteger("id");

      {
         //////////////////////////////////////////////////
         // as user with allAccessKeyType, see all views //
         //////////////////////////////////////////////////
         QContext.getQSession().withSecurityKeyValues(Map.of(allAccessKeyType, List.of(true)));
         List<QRecord> records = runQuerySavedViewsProcess(tableName);
         assertEquals(3, records.size());
      }

      {
         /////////////////////////////////////////////////////
         // as an individual user, only see their own views //
         /////////////////////////////////////////////////////
         QContext.getQSession().withSecurityKeyValues(Map.of(keyType, List.of("jim")));
         List<QRecord> records = runQuerySavedViewsProcess(tableName);
         assertEquals(1, records.size());
         assertEquals("Jim's", records.get(0).getValue("label"));
      }

      {
         /////////////////////////////////////////////////////////////////////////////
         // now build a share of jean's view to ben                                 //
         // todo - a flaw we have is, jean can't insert a record to share to ben... //
         /////////////////////////////////////////////////////////////////////////////
         QContext.getQSession().withSecurityKeyValues(Map.of(allAccessKeyType, List.of(true)));
         new InsertAction().execute(new InsertInput(SharedSavedView.TABLE_NAME).withRecordEntity(
            new SharedSavedView().withSavedViewId(jeansViewId).withUserId("ben")));

         ///////////////////////////////////////////////////
         // then, as ben, run the process and get 2 views //
         ///////////////////////////////////////////////////
         QContext.getQSession().withSecurityKeyValues(Map.of(keyType, List.of("ben")));
         List<QRecord> records = runQuerySavedViewsProcess(tableName);
         assertEquals(2, records.size());
         assertEquals("Ben's", records.get(0).getValue("label"));
         assertEquals("Jean's", records.get(1).getValue("label"));
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static List<QRecord> runQuerySavedViewsProcess(String tableName) throws QException
   {
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(QuerySavedViewProcess.getProcessMetaData().getName());
      runProcessInput.addValue("tableName", tableName);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      List<QRecord>    savedViewList    = (List<QRecord>) runProcessOutput.getValue("savedViewList");
      return savedViewList;
   }



   /***************************************************************************
    * test overriding the {@link QuerySavedViewProcess} step, to do special logic
    * for quick views.
    ***************************************************************************/
   public static class CustomQuerySavedViewProcessStep extends QuerySavedViewProcess
   {
      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      protected void lookupQuickViews(RunBackendStepInput runBackendStepInput, List<QRecord> savedViewRecords) throws QException
      {
         savedViewRecords.add(new SavedView()
            .withTableName(runBackendStepInput.getValueString("tableName"))
            .withLabel("Custom Quick View")
            .toQRecord()
            .withValue("type", "quickView")
         );
      }
   }
}