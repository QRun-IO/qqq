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

package com.kingsrook.qqq.backend.core.scheduler;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.VariantRunStrategy;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;


/*******************************************************************************
 ** Utility methods used by various schedulers.
 *******************************************************************************/
public class SchedulerUtils
{
   private static final QLogger LOG = QLogger.getLogger(SchedulerUtils.class);


   /*******************************************************************************
    **
    *******************************************************************************/
   public static boolean allowedToStart(String name)
   {
      String propertyName  = "qqq.scheduleManager.onlyStartNamesMatching";
      String propertyValue = System.getProperty(propertyName, "");
      if(propertyValue.equals(""))
      {
         return (true);
      }

      return (name.matches(propertyValue));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void runProcess(QInstance qInstance, Supplier<QSession> sessionSupplier, QProcessMetaData process, Map<String, Serializable> backendVariantData, Map<String, Serializable> processInputValues)
   {
      String originalThreadName = Thread.currentThread().getName();

      try
      {
         QContext.init(qInstance, sessionSupplier.get());

         if(process.getVariantBackend() == null || VariantRunStrategy.PARALLEL.equals(process.getVariantRunStrategy()))
         {
            executeSingleProcess(process, backendVariantData, processInputValues);
         }
         else if(VariantRunStrategy.SERIAL.equals(process.getVariantRunStrategy()))
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            // if this is "serial", which for example means we want to run each backend variant one after    //
            // the other in the same thread so loop over these here so that they run in same lambda function //
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            for(QRecord qRecord : getBackendVariantFilteredRecords(process))
            {
               try
               {
                  QBackendMetaData  backendMetaData  = qInstance.getBackend(process.getVariantBackend());
                  QTableMetaData variantTable = QContext.getQInstance().getTable(backendMetaData.getBackendVariantsConfig().getOptionsTableName());
                  Map<String, Serializable> thisVariantData = MapBuilder.of(backendMetaData.getBackendVariantsConfig().getVariantTypeKey(), qRecord.getValue(variantTable.getPrimaryKeyField()));
                  executeSingleProcess(process, thisVariantData, processInputValues);
               }
               catch(Exception e)
               {
                  LOG.error("An error starting process [" + process.getLabel() + "], with backend variant data.", e, new LogPair("variantQRecord", qRecord));
               }
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Exception thrown running scheduled process [" + process.getName() + "]", e);
      }
      finally
      {
         Thread.currentThread().setName(originalThreadName);
         QContext.clear();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void executeSingleProcess(QProcessMetaData process, Map<String, Serializable> backendVariantData, Map<String, Serializable> processInputValues) throws QException
   {
      if(backendVariantData != null)
      {
         QContext.getQSession().setBackendVariants(backendVariantData);
      }

      Thread.currentThread().setName("ScheduledProcess>" + process.getName());
      LOG.debug("Running Scheduled Process [" + process.getName() + "] with values [" + processInputValues + "]");

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(process.getName());

      Serializable recordId = null;
      for(Map.Entry<String, Serializable> entry : CollectionUtils.nonNullMap(processInputValues).entrySet())
      {
         runProcessInput.withValue(entry.getKey(), entry.getValue());
         if(entry.getKey().equals("recordId"))
         {
            recordId = entry.getValue();
         }
      }

      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if there was a "recordId" input value, and this table is for a process, then set up a callback to get the record //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(recordId != null && StringUtils.hasContent(process.getTableName()))
      {
         QTableMetaData table = QContext.getQInstance().getTable(process.getTableName());
         runProcessInput.setCallback(QProcessCallbackFactory.forPrimaryKey(table.getPrimaryKeyField(), recordId));
      }

      QContext.pushAction(runProcessInput);

      RunProcessAction runProcessAction = new RunProcessAction();
      runProcessAction.execute(runProcessInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<QRecord> getBackendVariantFilteredRecords(QProcessMetaData processMetaData)
   {
      List<QRecord> records = null;
      try
      {
         QBackendMetaData  backendMetaData  = QContext.getQInstance().getBackend(processMetaData.getVariantBackend());

         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(backendMetaData.getBackendVariantsConfig().getOptionsTableName());
         queryInput.setFilter(backendMetaData.getBackendVariantsConfig().getOptionsFilter());

         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         records = queryOutput.getRecords();
      }
      catch(Exception e)
      {
         LOG.error("An error fetching variant data for process [" + processMetaData.getLabel() + "]", e);
      }

      return (records);
   }

}
