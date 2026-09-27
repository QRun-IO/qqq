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
 * A broker queue's state, from the broker's management API, at the moment it
 * was asked.
 *
 * - messageCount: messages on the queue, including any delivered to a
 *   consumer but not yet acknowledged.
 * - consumerCount: consumers attached to the queue as reported by broker
 *   management, across all nodes. RabbitMQ JMS 3.9 polls synchronously with
 *   basicGet, so RabbitMQ reports zero here even while a QQQ trigger is
 *   polling and processing messages. Zero does not mean the trigger is
 *   inactive; use its runtime state and trigger metadata for the QQQ
 *   subscriber view.
 * - paused: whether delivery to the queue's consumers is paused on the broker
 *   (always false on a broker that can't pause a queue, e.g., RabbitMQ).
 *******************************************************************************/
public record EsbQueueInfo(Long messageCount, Integer consumerCount, Boolean paused)
{
}
