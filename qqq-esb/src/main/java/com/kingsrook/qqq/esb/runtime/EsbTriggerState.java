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


/*******************************************************************************
 * The state of one trigger's consumers on this node (EsbTriggerRunner.getState).
 *
 * - RUNNING: every consumer is connected and receiving.
 * - PAUSED: paused (by startPaused or pauseLocal); consumers are closed.
 * - CONNECTING: started and not paused, but at least one consumer has no
 *   connection yet (the broker is down, or being reconnected to).
 * - STOPPED: the runtime is not running.
 *******************************************************************************/
public enum EsbTriggerState
{
   RUNNING,
   PAUSED,
   CONNECTING,
   STOPPED
}
