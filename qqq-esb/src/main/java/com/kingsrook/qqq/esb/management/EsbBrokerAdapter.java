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


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 * Queue management through a broker's management API (spec section 8): queue
 * depth and consumer counts, and actions on the broker's queues - including
 * dead-letter queues and topic subscription queues, which are queues on the
 * broker too (see EsbBrokerNames).
 *
 * Queues are named by their broker-side names.  An action the broker doesn't
 * support (per capabilities) throws UnsupportedOperationException, without
 * calling the broker.  Anything else that goes wrong - the API can't be
 * reached, refuses the credentials, or reports an error, or the queue isn't
 * there (except for getQueueInfo, which is then empty) - throws QException.
 *
 * Get an adapter for a provider from EsbBrokerAdapters.forProvider.  Browsing
 * messages is plain JMS, in EsbMessageBrowser.
 *******************************************************************************/
public interface EsbBrokerAdapter
{
   /*******************************************************************************
    ** The actions this adapter's broker supports.
    *******************************************************************************/
   EsbBrokerCapabilities capabilities();

   /*******************************************************************************
    ** The queue's depth, consumer count, and paused state - or empty if the
    ** broker has no such queue.
    *******************************************************************************/
   Optional<EsbQueueInfo> getQueueInfo(String brokerQueueName) throws QException;

   /*******************************************************************************
    ** Pause delivery to all of the queue's consumers (QQQ's and any others);
    ** messages keep arriving, and wait.  UnsupportedOperationException if
    ** !capabilities().pauseQueue().
    *******************************************************************************/
   void pauseQueue(String brokerQueueName) throws QException;

   /*******************************************************************************
    ** Resume delivery to the queue's consumers, after pauseQueue.
    ** UnsupportedOperationException if !capabilities().pauseQueue().
    *******************************************************************************/
   void resumeQueue(String brokerQueueName) throws QException;

   /*******************************************************************************
    ** Remove all of the queue's messages; returns how many.
    ** UnsupportedOperationException if !capabilities().purge().
    *******************************************************************************/
   long purgeQueue(String brokerQueueName) throws QException;

   /*******************************************************************************
    ** Remove the messages with these JMS message ids (as browsing shows them);
    ** returns how many were removed.  With no ids, removes nothing.
    ** UnsupportedOperationException if !capabilities().deleteSelected().
    *******************************************************************************/
   int deleteMessages(String brokerQueueName, List<String> messageIds) throws QException;

   /*******************************************************************************
    ** Remove the messages whose JMS timestamp is before the cutoff; returns how
    ** many were removed.  UnsupportedOperationException if
    ** !capabilities().deleteOlderThan().
    *******************************************************************************/
   int deleteMessagesOlderThan(String brokerQueueName, Instant cutoff) throws QException;

   /*******************************************************************************
    ** Move the messages with these JMS message ids to another (existing) queue;
    ** returns how many were moved.  With no ids, moves nothing.
    ** UnsupportedOperationException if !capabilities().move().
    *******************************************************************************/
   int moveMessages(String brokerQueueName, List<String> messageIds, String toBrokerQueueName) throws QException;

}
