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

package com.kingsrook.qqq.esb.processes;


import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbCausation;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapter;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapters;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.runtime.EsbTriggerControl;
import com.kingsrook.qqq.esb.runtime.EsbTriggerHandler;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;


/*******************************************************************************
 * Server-side permission boundary and implementation for ESB operations.
 ******************************************************************************/
public class EsbOperateStep implements BackendStep
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
   {
      PermissionsHelper.checkProcessPermissionThrowing(input, input.getProcessName());
      Serializable count = switch(input.getProcessName())
      {
         case "esbPauseQueue" -> queueAction(input, "pause");
         case "esbResumeQueue" -> queueAction(input, "resume");
         case "esbPurgeQueue" -> queueAction(input, "purge");
         case "esbDeleteMessages" -> queueAction(input, "delete");
         case "esbMoveMessages" -> queueAction(input, "move");
         case "esbReplayDeadLetters" -> replay(input);
         case "esbPauseTrigger" -> triggerAction(input, "pause");
         case "esbResumeTrigger" -> triggerAction(input, "resume");
         case "esbRestartTrigger" -> triggerAction(input, "restart");
         default -> throw (new QException("ESB operation is not implemented: " + input.getProcessName()));
      };
      output.addValue("count", count).addValue("message", input.getProcessName() + " completed.");
   }



   /*******************************************************************************
    ** Perform one broker queue action, with explicit capability checks.
    *******************************************************************************/
   private static Serializable queueAction(RunBackendStepInput input, String action) throws QException
   {
      String providerName    = required(input, "providerName");
      String brokerQueueName = required(input, "brokerQueueName");
      EsbBrokerAdapter adapter = EsbBrokerAdapters.forProvider(providerName)
         .orElseThrow(() -> new QException("ESB provider " + providerName + " has no management adapter; configure managementUrl."));

      try
      {
         return (switch(action)
         {
            case "pause" ->
            {
               requireCapability(adapter.capabilities().pauseQueue(), action);
               adapter.pauseQueue(brokerQueueName);
               yield 0;
            }
            case "resume" ->
            {
               requireCapability(adapter.capabilities().pauseQueue(), action);
               adapter.resumeQueue(brokerQueueName);
               yield 0;
            }
            case "purge" ->
            {
               requireCapability(adapter.capabilities().purge(), action);
               yield adapter.purgeQueue(brokerQueueName);
            }
            case "delete" ->
            {
               String ids = input.getValueString("messageIds");
               String olderThan = input.getValueString("olderThan");
               if(StringUtils.hasContent(ids) == StringUtils.hasContent(olderThan))
               {
                  throw (new QBadRequestException("Give either messageIds or olderThan for esbDeleteMessages."));
               }
               if(StringUtils.hasContent(ids))
               {
                  requireCapability(adapter.capabilities().deleteSelected(), action);
                  yield adapter.deleteMessages(brokerQueueName, ids(ids));
               }
               requireCapability(adapter.capabilities().deleteOlderThan(), action);
               try
               {
                  yield adapter.deleteMessagesOlderThan(brokerQueueName, Instant.parse(olderThan));
               }
               catch(DateTimeParseException e)
               {
                  throw (new QBadRequestException("olderThan must be an ISO instant."));
               }
            }
            case "move" ->
            {
               requireCapability(adapter.capabilities().move(), action);
               yield adapter.moveMessages(brokerQueueName, ids(required(input, "messageIds")), required(input, "toBrokerQueueName"));
            }
            default -> throw (new QException("Unknown ESB queue action: " + action));
         });
      }
      catch(UnsupportedOperationException e)
      {
         throw (new QException("ESB queue action " + action + " is unsupported by provider " + providerName, e));
      }
   }



   /*******************************************************************************
    ** Broadcast a trigger action to every runtime on its provider.
    *******************************************************************************/
   private static Integer triggerAction(RunBackendStepInput input, String action) throws QException
   {
      String triggerName = required(input, "triggerName");
      switch(action)
      {
         case "pause" -> EsbTriggerControl.pause(triggerName);
         case "resume" -> EsbTriggerControl.resume(triggerName);
         case "restart" -> EsbTriggerControl.restart(triggerName);
         default -> throw (new QException("Unknown ESB trigger action: " + action));
      }
      return (0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void requireCapability(boolean supported, String action) throws QException
   {
      if(!supported)
      {
         throw (new QException("ESB queue action " + action + " is unsupported by this provider."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String required(RunBackendStepInput input, String name) throws QBadRequestException
   {
      String value = input.getValueString(name);
      if(!StringUtils.hasContent(value))
      {
         throw (new QBadRequestException(name + " is required."));
      }
      return (value.trim());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> ids(String value) throws QBadRequestException
   {
      List<String> result = Arrays.stream(value.split(","))
         .map(String::trim)
         .filter(StringUtils::hasContent)
         .distinct()
         .toList();
      if(result.isEmpty())
      {
         throw (new QBadRequestException("messageIds must include at least one id."));
      }
      return (result);
   }



   /*******************************************************************************
    ** Replay dead letters directly through the trigger's process.  Each
    ** successful run commits its receive; any failure rolls it back.
    *******************************************************************************/
   private static Integer replay(RunBackendStepInput input) throws QException
   {
      String triggerName = required(input, "triggerName");
      String messageIds  = input.getValueString("messageIds");
      boolean all        = Boolean.TRUE.equals(input.getValueBoolean("all"));
      if(all == StringUtils.hasContent(messageIds))
      {
         throw (new QBadRequestException("Give either messageIds or all=true for esbReplayDeadLetters."));
      }

      QInstance instance = QContext.getQInstance();
      EsbInstanceMetaData esb = QSupplementalInstanceMetaData.of(instance, EsbInstanceMetaData.NAME);
      if(esb == null)
      {
         throw (new QException("Unknown ESB trigger: " + triggerName));
      }

      for(QProcessMetaData process : instance.getProcesses().values())
      {
         EsbProcessMetaData processEsb = EsbProcessMetaData.of(process);
         if(processEsb == null)
         {
            continue;
         }
         for(EsbTrigger trigger : CollectionUtils.nonNullList(processEsb.getTriggers()))
         {
            QEsbDestinationMetaData destination = esb.getDestination(trigger.getDestinationName());
            if(destination != null && triggerName.equals(trigger.getName(process.getName())))
            {
               String queueName = trigger.getEffectiveDeadLetterDestinationName(process.getName(), destination);
               return (replayQueue(instance, process.getName(), trigger, destination.getProviderName(), queueName, all ? null : ids(messageIds)));
            }
         }
      }
      throw (new QException("Unknown ESB trigger: " + triggerName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Integer replayQueue(QInstance instance, String processName, EsbTrigger trigger, String providerName, String queueName, List<String> selectedIds) throws QException
   {
      int count = 0;
      try(Session session = EsbConnectionManager.getInstance().openSession(providerName, true))
      {
         Queue queue = EsbConnectionManager.getInstance().resolveQueue(session, providerName, queueName);
         if(selectedIds == null)
         {
            try(MessageConsumer consumer = session.createConsumer(queue))
            {
               Message message;
               while((message = consumer.receiveNoWait()) != null)
               {
                  replayOne(session, instance, processName, trigger, message);
                  count++;
               }
            }
         }
         else
         {
            for(String id : selectedIds)
            {
               String selector = "JMSMessageID = '" + id.replace("'", "''") + "'";
               try(MessageConsumer consumer = session.createConsumer(queue, selector))
               {
                  Message message = consumer.receiveNoWait();
                  if(message != null)
                  {
                     replayOne(session, instance, processName, trigger, message);
                     count++;
                  }
               }
            }
         }
         return (count);
      }
      catch(JMSException e)
      {
         throw (new QException("Could not replay dead letters on ESB provider " + providerName, e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void replayOne(Session session, QInstance instance, String processName, EsbTrigger trigger, Message message) throws QException, JMSException
   {
      String priorCausation = EsbCausation.current();
      try
      {
         var event = EsbEventCodec.fromMessage(message);
         EsbCausation.set(event.getId());
         new EsbTriggerHandler(instance).runProcess(trigger, processName, List.of(event));
         session.commit();
      }
      catch(Exception e)
      {
         session.rollback();
         throw (new QException("ESB dead-letter replay failed: " + e.getMessage(), e));
      }
      finally
      {
         EsbCausation.set(priorCausation);
      }
   }
}
