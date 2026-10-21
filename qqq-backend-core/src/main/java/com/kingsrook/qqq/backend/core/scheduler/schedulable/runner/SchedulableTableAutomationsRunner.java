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

package com.kingsrook.qqq.backend.core.scheduler.schedulable.runner;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.polling.PollingAutomationPerTableRunner;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.automation.QAutomationProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.SchedulableIdentity;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Schedulable TableAutomations runner - e.g., how a table automations are run
 ** by a scheduler.
 *******************************************************************************/
public class SchedulableTableAutomationsRunner implements SchedulableRunner
{
   private static final QLogger LOG = QLogger.getLogger(SchedulableTableAutomationsRunner.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(Map<String, Object> params)
   {
      QInstance qInstance = QuartzScheduler.getInstance().getQInstance();

      String tableName = ValueUtils.getValueAsString(params.get("tableName"));
      if(!StringUtils.hasContent(tableName))
      {
         LOG.warn("Missing tableName in params.");
         return;
      }

      QTableMetaData table = qInstance.getTable(tableName);
      if(table == null)
      {
         LOG.warn("Unrecognized tableName [" + tableName + "]");
         return;
      }

      AutomationStatus automationStatus = AutomationStatus.valueOf(ValueUtils.getValueAsString(params.get("automationStatus")));

      QTableAutomationDetails automationDetails = table.getAutomationDetails();
      if(automationDetails == null)
      {
         LOG.warn("Could not find automationDetails for table for automations in QInstance", logPair("tableName", tableName));
         return;
      }

      ///////////////////////////////////
      // todo - sharded automations... //
      ///////////////////////////////////
      PollingAutomationPerTableRunner.TableActionsInterface tableAction = new PollingAutomationPerTableRunner.TableActions(tableName, automationDetails, automationStatus);
      PollingAutomationPerTableRunner                       runner      = new PollingAutomationPerTableRunner(qInstance, automationDetails.getProviderName(), QuartzScheduler.getInstance().getSessionSupplier(), tableAction);

      /////////////
      // run it. //
      /////////////
      LOG.debug("Running Table Automations", logPair("tableName", tableName), logPair("automationStatus", automationStatus));
      runner.run();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validateParams(SchedulableIdentity schedulableIdentity, Map<String, Object> paramMap) throws QException
   {
      String tableName = ValueUtils.getValueAsString(paramMap.get("tableName"));
      if(!StringUtils.hasContent(tableName))
      {
         throw (new QException("Missing scheduledJobParameter with key [tableName] in " + schedulableIdentity));
      }

      String automationStatus = ValueUtils.getValueAsString(paramMap.get("automationStatus"));
      if(!StringUtils.hasContent(automationStatus))
      {
         throw (new QException("Missing scheduledJobParameter with key [automationStatus] in " + schedulableIdentity));
      }

      QTableMetaData table = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         throw (new QException("Unrecognized tableName [" + tableName + "] in " + schedulableIdentity));
      }

      QTableAutomationDetails automationDetails = table.getAutomationDetails();
      if(automationDetails == null)
      {
         throw (new QException("Table [" + tableName + "] does not have automationDetails in " + schedulableIdentity));
      }

      if(automationDetails.getSchedule() != null)
      {
         throw (new QException("Table [" + tableName + "] automationDetails has a schedule in its metaData - so it should not be dynamically scheduled via a scheduled job! " + schedulableIdentity));
      }

      QAutomationProviderMetaData automationProvider = QContext.getQInstance().getAutomationProvider(automationDetails.getProviderName());

      List<PollingAutomationPerTableRunner.TableActionsInterface> tableActionList = PollingAutomationPerTableRunner.getTableActions(QContext.getQInstance(), automationProvider.getName());
      for(PollingAutomationPerTableRunner.TableActionsInterface tableActions : tableActionList)
      {
         if(tableActions.status().name().equals(automationStatus))
         {
            return;
         }
      }

      /////////////////////////////////////////////////////////////////////////////////////
      // if we get out of the loop, it means we didn't find a matching status - so throw //
      /////////////////////////////////////////////////////////////////////////////////////
      throw (new QException("Did not find table automation actions matching automationStatus [" + automationStatus + "] for table [" + tableName + "] in " + schedulableIdentity
         + " (Found: " + tableActionList.stream().map(ta -> ta.status().name()).collect(Collectors.joining(",")) + ")"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getDescription(Map<String, Object> params)
   {
      return "TableAutomations: " + params.get("tableName") + "." + params.get("automationStatus");
   }

}
