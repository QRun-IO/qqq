/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.scripts;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.scripts.RunAdHocRecordScriptAction;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.StoreScriptLogAndScriptLogLineExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryFilterLink;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.AdHocScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptLog;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.commons.lang3.BooleanUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IN;


/*******************************************************************************
 ** Load step for the runRecordScript process - runs the script on a page of records
 *******************************************************************************/
public class RunRecordScriptLoadStep extends AbstractLoadStep implements ProcessSummaryProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(RunRecordScriptLoadStep.class);

   private ProcessSummaryLine okLine = new ProcessSummaryLine(Status.OK)
      .withSingularPastMessage("had the script ran against it.")
      .withPluralPastMessage("had the script ran against them.");

   private ProcessSummaryLine unloggedExceptionLine = new ProcessSummaryLine(Status.ERROR, null, "had an error that was not logged.");

   private List<Serializable> okScriptLogIds    = new ArrayList<>();
   private List<Serializable> errorScriptLogIds = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getOverrideRecordPipeCapacity(RunBackendStepInput runBackendStepInput)
   {
      Integer scriptId = runBackendStepInput.getValueInteger("scriptId");
      try
      {
         GetInput getInput = new GetInput();
         getInput.setTableName(Script.TABLE_NAME);
         getInput.setPrimaryKey(scriptId);
         GetOutput getOutput = new GetAction().execute(getInput);
         if(getOutput.getRecord() != null)
         {
            Integer scriptMaxBatchSize = getOutput.getRecord().getValueInteger("maxBatchSize");
            if(scriptMaxBatchSize != null)
            {
               return (scriptMaxBatchSize);
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error getting script by id: " + scriptId);
      }

      return super.getOverrideRecordPipeCapacity(runBackendStepInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      runBackendStepInput.getAsyncJobCallback().updateStatus("Running script");

      okLine.incrementCount(runBackendStepInput.getRecords().size());

      Integer                                       scriptId     = runBackendStepInput.getValueInteger("scriptId");
      StoreScriptLogAndScriptLogLineExecutionLogger scriptLogger = new StoreScriptLogAndScriptLogLineExecutionLogger(null, null); // downstream these will get set!

      GetInput getInput = new GetInput();
      getInput.setTableName(Script.TABLE_NAME);
      getInput.setPrimaryKey(scriptId);
      GetOutput getOutput = new GetAction().execute(getInput);
      QRecord   script    = getOutput.getRecord();
      if(script == null)
      {
         throw (new QException("Could not find script by id: " + scriptId));
      }

      String tableName = script.getValueString("tableName");

      RunAdHocRecordScriptInput input = new RunAdHocRecordScriptInput();
      input.setRecordList(runBackendStepInput.getRecords());
      input.setCodeReference(new AdHocScriptCodeReference().withScriptId(scriptId));
      input.setTableName(tableName);
      input.setLogger(scriptLogger);

      RunAdHocRecordScriptOutput output          = new RunAdHocRecordScriptOutput();
      Exception                  caughtException = null;
      try
      {
         new RunAdHocRecordScriptAction().run(input, output);
         if(output.getException().isPresent())
         {
            caughtException = output.getException().get();
         }
      }
      catch(Exception e)
      {
         LOG.info("Exception running record script", e, logPair("scriptId", scriptId));
         caughtException = e;
      }

      String auditMessage = "Script \"" + script.getValueString("name") + "\" (id: " + scriptId + ") was executed against this record";

      //////////////////////////////////////////////////////////
      // add the record to the appropriate processSummaryLine //
      //////////////////////////////////////////////////////////
      if(scriptLogger.getScriptLog() != null)
      {
         Integer id = scriptLogger.getScriptLog().getValueInteger("id");
         if(id != null)
         {
            auditMessage += ", creating script log: " + id;
            boolean hadError = BooleanUtils.isTrue(scriptLogger.getScriptLog().getValueBoolean("hadError"));
            (hadError ? errorScriptLogIds : okScriptLogIds).add(id);
         }
      }
      else if(caughtException != null)
      {
         unloggedExceptionLine.incrementCount(runBackendStepInput.getRecords().size());
      }

      ////////////////////////////////////////////////////////////
      // audit that the script was executed against the records //
      ////////////////////////////////////////////////////////////
      audit(runBackendStepInput, tableName, auditMessage);
   }



   /*******************************************************************************
    ** for each input record, add an audit stating that the script was executed.
    *******************************************************************************/
   private static void audit(RunBackendStepInput runBackendStepInput, String tableName, String auditMessage)
   {
      try
      {
         QTableMetaData table      = QContext.getQInstance().getTable(tableName);
         AuditInput     auditInput = new AuditInput();
         for(QRecord record : runBackendStepInput.getRecords())
         {
            AuditAction.appendToInput(auditInput, table, record, auditMessage);
         }
         new AuditAction().execute(auditInput);
      }
      catch(Exception e)
      {
         LOG.warn("Error recording audits after running record script", e, logPair("tableName", tableName), logPair("auditMessage", auditMessage));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> summary = new ArrayList<>();
      summary.add(okLine);

      if(CollectionUtils.nullSafeHasContents(okScriptLogIds))
      {
         summary.add(new ProcessSummaryFilterLink(Status.OK, ScriptLog.TABLE_NAME, new QQueryFilter(new QFilterCriteria("id", IN, okScriptLogIds)))
            .withLinkText("Created " + String.format("%,d", okScriptLogIds.size()) + " Successful Script Log" + StringUtils.plural(okScriptLogIds)));
      }

      if(CollectionUtils.nullSafeHasContents(errorScriptLogIds))
      {
         summary.add(new ProcessSummaryFilterLink(Status.ERROR, ScriptLog.TABLE_NAME, new QQueryFilter(new QFilterCriteria("id", IN, errorScriptLogIds)))
            .withLinkText("Created " + String.format("%,d", errorScriptLogIds.size()) + " Script Log" + StringUtils.plural(errorScriptLogIds) + " with Errors"));
      }

      unloggedExceptionLine.addSelfToListIfAnyCount(summary);

      return (summary);
   }
}
