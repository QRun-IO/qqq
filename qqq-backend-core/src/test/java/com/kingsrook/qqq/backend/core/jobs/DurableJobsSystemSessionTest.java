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

package com.kingsrook.qqq.backend.core.jobs;


import java.time.Duration;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The runtime background threads have no QContext of their own, so the system
 ** sessions they run as must be built from the runtime instance (else every
 ** tick warns, and the session has no all-access keys).
 *******************************************************************************/
class DurableJobsSystemSessionTest extends DurableJobsTestBase
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void backgroundThreadsBuildSystemSessionsWithTheInstance() throws Exception
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(QSystemUserSession.class);
      try
      {
         DurableJobsRuntime.start(instance);
         Thread.sleep(2_500);
         DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));

         assertThat(collectingLogger.getCollectedMessages())
            .noneMatch(m -> m.getMessage().contains("QInstance was not supplied"));
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(QSystemUserSession.class);
      }
   }
}
