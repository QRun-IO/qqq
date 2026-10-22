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

package com.kingsrook.qqq.esb.envelope;


/*******************************************************************************
 * The id of the ESB event that caused the work running on the current thread.
 *
 * A triggered run sets it to the triggering event's id (and clears it when
 * done); EsbEventFactory copies it into each event it makes, as the event's
 * causationId (the qqqcausationid CloudEvent attribute).  So a process that is
 * triggered by table X and updates table X publishes events that point back at
 * the event that triggered it, which is how a self-trigger loop can be spotted.
 *
 * It is a thread-local: work handed to another thread doesn't carry it.
 *******************************************************************************/
public class EsbCausation
{
   private static final ThreadLocal<String> CURRENT_EVENT_ID = new ThreadLocal<>();



   /*******************************************************************************
    ** Set the current thread's causing event id (null is the same as clear).
    *******************************************************************************/
   public static void set(String eventId)
   {
      if(eventId == null)
      {
         clear();
      }
      else
      {
         CURRENT_EVENT_ID.set(eventId);
      }
   }



   /*******************************************************************************
    ** The current thread's causing event id, or null.
    *******************************************************************************/
   public static String current()
   {
      return (CURRENT_EVENT_ID.get());
   }



   /*******************************************************************************
    ** Remove the current thread's causing event id.
    *******************************************************************************/
   public static void clear()
   {
      CURRENT_EVENT_ID.remove();
   }

}
