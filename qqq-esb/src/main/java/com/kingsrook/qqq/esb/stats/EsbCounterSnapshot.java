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
