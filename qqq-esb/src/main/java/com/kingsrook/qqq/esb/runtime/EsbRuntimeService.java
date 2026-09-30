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

package com.kingsrook.qqq.esb.runtime;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;


/*******************************************************************************
 ** The runtime service (QInstance.withRuntimeService) through which an
 ** application launcher starts the node's ESB trigger runtime
 ** (QEsbRuntime.getInstance) with its server, and stops it at shutdown.
 ** EsbInstanceMetaData.enrich registers it.
 *******************************************************************************/
public class EsbRuntimeService implements QRuntimeServiceInterface
{
   public static final String NAME = "esbRuntime";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Start the node's runtime (doesn't block).
    *******************************************************************************/
   @Override
   public void start(QInstance qInstance) throws QException
   {
      QEsbRuntime.getInstance().start(qInstance);
   }



   /*******************************************************************************
    ** Stop consumers while application-owned producers can still be draining.
    ** Provider resources remain usable until final application cleanup.
    *******************************************************************************/
   @Override
   public void stop()
   {
      QEsbRuntime.getInstance().stop();
   }



   /*******************************************************************************
    ** Release provider resources after all application stop attempts, preserving
    ** publishing leases while scheduler/HTTP work drains after consumer stop.
    ******************************************************************************/
   @Override
   public void afterApplicationStop()
   {
      EsbConnectionManager.getInstance().closeAll();
   }

}
