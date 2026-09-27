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

package com.kingsrook.qqq.backend.core.instances;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;


/*******************************************************************************
 ** A long-running service that an application starts along with its server,
 ** and stops at shutdown (e.g., a message consumer).  Registered via
 ** QInstance.withRuntimeService; an application launcher loads each one (via
 ** QCodeLoader, so it needs a no-arg constructor), starts them in registration
 ** order, and stops them in reverse order.
 **
 ** If start throws, the launcher stops the services it had already started
 ** (not the one that threw), and fails.  stop is called at most once per
 ** started service, and should not throw.
 *******************************************************************************/
public interface QRuntimeServiceInterface
{
   /***************************************************************************
    ** Name of the service, for logs and errors.
    ***************************************************************************/
   String getName();

   /***************************************************************************
    ** Start the service, for the application's (validated) instance.
    ***************************************************************************/
   void start(QInstance qInstance) throws QException;

   /***************************************************************************
    ** Stop the service, releasing what start acquired.
    ***************************************************************************/
   void stop();

}
