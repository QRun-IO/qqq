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

package com.kingsrook.qqq.backend.core.processes.implementations.savedreports;


import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReport;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.json.JSONObject;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class RunScheduledReportExecuteStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(RunScheduledReportExecuteStep.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      Integer scheduledReportId = null;
      try
      {
         List<QRecord> records = runBackendStepInput.getRecords();
         if(!CollectionUtils.nullSafeHasContents(records))
         {
            throw (new QUserFacingException("No scheduled report was selected or found."));
         }

         ScheduledReport scheduledReport = new ScheduledReport(records.get(0));
         scheduledReportId = scheduledReport.getId();
         if(!StringUtils.hasContent(scheduledReport.getUserId()))
         {
            throw new QPermissionDeniedException("Scheduled report must have an owner.");
         }

         ////////////////////////////////////////////////////////////////////////////////////
         // get the schedule's user - as that will drive the security key we need to apply //
         ////////////////////////////////////////////////////////////////////////////////////
         updateSessionForUser(scheduledReport.getUserId());

         /////////////////////////////////////////////
         // run the process that renders the report //
         /////////////////////////////////////////////
         RunProcessAction runProcessAction   = new RunProcessAction();
         RunProcessInput  renderProcessInput = new RunProcessInput();
         renderProcessInput.setProcessName(RenderSavedReportMetaDataProducer.NAME);
         renderProcessInput.setCallback(QProcessCallbackFactory.forPrimaryKey("id", scheduledReport.getSavedReportId()));
         renderProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         renderProcessInput.setAsyncJobCallback(runBackendStepInput.getAsyncJobCallback());

         renderProcessInput.addValue(RenderSavedReportMetaDataProducer.FIELD_NAME_REPORT_FORMAT, scheduledReport.getFormat());
         renderProcessInput.addValue(RenderSavedReportMetaDataProducer.FIELD_NAME_EMAIL_ADDRESS, scheduledReport.getToAddresses());
         renderProcessInput.addValue(RenderSavedReportMetaDataProducer.FIELD_NAME_EMAIL_SUBJECT, scheduledReport.getSubject());

         if(StringUtils.hasContent(scheduledReport.getInputValues()))
         {
            /////////////////////////////////////////////////////////////////////////////////////
            // if there are input values, pass them along on report input...                   //
            // this could maybe be better (e.g., some object?), but, this is working initially //
            /////////////////////////////////////////////////////////////////////////////////////
            JSONObject jsonObject = JsonUtils.toJSONObject(scheduledReport.getInputValues());
            for(String name : jsonObject.keySet())
            {
               renderProcessInput.addValue(name, jsonObject.optString(name));
            }
         }

         runProcessAction.execute(renderProcessInput);
      }
      catch(QUserFacingException ufe)
      {
         LOG.info("Error running scheduled report", ufe, logPair("id", scheduledReportId));
         throw (ufe);
      }
      catch(Exception e)
      {
         LOG.warn("Error running scheduled report", e, logPair("id", scheduledReportId));
         throw (new QException("Error running scheduled report", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void updateSessionForUser(String userId) throws QException
   {
      try
      {
         QInstance                       qInstance                       = QContext.getQInstance();
         QAuthenticationModuleDispatcher qAuthenticationModuleDispatcher = new QAuthenticationModuleDispatcher();
         QAuthenticationModuleInterface  authenticationModule            = qAuthenticationModuleDispatcher.getQModule(qInstance.getAuthentication());

         ///////////////////////////////////////
         // create automated-session for user //
         ///////////////////////////////////////
         QSession session = authenticationModule.createAutomatedSessionForUser(qInstance, userId);
         if(session == null || session instanceof QSystemUserSession || session.getUser() == null
            || !Objects.equals(userId, session.getUser().getIdReference()) || !authenticationModule.isSessionValid(qInstance, session))
         {
            throw new QPermissionDeniedException("Could not establish the scheduled report owner's session.");
         }

         /////////////////////////////////////////////
         // set that session in the current context //
         /////////////////////////////////////////////
         QContext.setQSession(session);
      }
      catch(Exception e)
      {
         LOG.warn("Error setting up user session for running scheduled report", e, logPair("userId", userId));
         throw (new QException("Error setting up user session for running scheduled report", e));
      }
   }

}
