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

package com.kingsrook.qqq.backend.core.alerts;


import java.time.Instant;


/*******************************************************************************
 ** Where QAlerts sends signals.  The default is LoggingAlertSink; an alerts
 ** implementation (such as qbit-alerts) registers its own through
 ** AlertsInstanceMetaData.withSink.
 **
 ** Methods are called after the caller's transaction commits (or at once, when
 ** there is none), on the caller's thread, so they must be fast: queue the
 ** work, don't do database writes or network calls inline.  Anything they
 ** throw is caught, logged and counted by QAlerts; it never reaches the
 ** producer.
 **
 ** A new instance is not loaded per call: QAlerts loads the sink once per
 ** QInstance and reuses it, so implementations must be thread-safe.
 *******************************************************************************/
public interface AlertSinkInterface
{
   /***************************************************************************
    ** Record (or refresh) an alert.  The signal's occurredAt is always set.
    ***************************************************************************/
   void raise(AlertSignal signal);

   /***************************************************************************
    ** Resolve an open alert, if there is one.  resolvedAt is always set.
    ***************************************************************************/
   void resolve(AlertResolution resolution);

   /***************************************************************************
    ** Note that a dead-man's switch beat.
    ***************************************************************************/
   void heartbeat(String heartbeatKey, Instant at);

}
