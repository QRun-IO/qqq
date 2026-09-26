/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
