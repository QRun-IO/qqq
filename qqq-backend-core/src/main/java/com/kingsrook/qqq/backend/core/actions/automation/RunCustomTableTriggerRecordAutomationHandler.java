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

package com.kingsrook.qqq.backend.core.actions.automation;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** RecordAutomationHandler implementation that is called by automation runner
 ** that doesn't know to deal with a TableTrigger record that it received.
 **
 ** e.g., if an app has altered that table (e.g., workflows-qbit).
 *******************************************************************************/
public class RunCustomTableTriggerRecordAutomationHandler implements RecordAutomationHandlerInterface
{
   private static final QLogger LOG = QLogger.getLogger(RunCustomTableTriggerRecordAutomationHandler.class);

   private static Map<String, QCodeReference> handlers = new LinkedHashMap<>();



   /***************************************************************************
    **
    ***************************************************************************/
   public static void registerHandler(String name, QCodeReference codeReference)
   {
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // if there's already a value mapped for this name, warn about it (unless it's for the same code reference) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      if(handlers.containsKey(name))
      {
         if(handlers.get(name).getName().equals(codeReference.getName()))
         {
            LOG.warn("Registering a CustomTableTriggerRecordAutomationHandler for a name that is already registered", logPair("name", name));
         }
      }

      handlers.put(name, codeReference);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void execute(RecordAutomationInput recordAutomationInput) throws QException
   {
      for(QCodeReference codeReference : handlers.values())
      {
         CustomTableTriggerRecordAutomationHandler customHandler = QCodeLoader.getAdHoc(CustomTableTriggerRecordAutomationHandler.class, codeReference);
         if(customHandler.handlesThisInput(recordAutomationInput))
         {
            customHandler.execute(recordAutomationInput);
            return;
         }
      }

      throw (new QException("No custom record automation handler was found for " + recordAutomationInput));
   }

}
