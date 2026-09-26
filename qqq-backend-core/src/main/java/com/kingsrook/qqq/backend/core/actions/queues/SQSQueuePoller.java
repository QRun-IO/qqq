/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSPollerSettings;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;


/*******************************************************************************
 ** Class to poll an SQS queue, and run process code for each message found.
 *******************************************************************************/
public class SQSQueuePoller implements Runnable
{
   private static final QLogger LOG = QLogger.getLogger(SQSQueuePoller.class);

   ///////////////////////////////////////////////
   // todo - move these 2 to a "QBaseRunnable"? //
   ///////////////////////////////////////////////
   private QInstance          qInstance;
   private Supplier<QSession> sessionSupplier;

   private SQSQueueProviderMetaData queueProviderMetaData;
   private QQueueMetaData           queueMetaData;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run()
   {
      QContext.init(qInstance, sessionSupplier.get());

      String originalThreadName = Thread.currentThread().getName();
      Thread.currentThread().setName("SQSPoller>" + queueMetaData.getName());
      LOG.debug("Running " + this.getClass().getSimpleName() + "[" + queueMetaData.getName() + "]");

      try
      {
         BasicAWSCredentials credentials = new BasicAWSCredentials(queueProviderMetaData.getAccessKey(), queueProviderMetaData.getSecretKey());
         final AmazonSQS sqs = AmazonSQSClientBuilder.standard()
            .withRegion(queueProviderMetaData.getRegion())
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();

         String queueUrl = queueProviderMetaData.getBaseURL();
         if(!queueUrl.endsWith("/"))
         {
            queueUrl += "/";
         }
         queueUrl += queueMetaData.getQueueName();

         SQSPollerSettings sqsPollerSettings = getSqsPollerSettings(queueProviderMetaData, queueMetaData);

         for(int loop = 0; loop < sqsPollerSettings.getMaxLoops(); loop++)
         {
            ///////////////////////////////
            // fetch a batch of messages //
            ///////////////////////////////
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest();
            receiveMessageRequest.setQueueUrl(queueUrl);
            receiveMessageRequest.setMaxNumberOfMessages(sqsPollerSettings.getMaxNumberOfMessages());
            receiveMessageRequest.setWaitTimeSeconds(sqsPollerSettings.getWaitTimeSeconds()); // larger value (e.g., 20) can help urge SQS to query multiple servers and find more messages
            ReceiveMessageResult receiveMessageResult = sqs.receiveMessage(receiveMessageRequest);
            if(receiveMessageResult.getMessages().isEmpty())
            {
               LOG.debug("0 messages received.  Breaking.");
               break;
            }
            LOG.debug(receiveMessageResult.getMessages().size() + " messages received.  Processing.");

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // extract data from the messages into list of bodies to pass into process, and list of delete-batch-inputs //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////
            List<DeleteMessageBatchRequestEntry> deleteRequestEntries = new ArrayList<>();
            ArrayList<String>                    bodies               = new ArrayList<>();
            int                                  i                    = 0;
            for(Message message : receiveMessageResult.getMessages())
            {
               bodies.add(message.getBody());
               deleteRequestEntries.add(new DeleteMessageBatchRequestEntry(String.valueOf(i++), message.getReceiptHandle()));
            }

            /////////////////////////////////////////////////////////////////////////////////////
            // run the process, in a try-catch, so even if it fails, our loop keeps going.     //
            // the messages in a failed process will get re-delivered, to try-again, up to the //
            // number of times configured in AWS                                               //
            /////////////////////////////////////////////////////////////////////////////////////
            try
            {
               RunProcessInput runProcessInput = new RunProcessInput();
               runProcessInput.setProcessName(queueMetaData.getProcessName());
               runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
               runProcessInput.addValue("bodies", bodies);

               QContext.pushAction(runProcessInput);

               RunProcessAction runProcessAction = new RunProcessAction();
               RunProcessOutput runProcessOutput = runProcessAction.execute(runProcessInput);

               ////////////////////////////////////////////////////////////////////////////////////////////
               // if there was an exception returned by the process (e.g., thrown in backend step), then //
               // warn and leave the messages for re-processing.                                         //
               ////////////////////////////////////////////////////////////////////////////////////////////
               if(runProcessOutput.getException().isPresent())
               {
                  LOG.warn("Exception returned by process when handling SQS Messages.  They will not be deleted from the queue.", runProcessOutput.getException().get());
               }
               else
               {
                  ///////////////////////////////////////////////
                  // else, if no exception, do a batch delete. //
                  ///////////////////////////////////////////////
                  sqs.deleteMessageBatch(new DeleteMessageBatchRequest()
                     .withQueueUrl(queueUrl)
                     .withEntries(deleteRequestEntries));
               }
            }
            catch(Exception e)
            {
               LOG.warn("Error receiving SQS Messages.", e);
            }
            finally
            {
               QContext.popAction();
            }
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error running SQS Queue Poller", e);
      }
      finally
      {
         Thread.currentThread().setName(originalThreadName);
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** For a given queueProvider and queue, get the poller settings to use (using
    ** default values if none are set at either level).
    *******************************************************************************/
   static SQSPollerSettings getSqsPollerSettings(SQSQueueProviderMetaData queueProviderMetaData, QQueueMetaData queueMetaData)
   {
      /////////////////////////////////
      // start with default settings //
      /////////////////////////////////
      SQSPollerSettings sqsPollerSettings = new SQSPollerSettings()
         .withMaxLoops(Integer.MAX_VALUE)
         .withMaxNumberOfMessages(10)
         .withWaitTimeSeconds(20);

      /////////////////////////////////////////////////////////////////////
      // if the queue provider has settings, let them overwrite defaults //
      /////////////////////////////////////////////////////////////////////
      if(queueProviderMetaData != null && queueProviderMetaData.getPollerSettings() != null)
      {
         SQSPollerSettings providerSettings = queueProviderMetaData.getPollerSettings();
         sqsPollerSettings.setMaxLoops(Objects.requireNonNullElse(providerSettings.getMaxLoops(), sqsPollerSettings.getMaxLoops()));
         sqsPollerSettings.setMaxNumberOfMessages(Objects.requireNonNullElse(providerSettings.getMaxNumberOfMessages(), sqsPollerSettings.getMaxNumberOfMessages()));
         sqsPollerSettings.setWaitTimeSeconds(Objects.requireNonNullElse(providerSettings.getWaitTimeSeconds(), sqsPollerSettings.getWaitTimeSeconds()));
      }

      ////////////////////////////////////////////////////////////
      // if the queue has settings, let them overwrite defaults //
      ////////////////////////////////////////////////////////////
      if(queueMetaData instanceof SQSQueueMetaData sqsQueueMetaData && sqsQueueMetaData.getPollerSettings() != null)
      {
         SQSPollerSettings providerSettings = sqsQueueMetaData.getPollerSettings();
         sqsPollerSettings.setMaxLoops(Objects.requireNonNullElse(providerSettings.getMaxLoops(), sqsPollerSettings.getMaxLoops()));
         sqsPollerSettings.setMaxNumberOfMessages(Objects.requireNonNullElse(providerSettings.getMaxNumberOfMessages(), sqsPollerSettings.getMaxNumberOfMessages()));
         sqsPollerSettings.setWaitTimeSeconds(Objects.requireNonNullElse(providerSettings.getWaitTimeSeconds(), sqsPollerSettings.getWaitTimeSeconds()));
      }

      return sqsPollerSettings;
   }



   /*******************************************************************************
    ** Setter for queueProviderMetaData
    **
    *******************************************************************************/
   public void setQueueProviderMetaData(SQSQueueProviderMetaData queueProviderMetaData)
   {
      this.queueProviderMetaData = queueProviderMetaData;
   }



   /*******************************************************************************
    ** Setter for queueMetaData
    **
    *******************************************************************************/
   public void setQueueMetaData(QQueueMetaData queueMetaData)
   {
      this.queueMetaData = queueMetaData;
   }



   /*******************************************************************************
    ** Setter for qInstance
    **
    *******************************************************************************/
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** Setter for sessionSupplier
    **
    *******************************************************************************/
   public void setSessionSupplier(Supplier<QSession> sessionSupplier)
   {
      this.sessionSupplier = sessionSupplier;
   }
}
