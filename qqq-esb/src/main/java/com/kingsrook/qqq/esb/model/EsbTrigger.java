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

package com.kingsrook.qqq.esb.model;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 * A trigger that runs a process from messages on an ESB queue or topic.
 *
 * A trigger doesn't know its process's name (it lives in the process's
 * EsbProcessMetaData), so the derived names take it as a parameter:
 * - name: processName.destinationName (the QQQ destination name).
 * - subscriptionName (topics only): defaults to processName.X, where X is the
 *   destination's broker-side name (QEsbDestinationMetaData
 *   getEffectiveDestinationName).
 * - deadLetterDestinationName: defaults to X.dlq for a queue, or
 *   X.subscriptionName.dlq for a topic (X again being the broker-side name).
 * The subscription and dead-letter names are names on the broker.  When the
 * destination has no separate broker-side name, X is its QQQ name, and the
 * default subscription name is the same as the trigger's name.
 *
 * Other defaults are per spec section 3.  batchSize and batchWaitMs are left
 * unset unless given (so validation can reject them in SINGLE mode); use
 * getEffectiveBatchSize and getEffectiveBatchWaitMs for their values.
 *******************************************************************************/
public class EsbTrigger
{
   public static final Integer DEFAULT_BATCH_SIZE    = 100;
   public static final Integer DEFAULT_BATCH_WAIT_MS = 1000;

   private String              destinationName;
   private String              subscriptionName;
   private EsbTriggerMode      mode                      = EsbTriggerMode.SINGLE;
   private Integer             batchSize;
   private Integer             batchWaitMs;
   private Integer             concurrency               = 1;
   private Integer             maxAttempts               = 3;
   private Integer             retryDelayMs              = 0;
   private Double              retryMultiplier           = 2.0;
   private Integer             retryMaxDelayMs           = 60_000;
   private EsbDeadLetterAction onDeadLetter              = EsbDeadLetterAction.DEAD_LETTER_QUEUE;
   private String              deadLetterDestinationName;
   private QCodeReference      runAsSessionSupplier;
   private Integer             timeoutMs;
   private Boolean             startPaused               = false;



   /*******************************************************************************
    ** The trigger's name: processName.destinationName
    *******************************************************************************/
   public String getName(String processName)
   {
      return (processName + "." + destinationName);
   }



   /*******************************************************************************
    ** The shared durable subscription name (topic triggers): subscriptionName if
    ** set, else processName.X, where X is the broker-side name of this trigger's
    ** destination, which is looked up in the ESB meta-data of the QContext's
    ** instance (so, on a thread of your own, call this after QContext.init, or
    ** use the overload that takes the destination).
    **
    ** Throws QRuntimeException if a default is needed but the destination can't
    ** be found there.
    *******************************************************************************/
   public String getEffectiveSubscriptionName(String processName)
   {
      if(StringUtils.hasContent(subscriptionName))
      {
         return (subscriptionName);
      }

      return (getEffectiveSubscriptionName(processName, lookUpDestination()));
   }



   /*******************************************************************************
    ** The shared durable subscription name (topic triggers), given this trigger's
    ** destination: subscriptionName if set, else processName.X, where X is the
    ** destination's broker-side name.
    *******************************************************************************/
   public String getEffectiveSubscriptionName(String processName, QEsbDestinationMetaData destination)
   {
      if(StringUtils.hasContent(subscriptionName))
      {
         return (subscriptionName);
      }

      Objects.requireNonNull(destination, "destination");
      return (processName + "." + destination.getEffectiveDestinationName());
   }



   /*******************************************************************************
    ** The dead-letter destination (broker queue) name: deadLetterDestinationName
    ** if set, else the default for this trigger's destination, which is looked up
    ** in the ESB meta-data of the QContext's instance (so, on a thread of your
    ** own, call this after QContext.init, or use the overload that takes the
    ** destination).
    **
    ** Throws QRuntimeException if a default is needed but the destination can't
    ** be found there.
    *******************************************************************************/
   public String getEffectiveDeadLetterDestinationName(String processName)
   {
      if(StringUtils.hasContent(deadLetterDestinationName))
      {
         return (deadLetterDestinationName);
      }

      return (getEffectiveDeadLetterDestinationName(processName, lookUpDestination()));
   }



   /*******************************************************************************
    ** The dead-letter destination (broker queue) name, given this trigger's
    ** destination: deadLetterDestinationName if set, else X.dlq for a queue, or
    ** X.effectiveSubscriptionName.dlq for a topic, where X is the destination's
    ** broker-side name.
    *******************************************************************************/
   public String getEffectiveDeadLetterDestinationName(String processName, QEsbDestinationMetaData destination)
   {
      if(StringUtils.hasContent(deadLetterDestinationName))
      {
         return (deadLetterDestinationName);
      }

      Objects.requireNonNull(destination, "destination");
      String brokerDestinationName = destination.getEffectiveDestinationName();
      if(destination.getType() == EsbDestinationType.TOPIC)
      {
         return (brokerDestinationName + "." + getEffectiveSubscriptionName(processName, destination) + ".dlq");
      }

      return (brokerDestinationName + ".dlq");
   }



   /*******************************************************************************
    ** batchSize if set, else the default (100).
    *******************************************************************************/
   public Integer getEffectiveBatchSize()
   {
      return (batchSize == null ? DEFAULT_BATCH_SIZE : batchSize);
   }



   /*******************************************************************************
    ** batchWaitMs if set, else the default (1000).
    *******************************************************************************/
   public Integer getEffectiveBatchWaitMs()
   {
      return (batchWaitMs == null ? DEFAULT_BATCH_WAIT_MS : batchWaitMs);
   }



   /*******************************************************************************
    ** Find this trigger's destination (with a type) in the QContext's instance.
    *******************************************************************************/
   private QEsbDestinationMetaData lookUpDestination()
   {
      QInstance               qInstance           = QContext.getQInstance();
      EsbInstanceMetaData     esbInstanceMetaData = qInstance == null ? null : EsbInstanceMetaData.ofOrNull(qInstance);
      QEsbDestinationMetaData destination         = esbInstanceMetaData == null ? null : esbInstanceMetaData.getDestination(destinationName);
      if(destination == null || destination.getType() == null)
      {
         throw (new QRuntimeException("Cannot determine the default subscription or dead-letter destination name for ESB destination " + destinationName + ": it is not defined (with a type) in the ESB meta-data of the QContext's instance."));
      }
      return (destination);
   }



   /*******************************************************************************
    ** Validate this trigger, which is on the named process.  Called by
    ** EsbProcessMetaData.validate (which also checks for duplicate triggers).
    *******************************************************************************/
   void validate(String processName, EsbInstanceMetaData esbInstanceMetaData, QInstanceValidator validator)
   {
      String prefix = "ESB trigger " + getName(processName) + " ";

      QEsbDestinationMetaData destination = esbInstanceMetaData == null ? null : esbInstanceMetaData.getDestination(destinationName);
      validator.assertCondition(destination != null, prefix + "references an unknown destination: " + destinationName + ".");
      if(destination != null)
      {
         boolean subscriptionOnQueue = StringUtils.hasContent(subscriptionName) && destination.getType() == EsbDestinationType.QUEUE;
         validator.assertCondition(!subscriptionOnQueue, prefix + "has a subscriptionName, but destination " + destinationName + " is a QUEUE (subscriptionName is only for topics).");
      }

      validator.assertCondition(concurrency != null && concurrency >= 1, prefix + "concurrency must be at least 1.");
      validator.assertCondition(maxAttempts != null && maxAttempts >= 1, prefix + "maxAttempts must be at least 1.");
      validator.assertCondition(onDeadLetter != null, prefix + "is missing an onDeadLetter action.");
      validator.assertCondition(timeoutMs == null || timeoutMs > 0, prefix + "timeoutMs must be greater than 0.");
      validateRetryFields(prefix, validator);

      validator.assertCondition(mode != null, prefix + "is missing a mode.");
      if(mode == EsbTriggerMode.BATCH)
      {
         validator.assertCondition(batchSize == null || batchSize >= 1, prefix + "batchSize must be at least 1.");
         validator.assertCondition(batchWaitMs == null || batchWaitMs >= 0, prefix + "batchWaitMs must be at least 0.");
      }
      else if(mode != null)
      {
         validator.assertCondition(batchSize == null && batchWaitMs == null, prefix + "sets batchSize or batchWaitMs, but its mode is " + mode + " (batch fields are only for BATCH mode).");
      }

      if(runAsSessionSupplier != null)
      {
         validateRunAsSessionSupplier(prefix, validator);
      }
   }



   /*******************************************************************************
    ** Backoff fields: a delay of at least 0, a multiplier of at least 1, and a
    ** maximum delay no less than the (first) delay.
    *******************************************************************************/
   private void validateRetryFields(String prefix, QInstanceValidator validator)
   {
      validator.assertCondition(retryDelayMs != null && retryDelayMs >= 0, prefix + "retryDelayMs must be at least 0.");
      validator.assertCondition(retryMultiplier != null && retryMultiplier >= 1, prefix + "retryMultiplier must be at least 1.");
      validator.assertCondition(retryMaxDelayMs != null && (retryDelayMs == null || retryMaxDelayMs >= retryDelayMs), prefix + "retryMaxDelayMs must be at least retryDelayMs.");
   }



   /*******************************************************************************
    ** The run-as session supplier must be a Supplier - and, if its type argument
    ** can be seen (i.e., on a class that implements Supplier<X>), X must be a
    ** QSession.
    *******************************************************************************/
   private void validateRunAsSessionSupplier(String prefix, QInstanceValidator validator)
   {
      int errorCountBefore = validator.getErrors().size();
      validator.validateSimpleCodeReference(prefix + "runAsSessionSupplier: ", runAsSessionSupplier, Supplier.class);
      if(validator.getErrors().size() > errorCountBefore || runAsSessionSupplier instanceof QCodeReferenceLambda<?>)
      {
         return;
      }

      Class<?> supplierClass;
      try
      {
         supplierClass = Class.forName(runAsSessionSupplier.getName());
      }
      catch(ClassNotFoundException e)
      {
         /////////////////////////////////////////////////////////////////
         // not reachable - validateSimpleCodeReference already loaded  //
         // the class (and would have added an error if it couldn't)    //
         /////////////////////////////////////////////////////////////////
         return;
      }

      Type suppliedType = getSuppliedType(supplierClass);
      if(suppliedType instanceof Class<?> suppliedClass)
      {
         validator.assertCondition(QSession.class.isAssignableFrom(suppliedClass), prefix + "runAsSessionSupplier must implement Supplier<QSession>, but " + supplierClass.getName() + " supplies " + suppliedClass.getName() + ".");
      }
   }



   /*******************************************************************************
    ** Find the type argument X of Supplier<X>, as declared on a class or one of
    ** its superclasses - or null if not declared.
    *******************************************************************************/
   private static Type getSuppliedType(Class<?> clazz)
   {
      for(Class<?> c = clazz; c != null; c = c.getSuperclass())
      {
         for(Type genericInterface : c.getGenericInterfaces())
         {
            if(genericInterface instanceof ParameterizedType parameterizedType && Supplier.class.equals(parameterizedType.getRawType()))
            {
               return (parameterizedType.getActualTypeArguments()[0]);
            }
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** Getter for destinationName
    *******************************************************************************/
   public String getDestinationName()
   {
      return (this.destinationName);
   }



   /*******************************************************************************
    ** Setter for destinationName
    *******************************************************************************/
   public void setDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
   }



   /*******************************************************************************
    ** Fluent setter for destinationName
    *******************************************************************************/
   public EsbTrigger withDestinationName(String destinationName)
   {
      this.destinationName = destinationName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for subscriptionName.  See also getEffectiveSubscriptionName.
    *******************************************************************************/
   public String getSubscriptionName()
   {
      return (this.subscriptionName);
   }



   /*******************************************************************************
    ** Setter for subscriptionName
    *******************************************************************************/
   public void setSubscriptionName(String subscriptionName)
   {
      this.subscriptionName = subscriptionName;
   }



   /*******************************************************************************
    ** Fluent setter for subscriptionName
    *******************************************************************************/
   public EsbTrigger withSubscriptionName(String subscriptionName)
   {
      this.subscriptionName = subscriptionName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mode
    *******************************************************************************/
   public EsbTriggerMode getMode()
   {
      return (this.mode);
   }



   /*******************************************************************************
    ** Setter for mode
    *******************************************************************************/
   public void setMode(EsbTriggerMode mode)
   {
      this.mode = mode;
   }



   /*******************************************************************************
    ** Fluent setter for mode
    *******************************************************************************/
   public EsbTrigger withMode(EsbTriggerMode mode)
   {
      this.mode = mode;
      return (this);
   }



   /*******************************************************************************
    ** Getter for batchSize (null unless set).  See also getEffectiveBatchSize.
    *******************************************************************************/
   public Integer getBatchSize()
   {
      return (this.batchSize);
   }



   /*******************************************************************************
    ** Setter for batchSize
    *******************************************************************************/
   public void setBatchSize(Integer batchSize)
   {
      this.batchSize = batchSize;
   }



   /*******************************************************************************
    ** Fluent setter for batchSize
    *******************************************************************************/
   public EsbTrigger withBatchSize(Integer batchSize)
   {
      this.batchSize = batchSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for batchWaitMs (null unless set).  See also getEffectiveBatchWaitMs.
    *******************************************************************************/
   public Integer getBatchWaitMs()
   {
      return (this.batchWaitMs);
   }



   /*******************************************************************************
    ** Setter for batchWaitMs
    *******************************************************************************/
   public void setBatchWaitMs(Integer batchWaitMs)
   {
      this.batchWaitMs = batchWaitMs;
   }



   /*******************************************************************************
    ** Fluent setter for batchWaitMs
    *******************************************************************************/
   public EsbTrigger withBatchWaitMs(Integer batchWaitMs)
   {
      this.batchWaitMs = batchWaitMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for concurrency
    *******************************************************************************/
   public Integer getConcurrency()
   {
      return (this.concurrency);
   }



   /*******************************************************************************
    ** Setter for concurrency
    *******************************************************************************/
   public void setConcurrency(Integer concurrency)
   {
      this.concurrency = concurrency;
   }



   /*******************************************************************************
    ** Fluent setter for concurrency
    *******************************************************************************/
   public EsbTrigger withConcurrency(Integer concurrency)
   {
      this.concurrency = concurrency;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxAttempts
    *******************************************************************************/
   public Integer getMaxAttempts()
   {
      return (this.maxAttempts);
   }



   /*******************************************************************************
    ** Setter for maxAttempts
    *******************************************************************************/
   public void setMaxAttempts(Integer maxAttempts)
   {
      this.maxAttempts = maxAttempts;
   }



   /*******************************************************************************
    ** Fluent setter for maxAttempts
    *******************************************************************************/
   public EsbTrigger withMaxAttempts(Integer maxAttempts)
   {
      this.maxAttempts = maxAttempts;
      return (this);
   }



   /*******************************************************************************
    ** Getter for retryDelayMs
    *******************************************************************************/
   public Integer getRetryDelayMs()
   {
      return (this.retryDelayMs);
   }



   /*******************************************************************************
    ** Setter for retryDelayMs
    *******************************************************************************/
   public void setRetryDelayMs(Integer retryDelayMs)
   {
      this.retryDelayMs = retryDelayMs;
   }



   /*******************************************************************************
    ** Fluent setter for retryDelayMs
    *******************************************************************************/
   public EsbTrigger withRetryDelayMs(Integer retryDelayMs)
   {
      this.retryDelayMs = retryDelayMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for retryMultiplier
    *******************************************************************************/
   public Double getRetryMultiplier()
   {
      return (this.retryMultiplier);
   }



   /*******************************************************************************
    ** Setter for retryMultiplier
    *******************************************************************************/
   public void setRetryMultiplier(Double retryMultiplier)
   {
      this.retryMultiplier = retryMultiplier;
   }



   /*******************************************************************************
    ** Fluent setter for retryMultiplier
    *******************************************************************************/
   public EsbTrigger withRetryMultiplier(Double retryMultiplier)
   {
      this.retryMultiplier = retryMultiplier;
      return (this);
   }



   /*******************************************************************************
    ** Getter for retryMaxDelayMs
    *******************************************************************************/
   public Integer getRetryMaxDelayMs()
   {
      return (this.retryMaxDelayMs);
   }



   /*******************************************************************************
    ** Setter for retryMaxDelayMs
    *******************************************************************************/
   public void setRetryMaxDelayMs(Integer retryMaxDelayMs)
   {
      this.retryMaxDelayMs = retryMaxDelayMs;
   }



   /*******************************************************************************
    ** Fluent setter for retryMaxDelayMs
    *******************************************************************************/
   public EsbTrigger withRetryMaxDelayMs(Integer retryMaxDelayMs)
   {
      this.retryMaxDelayMs = retryMaxDelayMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for onDeadLetter
    *******************************************************************************/
   public EsbDeadLetterAction getOnDeadLetter()
   {
      return (this.onDeadLetter);
   }



   /*******************************************************************************
    ** Setter for onDeadLetter
    *******************************************************************************/
   public void setOnDeadLetter(EsbDeadLetterAction onDeadLetter)
   {
      this.onDeadLetter = onDeadLetter;
   }



   /*******************************************************************************
    ** Fluent setter for onDeadLetter
    *******************************************************************************/
   public EsbTrigger withOnDeadLetter(EsbDeadLetterAction onDeadLetter)
   {
      this.onDeadLetter = onDeadLetter;
      return (this);
   }



   /*******************************************************************************
    ** Getter for deadLetterDestinationName.  See also
    ** getEffectiveDeadLetterDestinationName.
    *******************************************************************************/
   public String getDeadLetterDestinationName()
   {
      return (this.deadLetterDestinationName);
   }



   /*******************************************************************************
    ** Setter for deadLetterDestinationName
    *******************************************************************************/
   public void setDeadLetterDestinationName(String deadLetterDestinationName)
   {
      this.deadLetterDestinationName = deadLetterDestinationName;
   }



   /*******************************************************************************
    ** Fluent setter for deadLetterDestinationName
    *******************************************************************************/
   public EsbTrigger withDeadLetterDestinationName(String deadLetterDestinationName)
   {
      this.deadLetterDestinationName = deadLetterDestinationName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for runAsSessionSupplier - a Supplier<QSession>; null means the
    ** system user session.
    *******************************************************************************/
   public QCodeReference getRunAsSessionSupplier()
   {
      return (this.runAsSessionSupplier);
   }



   /*******************************************************************************
    ** Setter for runAsSessionSupplier
    *******************************************************************************/
   public void setRunAsSessionSupplier(QCodeReference runAsSessionSupplier)
   {
      this.runAsSessionSupplier = runAsSessionSupplier;
   }



   /*******************************************************************************
    ** Fluent setter for runAsSessionSupplier
    *******************************************************************************/
   public EsbTrigger withRunAsSessionSupplier(QCodeReference runAsSessionSupplier)
   {
      this.runAsSessionSupplier = runAsSessionSupplier;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timeoutMs (per run; null means no timeout)
    *******************************************************************************/
   public Integer getTimeoutMs()
   {
      return (this.timeoutMs);
   }



   /*******************************************************************************
    ** Setter for timeoutMs
    *******************************************************************************/
   public void setTimeoutMs(Integer timeoutMs)
   {
      this.timeoutMs = timeoutMs;
   }



   /*******************************************************************************
    ** Fluent setter for timeoutMs
    *******************************************************************************/
   public EsbTrigger withTimeoutMs(Integer timeoutMs)
   {
      this.timeoutMs = timeoutMs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startPaused
    *******************************************************************************/
   public Boolean getStartPaused()
   {
      return (this.startPaused);
   }



   /*******************************************************************************
    ** Setter for startPaused
    *******************************************************************************/
   public void setStartPaused(Boolean startPaused)
   {
      this.startPaused = startPaused;
   }



   /*******************************************************************************
    ** Fluent setter for startPaused
    *******************************************************************************/
   public EsbTrigger withStartPaused(Boolean startPaused)
   {
      this.startPaused = startPaused;
      return (this);
   }

}
