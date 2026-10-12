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


/*******************************************************************************
 ** The kinds of monitor an alerts implementation evaluates.  Declared in core
 ** so producers can describe a seeded (default) monitor for their alert types
 ** without depending on the implementation.
 *******************************************************************************/
public enum AlertMonitorKind
{
   /** count of rows matching a filter passes a threshold. */
   TABLE_QUERY,

   /** an aggregate (count, sum, avg, min, max) crosses a band. */
   AGGREGATE,

   /** nothing was seen (a heartbeat, a process success, a newest row) within an interval. */
   HEARTBEAT,

   /** a process run failed. */
   PROCESS_FAILED,

   /** a matching ESB event arrived. */
   ESB_EVENT,

   /** a code check returned the currently-failing conditions. */
   CODE,

   /** alerts ingested from an external system (e.g. Alertmanager). */
   EXTERNAL
}
