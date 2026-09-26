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
