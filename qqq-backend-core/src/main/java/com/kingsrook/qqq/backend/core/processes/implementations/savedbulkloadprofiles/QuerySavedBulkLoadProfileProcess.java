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
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.savedbulkloadprofiles.SavedBulkLoadProfile;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Process used by the saved bulkLoadProfile dialogs
 *******************************************************************************/
public class QuerySavedBulkLoadProfileProcess implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(QuerySavedBulkLoadProfileProcess.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData getProcessMetaData()
   {
      return (new QProcessMetaData()
         .withName("querySavedBulkLoadProfile")
         .withStepList(List.of(
            new QBackendStepMetaData()
               .withCode(new QCodeReference(QuerySavedBulkLoadProfileProcess.class))
               .withName("query")
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ActionHelper.validateSession(runBackendStepInput);
      Integer savedBulkLoadProfileId = runBackendStepInput.getValueInteger("id");

      try
      {
         if(savedBulkLoadProfileId != null)
         {
            GetInput input = new GetInput();
            input.setTableName(SavedBulkLoadProfile.TABLE_NAME);
            input.setPrimaryKey(savedBulkLoadProfileId);

            GetOutput output = new GetAction().execute(input);
            if(output.getRecord() == null)
            {
               throw (new QNotFoundException("The requested bulkLoadProfile was not found."));
            }

            runBackendStepOutput.addRecord(output.getRecord());
            runBackendStepOutput.addValue("savedBulkLoadProfile", output.getRecord());
            runBackendStepOutput.addValue("savedBulkLoadProfileList", (Serializable) List.of(output.getRecord()));
         }
         else
         {
            String  tableName  = runBackendStepInput.getValueString("tableName");
            boolean isBulkEdit = BooleanUtils.isTrue(runBackendStepInput.getValueBoolean("isBulkEdit"));

            QueryInput input = new QueryInput();
            input.setTableName(SavedBulkLoadProfile.TABLE_NAME);

            QQueryFilter filter = new QQueryFilter()
               .withCriteria(new QFilterCriteria("tableName", QCriteriaOperator.EQUALS, tableName))
               .withOrderBy(new QFilterOrderBy("label"));

            /////////////////////////////////////////////////////////////////////
            // account for nulls here, so if is bulk edit, only look for true, //
            // otherwise look for nulls or not equal to true                   //
            /////////////////////////////////////////////////////////////////////
            if(isBulkEdit)
            {
               filter.withCriteria(new QFilterCriteria("isBulkEdit", QCriteriaOperator.EQUALS, true));
            }
            else
            {
               filter.withCriteria(new QFilterCriteria("isBulkEdit", QCriteriaOperator.NOT_EQUALS_OR_IS_NULL, true));
            }
            input.setFilter(filter);

            QueryOutput output = new QueryAction().execute(input);
            runBackendStepOutput.setRecords(output.getRecords());
            runBackendStepOutput.addValue("savedBulkLoadProfileList", (Serializable) output.getRecords());
         }
      }
      catch(QNotFoundException qnfe)
      {
         LOG.info("BulkLoadProfile not found", logPair("savedBulkLoadProfileId", savedBulkLoadProfileId));
         throw (qnfe);
      }
      catch(Exception e)
      {
         LOG.warn("Error querying for saved bulkLoadProfiles", e);
         throw (e);
      }
   }
}
