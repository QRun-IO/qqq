/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.savedbulkloadprofiles;


import java.io.Serializable;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
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
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfile;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 ** Process used by the saved bulkLoadProfile dialog
 *******************************************************************************/
public class StoreSavedBulkLoadProfileProcess implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(StoreSavedBulkLoadProfileProcess.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData getProcessMetaData()
   {
      return (new QProcessMetaData()
         .withName("storeSavedBulkLoadProfile")
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withCode(new QCodeReference(StoreSavedBulkLoadProfileProcess.class))
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
         String  userId     = QContext.getQSession().getUser().getIdReference();
         String  tableName  = runBackendStepInput.getValueString("tableName");
         String  label      = runBackendStepInput.getValueString("label");
         Boolean isBulkEdit = BooleanUtils.isTrue(runBackendStepInput.getValueBoolean("isBulkEdit"));

         String mappingJson = processMappingJson(runBackendStepInput.getValueString("mappingJson"));

         QRecord qRecord = new QRecord()
            .withValue("id", runBackendStepInput.getValueInteger("id"))
            .withValue("mappingJson", mappingJson)
            .withValue("label", label)
            .withValue("tableName", tableName)
            .withValue("isBulkEdit", isBulkEdit)
            .withValue("userId", userId);

         List<QRecord> savedBulkLoadProfileList;
         if(qRecord.getValueInteger("id") == null)
         {
            checkForDuplicates(userId, tableName, label, null);

            InsertInput input = new InsertInput();
            input.setTableName(SavedBulkLoadProfile.TABLE_NAME);
            input.setRecords(List.of(qRecord));

            InsertOutput output = new InsertAction().execute(input);
            savedBulkLoadProfileList = output.getRecords();
         }
         else
         {
            checkForDuplicates(userId, tableName, label, qRecord.getValueInteger("id"));

            UpdateInput input = new UpdateInput();
            input.setTableName(SavedBulkLoadProfile.TABLE_NAME);
            input.setRecords(List.of(qRecord));

            UpdateOutput output = new UpdateAction().execute(input);
            savedBulkLoadProfileList = output.getRecords();
         }

         runBackendStepOutput.addValue("savedBulkLoadProfileList", (Serializable) savedBulkLoadProfileList);
      }
      catch(Exception e)
      {
         LOG.warn("Error storing saved bulkLoadProfile", e);
         throw (e);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String processMappingJson(String mappingJson)
   {
      return mappingJson;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void checkForDuplicates(String userId, String tableName, String label, Integer id) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(SavedBulkLoadProfile.TABLE_NAME);
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
         throw (new QUserFacingException("You already have a saved Bulk Load Profile on this table with this name."));
      }
   }
}
