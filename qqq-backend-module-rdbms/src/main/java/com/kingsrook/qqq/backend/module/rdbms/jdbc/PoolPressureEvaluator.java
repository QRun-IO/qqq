/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.time.Duration;
import java.util.Map;
import com.kingsrook.qqq.backend.core.health.HealthStatus;


/*******************************************************************************
 ** Per-provider pressure history. Caller supplies monotonic time for deterministic
 ** tests; connection failure is handled separately and never delayed here.
 *******************************************************************************/
public final class PoolPressureEvaluator
{
   public static final Duration WAIT_GRACE = Duration.ofSeconds(30);
   private Long waitingSince;
   private long previousTimeouts;



   /*******************************************************************************
    ** Transient growth is normal. Persistent waiters, new timeouts and live leaks
    ** are not; an old cumulative timeout counter alone must not latch a warning.
    *******************************************************************************/
   public synchronized HealthStatus evaluate(Map<String, Number> metrics, long nowNanos)
   {
      if(!metrics.containsKey("threadsAwaitingCheckout") || !metrics.containsKey("checkoutTimeouts") || !metrics.containsKey("suspectedLeaks"))
      {
         waitingSince = null;
         return (HealthStatus.UNKNOWN);
      }
      long timeouts = metrics.get("checkoutTimeouts").longValue();
      boolean newTimeout = timeouts > previousTimeouts;
      previousTimeouts = timeouts;
      long waiting = metrics.get("threadsAwaitingCheckout").longValue();
      if(waiting <= 0)
      {
         waitingSince = null;
      }
      else if(waitingSince == null || nowNanos < waitingSince)
      {
         waitingSince = nowNanos;
      }
      boolean sustained = waitingSince != null && nowNanos - waitingSince >= WAIT_GRACE.toNanos();
      return (newTimeout || metrics.get("suspectedLeaks").longValue() > 0 || sustained ? HealthStatus.DEGRADED : HealthStatus.UP);
   }
}
