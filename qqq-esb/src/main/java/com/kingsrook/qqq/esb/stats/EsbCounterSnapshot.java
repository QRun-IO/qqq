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

package com.kingsrook.qqq.esb.stats;


import java.time.Instant;


/*******************************************************************************
 * A point-in-time copy of one destination's or trigger's counters on this node
 * (spec section 7).  Component names match the endpoint contract's Counter.
 *
 * Destinations count published and publishFailures; triggers count the rest.
 * avgMs (rounded) and maxMs are over succeeded runs.  lastActivity is the time
 * of the latest count of any kind, and lastError the message of the latest
 * failure; both are null until there is one.
 *******************************************************************************/
public record EsbCounterSnapshot(
   Long published,
   Long publishFailures,
   Long consumed,
   Long succeeded,
   Long failed,
   Long retried,
   Long deadLettered,
   Integer inFlight,
   Instant lastActivity,
   Long avgMs,
   Long maxMs,
   String lastError)
{
   /*******************************************************************************
    ** The snapshot for a destination or trigger that hasn't counted anything.
    *******************************************************************************/
   public static final EsbCounterSnapshot EMPTY = new EsbCounterSnapshot(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0, null, 0L, 0L, null);
}
