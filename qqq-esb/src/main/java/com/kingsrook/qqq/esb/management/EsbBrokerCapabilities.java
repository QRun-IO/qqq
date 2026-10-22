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
