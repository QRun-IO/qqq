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
 * A broker queue's state, from the broker's management API, at the moment it
 * was asked.
 *
 * - messageCount: messages on the queue, including any delivered to a
 *   consumer but not yet acknowledged.
 * - consumerCount: consumers attached to the queue, on every node (QQQ's and
 *   any others).
 * - paused: whether delivery to the queue's consumers is paused on the broker
 *   (always false on a broker that can't pause a queue, e.g., RabbitMQ).
 *******************************************************************************/
public record EsbQueueInfo(Long messageCount, Integer consumerCount, Boolean paused)
{
}
