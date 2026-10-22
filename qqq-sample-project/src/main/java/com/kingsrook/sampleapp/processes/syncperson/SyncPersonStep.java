/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.sampleapp.processes.syncperson;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Sample subscriber. Replace the log with a real external person sync.
 *******************************************************************************/
public class SyncPersonStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(SyncPersonStep.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      @SuppressWarnings("unchecked")
      List<EsbEvent> messages = (List<EsbEvent>) input.getValue("esbMessages");
      for(EsbEvent event : messages)
      {
         LOG.info("Person event received", logPair("eventId", event.getId()), logPair("personId", event.getSubject()));
      }
   }
}
