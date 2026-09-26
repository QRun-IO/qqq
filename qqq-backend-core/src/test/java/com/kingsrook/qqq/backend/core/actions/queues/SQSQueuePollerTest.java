/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.queues;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSPollerSettings;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueProviderMetaData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for SQSQueuePoller 
 *******************************************************************************/
class SQSQueuePollerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetSqsPollerSettings()
   {
      ///////////////////
      // defaults only //
      ///////////////////
      assertSettings(Integer.MAX_VALUE, 10, 20, SQSQueuePoller.getSqsPollerSettings(null, null));
      assertSettings(Integer.MAX_VALUE, 10, 20, SQSQueuePoller.getSqsPollerSettings(new SQSQueueProviderMetaData(), new SQSQueueMetaData()));
      assertSettings(Integer.MAX_VALUE, 10, 20, SQSQueuePoller.getSqsPollerSettings(new SQSQueueProviderMetaData().withPollerSettings(new SQSPollerSettings()), new SQSQueueMetaData().withPollerSettings(new SQSPollerSettings())));

      ///////////////////////////////////
      // settings only in the provider //
      ///////////////////////////////////
      assertSettings(100, 5, 1, SQSQueuePoller.getSqsPollerSettings(
         new SQSQueueProviderMetaData().withPollerSettings(new SQSPollerSettings().withMaxLoops(100).withMaxNumberOfMessages(5).withWaitTimeSeconds(1)),
         new QQueueMetaData()));

      ////////////////////////////////
      // settings only in the queue //
      ////////////////////////////////
      assertSettings(90, 4, 2, SQSQueuePoller.getSqsPollerSettings(
         new SQSQueueProviderMetaData(),
         new SQSQueueMetaData().withPollerSettings(new SQSPollerSettings().withMaxLoops(90).withMaxNumberOfMessages(4).withWaitTimeSeconds(2))));

      /////////////////////////////////////////
      // mix of default, provider, and queue //
      /////////////////////////////////////////
      assertSettings(Integer.MAX_VALUE, 5, 2, SQSQueuePoller.getSqsPollerSettings(
         new SQSQueueProviderMetaData().withPollerSettings(new SQSPollerSettings().withMaxNumberOfMessages(5)),
         new SQSQueueMetaData().withPollerSettings(new SQSPollerSettings().withWaitTimeSeconds(2))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertSettings(Integer maxLoops, Integer maxNumberOfMessages, Integer waitTimeSeconds, SQSPollerSettings sqsPollerSettings)
   {
      assertEquals(maxLoops, sqsPollerSettings.getMaxLoops());
      assertEquals(maxNumberOfMessages, sqsPollerSettings.getMaxNumberOfMessages());
      assertEquals(waitTimeSeconds, sqsPollerSettings.getWaitTimeSeconds());
   }

}