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


import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerUtils;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.SchedulableIdentity;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Schedulable process runner - e.g., how a QProcess is run by a scheduler.
 *******************************************************************************/
public class SchedulableProcessRunner implements SchedulableRunner
{
   private static final QLogger LOG = QLogger.getLogger(SchedulableProcessRunner.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(Map<String, Object> params)
   {
      String processName = ValueUtils.getValueAsString(params.get("processName"));

      ///////////////////////////////////////
      // get the process from the instance //
      ///////////////////////////////////////
      QInstance        qInstance = QContext.getQInstance();
      QProcessMetaData process   = qInstance.getProcess(processName);
      if(process == null)
      {
         LOG.warn("Could not find scheduled process in QInstance", logPair("processName", processName));
         return;
      }

      ///////////////////////////////////////////////
      // if the job has variant data, get it ready //
      ///////////////////////////////////////////////
      Map<String, Serializable> backendVariantData = null;
      if(params.containsKey("backendVariantData"))
      {
         @SuppressWarnings("unchecked")
         Map<String, Serializable> dataFromMap = (Map<String, Serializable>) params.get("backendVariantData");
         backendVariantData = dataFromMap;
      }

      Map<String, Serializable> processInputValues = buildProcessInputValuesMap(params, process);

      /////////////
      // run it. //
      /////////////
      LOG.debug("Running scheduled process", logPair("processName", processName));
      SchedulerUtils.runProcess(qInstance, () -> QContext.getQSession(), qInstance.getProcess(processName), backendVariantData, processInputValues);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validateParams(SchedulableIdentity schedulableIdentity, Map<String, Object> paramMap) throws QException
   {
      String processName = ValueUtils.getValueAsString(paramMap.get("processName"));
      if(!StringUtils.hasContent(processName))
      {
         throw (new QException("Missing scheduledJobParameter with key [processName] in " + schedulableIdentity));
      }

      QProcessMetaData process = QContext.getQInstance().getProcess(processName);
      if(process == null)
      {
         throw (new QException("Unrecognized processName [" + processName + "] in " + schedulableIdentity));
      }

      if(process.getSchedule() != null)
      {
         throw (new QException("Process [" + processName + "] has a schedule in its metaData - so it should not be dynamically scheduled via a scheduled job! " + schedulableIdentity));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getDescription(Map<String, Object> params)
   {
      return "Process: " + params.get("processName");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Serializable> buildProcessInputValuesMap(Map<String, Object> params, QProcessMetaData process)
   {
      Map<String, Serializable> processInputValues = new HashMap<>();

      //////////////////////////////////////////////////////////////////////////////////////
      // track which keys need processed - start by removing ones we know we handle above //
      //////////////////////////////////////////////////////////////////////////////////////
      Set<String> keys = new HashSet<>(params.keySet());
      keys.remove("processName");
      keys.remove("backendVariantData");

      if(!keys.isEmpty())
      {
         //////////////////////////////////////////////////////////////////////////
         // first make a pass going over the process's identified input fields - //
         // getting values from the quartz job data map, and putting them into   //
         // the process input value map as the field's type (if we can)          //
         //////////////////////////////////////////////////////////////////////////
         for(QFieldMetaData inputField : process.getInputFields())
         {
            String fieldName = inputField.getName();
            if(params.containsKey(fieldName))
            {
               Object value = params.get(fieldName);
               try
               {
                  processInputValues.put(fieldName, ValueUtils.getValueAsFieldType(inputField.getType(), value));
                  keys.remove(fieldName);
               }
               catch(Exception e)
               {
                  LOG.warn("Error getting process input value from quartz job data map", e, logPair("fieldName", fieldName), logPair("value", value));
               }
            }
         }

         ////////////////////////////////////////////////////////////////////////////////////////
         // if any values are left in the map (based on keys set that we're removing from)     //
         // then try to put those in the input map (assuming they can be cast to Serializable) //
         ////////////////////////////////////////////////////////////////////////////////////////
         for(String key : keys)
         {
            Object value = params.get(key);
            try
            {
               processInputValues.put(key, (Serializable) value);
            }
            catch(Exception e)
            {
               LOG.warn("Error getting process input value from quartz job data map", e, logPair("key", key), logPair("value", value));
            }
         }
      }

      return processInputValues;
   }

}
