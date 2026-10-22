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


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedView;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Process used by the saved view dialog
 *******************************************************************************/
public class StoreSavedViewProcess implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(StoreSavedViewProcess.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData getProcessMetaData()
   {
      return (new QProcessMetaData()
         .withName("storeSavedView")
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withCode(new QCodeReference(StoreSavedViewProcess.class))
               .withName("store")
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ActionHelper.validateSession(runBackendStepInput);

      try
      {
         String userId    = QContext.getQSession().getUser().getIdReference();
         String tableName = runBackendStepInput.getValueString("tableName");
         String label     = runBackendStepInput.getValueString("label");

         QRecord qRecord = new QRecord()
            .withValue("id", runBackendStepInput.getValueInteger("id"))
            .withValue("viewJson", runBackendStepInput.getValueString("viewJson"))
            .withValue("label", label)
            .withValue("tableName", tableName)
            .withValue("userId", userId);

         List<QRecord> savedViewList;
         if(qRecord.getValueInteger("id") == null)
         {
            InsertInput input = new InsertInput();
            input.setTableName(SavedView.TABLE_NAME);
            input.setInputSource(QInputSource.USER);
            input.setRecords(List.of(qRecord));

            PermissionsHelper.checkTablePermissionThrowing(input, TablePermissionSubType.INSERT);
            checkForDuplicates(userId, tableName, label, null);
            InsertOutput output = new InsertAction().execute(input);
            savedViewList = output.getRecords();
         }
         else
         {
            UpdateInput input = new UpdateInput();
            input.setTableName(SavedView.TABLE_NAME);
            input.setInputSource(QInputSource.USER);
            input.setRecords(List.of(qRecord));

            PermissionsHelper.checkTablePermissionThrowing(input, TablePermissionSubType.EDIT);
            checkForDuplicates(userId, tableName, label, qRecord.getValueInteger("id"));
            UpdateOutput output = new UpdateAction().execute(input);
            savedViewList = output.getRecords();
         }

         if(CollectionUtils.nullSafeIsEmpty(savedViewList))
         {
            throw new QUserFacingException("No saved view was stored.");
         }
         for(QRecord record : savedViewList)
         {
            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               throw new QUserFacingException("Error storing saved view: " + record.getErrorsAsString());
            }
         }
         runBackendStepOutput.addValue("savedViewList", (Serializable) savedViewList);
      }
      catch(Exception e)
      {
         LOG.warn("Error storing saved view", e);
         throw (e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void checkForDuplicates(String userId, String tableName, String label, Integer id) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(SavedView.TABLE_NAME);
      queryInput.setFilter(new QQueryFilter(
         new QFilterCriteria("userId", QCriteriaOperator.EQUALS, userId),
         new QFilterCriteria("tableName", QCriteriaOperator.EQUALS, tableName),
         new QFilterCriteria("label", QCriteriaOperator.EQUALS, label)));

      if(id != null)
      {
         queryInput.getFilter().addCriteria(new QFilterCriteria("id", QCriteriaOperator.NOT_EQUALS, id));
      }

      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      if(CollectionUtils.nullSafeHasContents(queryOutput.getRecords()))
      {
         throw (new QUserFacingException("You already have a saved view on this table with this name."));
      }
   }
}
