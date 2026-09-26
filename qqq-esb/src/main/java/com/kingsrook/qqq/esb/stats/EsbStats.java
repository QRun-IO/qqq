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

package com.kingsrook.qqq.esb.stats;


import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 * In-memory ESB counters for this node (spec section 7): nothing is stored, and
 * counts start over when the app restarts.
 *
 * Destinations are counted by their QQQ destination name, and triggers by their
 * trigger name (processName.destinationName), in separate maps.  Counting never
 * throws: a null name is ignored, so observability can't break a publish or a
 * run.  Safe to call from any thread.
 *******************************************************************************/
public class EsbStats
{
   private static final EsbStats INSTANCE = new EsbStats();

   private final Map<String, Counter> destinationCounters = new ConcurrentHashMap<>();
   private final Map<String, Counter> triggerCounters     = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** Singleton: use getInstance.
    *******************************************************************************/
   private EsbStats()
   {
   }



   /*******************************************************************************
    ** The singleton.
    *******************************************************************************/
   public static EsbStats getInstance()
   {
      return (INSTANCE);
   }



   /*******************************************************************************
    ** A message was sent to the destination.
    *******************************************************************************/
   public void published(String destinationName)
   {
      count(destinationCounters, destinationName, Counter::published);
   }



   /*******************************************************************************
    ** A message could not be sent to the destination.
    *******************************************************************************/
   public void publishFailed(String destinationName, Exception exception)
   {
      count(destinationCounters, destinationName, counter -> counter.publishFailed(getErrorText(exception)));
   }



   /*******************************************************************************
    ** The trigger received a message.
    *******************************************************************************/
   public void consumed(String triggerName)
   {
      count(triggerCounters, triggerName, Counter::consumed);
   }



   /*******************************************************************************
    ** The trigger's run succeeded, taking ms milliseconds.
    *******************************************************************************/
   public void succeeded(String triggerName, long ms)
   {
      count(triggerCounters, triggerName, counter -> counter.succeeded(ms));
   }



   /*******************************************************************************
    ** The trigger's run failed.
    *******************************************************************************/
   public void failed(String triggerName, Exception exception)
   {
      count(triggerCounters, triggerName, counter -> counter.failed(getErrorText(exception)));
   }



   /*******************************************************************************
    ** The trigger's failed message will be redelivered for another attempt.
    *******************************************************************************/
   public void retried(String triggerName)
   {
      count(triggerCounters, triggerName, Counter::retried);
   }



   /*******************************************************************************
    ** The trigger's message used its last attempt, and was dead-lettered (or
    ** discarded).
    *******************************************************************************/
   public void deadLettered(String triggerName)
   {
      count(triggerCounters, triggerName, Counter::deadLettered);
   }



   /*******************************************************************************
    ** Change the number of messages the trigger is processing right now (+1 when
    ** a run starts, -1 when it ends).
    *******************************************************************************/
   public void inFlight(String triggerName, int delta)
   {
      count(triggerCounters, triggerName, counter -> counter.inFlight(delta));
   }



   /*******************************************************************************
    ** A snapshot of the destination's counters (all zero if it has none).
    *******************************************************************************/
   public EsbCounterSnapshot destination(String destinationName)
   {
      return (snapshot(destinationCounters, destinationName));
   }



   /*******************************************************************************
    ** A snapshot of the trigger's counters (all zero if it has none).
    *******************************************************************************/
   public EsbCounterSnapshot trigger(String triggerName)
   {
      return (snapshot(triggerCounters, triggerName));
   }



   /*******************************************************************************
    ** Clear all counters (used by tests).
    *******************************************************************************/
   public void reset()
   {
      destinationCounters.clear();
      triggerCounters.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void count(Map<String, Counter> counters, String name, Consumer<Counter> update)
   {
      if(name != null)
      {
         update.accept(counters.computeIfAbsent(name, n -> new Counter()));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static EsbCounterSnapshot snapshot(Map<String, Counter> counters, String name)
   {
      Counter counter = (name == null) ? null : counters.get(name);
      return (counter == null ? EsbCounterSnapshot.EMPTY : counter.snapshot());
   }



   /*******************************************************************************
    ** The exception's message, or its class name if it has none (null for a
    ** null exception, which leaves lastError as it was).
    *******************************************************************************/
   private static String getErrorText(Exception exception)
   {
      if(exception == null)
      {
         return (null);
      }

      return (StringUtils.hasContent(exception.getMessage()) ? exception.getMessage() : exception.getClass().getSimpleName());
   }



   /*******************************************************************************
    * One destination's or trigger's counters.  Each method holds the counter's
    * lock, so a snapshot is consistent; the work inside is a few field updates.
    * Primitive fields, since they are only counted, never null.
    *******************************************************************************/
   private static final class Counter
   {
      private long    published;
      private long    publishFailures;
      private long    consumed;
      private long    succeeded;
      private long    failed;
      private long    retried;
      private long    deadLettered;
      private int     inFlight;
      private long    totalSucceededMs;
      private long    maxMs;
      private Instant lastActivity;
      private String  lastError;



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized void published()
      {
         published++;
         touch();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized void publishFailed(String errorText)
      {
         publishFailures++;
         recordError(errorText);
         touch();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized void consumed()
      {
         consumed++;
         touch();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized void succeeded(long ms)
      {
         succeeded++;
         totalSucceededMs += ms;
         maxMs = Math.max(maxMs, ms);
         touch();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized void failed(String errorText)
      {
         failed++;
         recordError(errorText);
         touch();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized void retried()
      {
         retried++;
         touch();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized void deadLettered()
      {
         deadLettered++;
         touch();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized void inFlight(int delta)
      {
         inFlight += delta;
         touch();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      synchronized EsbCounterSnapshot snapshot()
      {
         long avgMs = (succeeded == 0) ? 0 : Math.round((double) totalSucceededMs / succeeded);
         return (new EsbCounterSnapshot(published, publishFailures, consumed, succeeded, failed, retried, deadLettered, inFlight, lastActivity, avgMs, maxMs, lastError));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private void recordError(String errorText)
      {
         if(errorText != null)
         {
            lastError = errorText;
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private void touch()
      {
         lastActivity = Instant.now();
      }
   }

}
