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


import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackBuilder;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.esb.envelope.EsbCausation;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.EsbTriggerMode;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageEOFException;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Runs a trigger's process for ESB events (spec section 6), plus the per-message
 * helpers that EsbTriggerRunner uses around a run: the attempt number, backoff,
 * error text, and dead-letter messages.
 *
 * runProcess is one triggered run.  It is also what a dead-letter replay calls,
 * so it can be called from any thread, including one that has a QContext of its
 * own (which it puts back when done).  A run:
 * - uses a QContext with the handler's instance and the trigger's run-as
 *   session (runAsSessionSupplier, or a QSystemUserSession by default);
 * - checks the session's permission to the process (QPermissionDeniedException
 *   if denied);
 * - runs the process with FrontendStepBehavior.SKIP, and the events as the
 *   esbMessages value (an ArrayList of EsbEvent; size 1 in SINGLE mode);
 * - when the process has a table, and some events are from that table (their
 *   source is qqq://instance/table/thatTable), sets a QProcessCallback whose
 *   filter is primaryKey IN (those events' subjects) - so a table-bound process
 *   runs on the changed records, as it would when run on selected records;
 * - in SINGLE mode, sets EsbCausation to the event's id for the run, so events
 *   published during the run point back at it.  BATCH runs (one run, many
 *   causes) leave causation as it was.
 * A failed run throws: the process's exception, as RunProcessAction throws it.
 *******************************************************************************/
public class EsbTriggerHandler
{
   private static final QLogger LOG = QLogger.getLogger(EsbTriggerHandler.class);

   public static final String VALUE_ESB_MESSAGES = "esbMessages";

   /////////////////////////////////////////////////////////////////
   // JMS properties set on dead letters, next to the original    //
   // message's own properties (ce_id, ce_type, ce_source, etc.)  //
   /////////////////////////////////////////////////////////////////
   public static final String PROPERTY_ERROR          = "qqqError";
   public static final String PROPERTY_FAILED_TRIGGER = "qqqFailedTrigger";
   public static final String PROPERTY_ATTEMPTS       = "qqqAttempts";
   public static final String PROPERTY_FAILED_AT      = "qqqFailedAt";
   public static final String PROPERTY_BODY_DROPPED   = "qqqBodyDropped";

   public static final String  ERROR_UNPARSEABLE = "unparseable message";
   public static final Integer MAX_ERROR_LENGTH  = 2000;

   static final String JMSX_DELIVERY_COUNT = "JMSXDeliveryCount";

   private static final String TABLE_SOURCE_PATH_PREFIX = "table/";

   private final QInstance qInstance;



   /*******************************************************************************
    ** Constructor - for runs against this instance.
    *******************************************************************************/
   public EsbTriggerHandler(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** Run the trigger's process (processName - the process the trigger is on) for
    ** some events: see the class comment.  Throws if the run fails.
    *******************************************************************************/
   public void runProcess(EsbTrigger trigger, String processName, List<EsbEvent> events) throws Exception
   {
      CapturedContext           callerContext   = QContext.capture();
      Map<String, Serializable> callerObjects   = QContext.getObjects();
      String                    callerCausation = EsbCausation.current();
      try
      {
         QContext.init(qInstance, null);
         QContext.setObjects(null);
         QContext.setQSession(getRunAsSession(trigger));

         if(trigger.getMode() != EsbTriggerMode.BATCH && events.size() == 1)
         {
            EsbCausation.set(events.get(0).getId());
         }

         RunProcessInput runProcessInput = new RunProcessInput()
            .withProcessName(processName)
            .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP)
            .withValue(VALUE_ESB_MESSAGES, new ArrayList<>(events))
            .withCallback(buildCallback(processName, events));

         PermissionsHelper.checkProcessPermissionThrowing(runProcessInput, processName);

         QContext.pushAction(runProcessInput);
         new RunProcessAction().execute(runProcessInput);
      }
      finally
      {
         EsbCausation.set(callerCausation);
         restoreContext(callerContext, callerObjects);
      }
   }



   /*******************************************************************************
    ** The trigger's run-as session - made while the QContext has the instance
    ** (a QSystemUserSession reads it for the all-access security keys).
    *******************************************************************************/
   private QSession getRunAsSession(EsbTrigger trigger) throws QException
   {
      QCodeReference codeReference = trigger.getRunAsSessionSupplier();
      if(codeReference == null)
      {
         return (new QSystemUserSession());
      }

      Object supplier;
      if(codeReference instanceof QCodeReferenceLambda<?> lambdaCodeReference)
      {
         supplier = lambdaCodeReference.getLambda();
      }
      else
      {
         supplier = QCodeLoader.getAdHoc(Object.class, codeReference);
      }

      if(!(supplier instanceof Supplier<?> sessionSupplier))
      {
         throw (new QException("Could not load the runAsSessionSupplier (" + codeReference.getName() + ") as a Supplier<QSession>"));
      }

      if(sessionSupplier.get() instanceof QSession session)
      {
         return (session);
      }

      throw (new QException("The runAsSessionSupplier (" + codeReference.getName() + ") did not supply a QSession"));
   }



   /*******************************************************************************
    ** The callback for a process on a table: a filter on the primary keys (the
    ** subjects, as the key's type) of the events from that table - or null if
    ** the process has no table, or no event is from it.
    *******************************************************************************/
   private QProcessCallback buildCallback(String processName, List<EsbEvent> events)
   {
      QProcessMetaData process   = qInstance.getProcess(processName);
      String           tableName = process == null ? null : process.getTableName();
      QTableMetaData   table     = StringUtils.hasContent(tableName) ? qInstance.getTable(tableName) : null;
      if(table == null || table.getPrimaryKeyField() == null)
      {
         return (null);
      }

      QFieldMetaData    primaryKeyField = table.getFields().get(table.getPrimaryKeyField());
      Set<Serializable> primaryKeys     = new LinkedHashSet<>();
      for(EsbEvent event : events)
      {
         if(tableName.equals(getEventTableName(event)) && event.getSubject() != null)
         {
            primaryKeys.add(primaryKeyField == null ? event.getSubject() : ValueUtils.getValueAsFieldType(primaryKeyField.getType(), event.getSubject()));
         }
      }

      if(primaryKeys.isEmpty())
      {
         return (null);
      }

      return (new QProcessCallbackBuilder()
         .withPrimaryKeys(table.getPrimaryKeyField(), new ArrayList<>(primaryKeys))
         .build());
   }



   /*******************************************************************************
    ** Put back the QContext (and its objects) that the calling thread had - or
    ** clear it, if it had none.
    *******************************************************************************/
   private static void restoreContext(CapturedContext callerContext, Map<String, Serializable> callerObjects)
   {
      if(callerContext.qInstance() == null && callerContext.qSession() == null)
      {
         QContext.clear();
      }
      else
      {
         QContext.init(callerContext);
      }
      QContext.setObjects(callerObjects);
   }



   /*******************************************************************************
    ** The table an event is about, from its source (qqq://instance/table/name,
    ** as EsbEventFactory makes for table events) - or null for other events.
    *******************************************************************************/
   static String getEventTableName(EsbEvent event)
   {
      String source = event.getSource();
      if(source == null || !source.startsWith(EsbEventFactory.SOURCE_PREFIX))
      {
         return (null);
      }

      String afterPrefix = source.substring(EsbEventFactory.SOURCE_PREFIX.length());
      int    slashIndex  = afterPrefix.indexOf('/');
      if(slashIndex < 0)
      {
         return (null);
      }

      String path = afterPrefix.substring(slashIndex + 1);
      return (path.startsWith(TABLE_SOURCE_PATH_PREFIX) ? path.substring(TABLE_SOURCE_PATH_PREFIX.length()) : null);
   }



   /*******************************************************************************
    ** The backoff before redelivering after failed attempt number attempt (1 for
    ** the first): retryDelayMs * retryMultiplier ^ (attempt - 1), at most
    ** retryMaxDelayMs.  0 when retryDelayMs is 0 (or unset).
    *******************************************************************************/
   static Long getRetryDelayMs(EsbTrigger trigger, Integer attempt)
   {
      Integer retryDelayMs = trigger.getRetryDelayMs();
      if(retryDelayMs == null || retryDelayMs <= 0)
      {
         return (0L);
      }

      double multiplier = trigger.getRetryMultiplier() == null ? 1.0 : trigger.getRetryMultiplier();
      double delayMs    = retryDelayMs * Math.pow(multiplier, Math.max(0, attempt - 1));
      if(trigger.getRetryMaxDelayMs() != null)
      {
         delayMs = Math.min(delayMs, trigger.getRetryMaxDelayMs());
      }

      return (delayMs >= Long.MAX_VALUE ? Long.MAX_VALUE : Math.round(delayMs));
   }



   /*******************************************************************************
    ** The attempt number of a received message: the broker's JMSXDeliveryCount
    ** (1 on first delivery) - or 1 if the broker doesn't give one.
    *******************************************************************************/
   static Integer getAttempt(Message message)
   {
      try
      {
         if(message.propertyExists(JMSX_DELIVERY_COUNT))
         {
            return (Math.max(1, message.getIntProperty(JMSX_DELIVERY_COUNT)));
         }
      }
      catch(JMSException | RuntimeException e)
      {
         LOG.debug("Could not read a message's delivery count", e);
      }
      return (1);
   }



   /*******************************************************************************
    ** Text for an error: its message (or class name), plus its root cause's
    ** message if that says something more - at most MAX_ERROR_LENGTH characters.
    *******************************************************************************/
   static String getErrorText(Exception exception)
   {
      if(exception == null)
      {
         return (null);
      }

      String    text = StringUtils.hasContent(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName();
      Throwable root = ExceptionUtils.getRootException(exception);
      if(root != exception && StringUtils.hasContent(root.getMessage()) && !text.contains(root.getMessage()))
      {
         text += ": " + root.getMessage();
      }

      return (StringUtils.safeTruncate(text, MAX_ERROR_LENGTH));
   }



   /*******************************************************************************
    ** A dead letter for a received message, made on the session that received it
    ** (so it is sent, and the original acknowledged, in one commit): a copy of
    ** the original (its body - see copyBody - and its properties, but not the
    ** JMS-defined or provider-internal ones), plus the qqqError,
    ** qqqFailedTrigger, qqqAttempts and qqqFailedAt properties - and, for an
    ** object message (whose body isn't copied), qqqBodyDropped true.
    *******************************************************************************/
   static Message buildDeadLetter(Session session, Message original, String triggerName, String error, Integer attempts) throws JMSException
   {
      Message deadLetter = copyBody(session, original);
      if(original instanceof ObjectMessage)
      {
         LOG.warn("Dead-lettering an ObjectMessage without its body, which is not deserialized", logPair("triggerName", triggerName), logPair("messageId", original.getJMSMessageID()));
         deadLetter.setBooleanProperty(PROPERTY_BODY_DROPPED, true);
      }

      copyProperties(original, deadLetter);
      deadLetter.setJMSCorrelationID(original.getJMSCorrelationID());
      deadLetter.setJMSType(original.getJMSType());

      deadLetter.setStringProperty(PROPERTY_ERROR, StringUtils.safeTruncate(error, MAX_ERROR_LENGTH));
      deadLetter.setStringProperty(PROPERTY_FAILED_TRIGGER, triggerName);
      deadLetter.setIntProperty(PROPERTY_ATTEMPTS, attempts);
      deadLetter.setStringProperty(PROPERTY_FAILED_AT, Instant.now().toString());
      return (deadLetter);
   }



   /*******************************************************************************
    ** A new message (made on the session) with a copy of the original's body -
    ** for a text, bytes, map, or stream message.  An object message's body is
    ** not copied, since reading it would deserialize an untrusted payload, so
    ** it (like a message of any other type) gets a message with no body.
    *******************************************************************************/
   private static Message copyBody(Session session, Message original) throws JMSException
   {
      if(original instanceof TextMessage textMessage)
      {
         return (session.createTextMessage(textMessage.getText()));
      }

      if(original instanceof BytesMessage bytesMessage)
      {
         bytesMessage.reset();
         byte[] body = new byte[(int) bytesMessage.getBodyLength()];
         bytesMessage.readBytes(body);

         BytesMessage copy = session.createBytesMessage();
         copy.writeBytes(body);
         return (copy);
      }

      if(original instanceof MapMessage mapMessage)
      {
         MapMessage     copy  = session.createMapMessage();
         Enumeration<?> names = mapMessage.getMapNames();
         while(names.hasMoreElements())
         {
            String name = String.valueOf(names.nextElement());
            copy.setObject(name, mapMessage.getObject(name));
         }
         return (copy);
      }

      if(original instanceof StreamMessage streamMessage)
      {
         streamMessage.reset();
         StreamMessage copy = session.createStreamMessage();
         try
         {
            while(true)
            {
               copy.writeObject(streamMessage.readObject());
            }
         }
         catch(MessageEOFException e)
         {
            return (copy);
         }
      }

      return (session.createMessage());
   }



   /*******************************************************************************
    ** Copy a message's application properties - skipping JMS-defined (JMS...)
    ** and provider-internal (_...) ones, and any from an earlier dead-lettering
    ** (qqq...).  A property the copy won't take is skipped (and logged).
    *******************************************************************************/
   private static void copyProperties(Message from, Message to) throws JMSException
   {
      Enumeration<?> propertyNames = from.getPropertyNames();
      while(propertyNames.hasMoreElements())
      {
         String name = String.valueOf(propertyNames.nextElement());
         if(name.startsWith("JMS") || name.startsWith("_") || name.startsWith("qqq"))
         {
            continue;
         }

         try
         {
            to.setObjectProperty(name, from.getObjectProperty(name));
         }
         catch(JMSException | RuntimeException e)
         {
            LOG.debug("Could not copy a message property to a dead letter", e, logPair("property", name));
         }
      }
   }

}
