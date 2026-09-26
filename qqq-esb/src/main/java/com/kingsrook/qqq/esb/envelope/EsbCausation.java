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
