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

package com.kingsrook.qqq.esb.management;


/*******************************************************************************
 * The broker queue management actions (spec section 8) that a provider's
 * broker supports - so the UI shows, and the operate processes allow, only
 * those.
 *
 * - browse: list a queue's messages (plain JMS, so every broker).
 * - queueInfo: report a queue's depth and consumer count.
 * - pauseQueue: pause (and resume) delivery to all of a queue's consumers.
 * - purge: remove all of a queue's messages.
 * - deleteSelected: remove messages by JMS message id.
 * - deleteOlderThan: remove messages older than a time.
 * - move: move messages by JMS message id to another queue.
 *
 * Each EsbBrokerAdapter reports its broker's; WITHOUT_MANAGEMENT is for a
 * provider with no management API (no managementUrl), which can only browse.
 *******************************************************************************/
public record EsbBrokerCapabilities(boolean browse, boolean queueInfo, boolean pauseQueue, boolean purge, boolean deleteSelected, boolean deleteOlderThan, boolean move)
{
   public static final EsbBrokerCapabilities WITHOUT_MANAGEMENT = new EsbBrokerCapabilities(true, false, false, false, false, false, false);
}
