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


package com.kingsrook.qqq.backend.module.postgres.jobs;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** A stand-alone "pod" for the kill-the-pod tests: a separate JVM that runs the
 ** durable jobs runtime against the test database until it is killed.
 **
 ** args: host port database user password podId [storageBasePath]
 *******************************************************************************/
public class DurableJobsTestNode
{

   /*******************************************************************************
    **
    *******************************************************************************/
   public static void main(String[] args) throws Exception
   {
      String podId = args[5];
      System.setProperty(KillTestInstance.POD_PROPERTY, podId);

      QInstance instance = KillTestInstance.define(args[0], Integer.parseInt(args[1]), args[2], args[3], args[4], podId, 1, args.length > 6 ? args[6] : null);
      QContext.init(instance, new QSystemUserSession());
      DurableJobsRuntime.start(instance);
      System.out.println("READY " + podId);
      System.out.flush();

      ////////////////////////////////////////////////////////////////////
      // run until killed (SIGKILL) or terminated (SIGTERM -> the drain //
      // shutdown hook), or until the parent closes our stdin.          //
      ////////////////////////////////////////////////////////////////////
      while(System.in.read() != -1)
      {
         // keep running
      }
      System.exit(0);
   }

}
