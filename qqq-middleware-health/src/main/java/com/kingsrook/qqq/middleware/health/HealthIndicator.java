/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.health;


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;


/*******************************************************************************
 ** Interface for components that can perform health checks.
 **
 ** Implementations should check a specific aspect of the system (database,
 ** memory, disk, external service, etc.) and return a result indicating
 ** whether that component is healthy.
 *******************************************************************************/
public interface HealthIndicator
{

   /*******************************************************************************
    ** Get the unique name for this health indicator.
    ** This name will be used as the key in the health response JSON.
    **
    ** @return indicator name (e.g., "database", "memory", "diskSpace")
    *******************************************************************************/
   String getName();


   /*******************************************************************************
    ** Execute the health check.
    **
    ** Implementations should:
    ** 1. Perform the check as quickly as possible (respecting timeouts)
    ** 2. Return UP, DOWN, DEGRADED, or UNKNOWN status
    ** 3. Include relevant details in the result
    ** 4. Handle exceptions gracefully (return UNKNOWN status)
    **
    ** @param qInstance the QInstance for accessing backends, configuration, etc.
    ** @return health check result with status and details
    ** @throws QException if the check fails catastrophically
    *******************************************************************************/
   HealthCheckResult check(QInstance qInstance) throws QException;
}
