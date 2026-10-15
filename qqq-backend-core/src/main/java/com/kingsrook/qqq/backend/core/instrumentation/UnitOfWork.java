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

package com.kingsrook.qqq.backend.core.instrumentation;


import java.util.concurrent.atomic.AtomicLong;


/*******************************************************************************
 ** One unit of work on one thread (an HTTP request, a scheduled job, or a
 ** process run outside both), opened and closed by core through
 ** {@link UnitsOfWork}. Statements run while it is open are attributed to it.
 **
 ** The name may be refined while the unit is open (for example an HTTP
 ** request learns its route template only after routing), so listeners that
 ** need the final name read it in onUnitOfWorkEnd.
 *******************************************************************************/
public final class UnitOfWork
{
   private static final AtomicLong NEXT_ID = new AtomicLong(1);

   private final long           id;
   private final UnitOfWorkKind kind;
   private final long           startNanos;
   private volatile String      name;



   /*******************************************************************************
    ** A new unit with a unique id.
    *******************************************************************************/
   UnitOfWork(UnitOfWorkKind kind, String name)
   {
      this.id = NEXT_ID.getAndIncrement();
      this.kind = kind == null ? UnitOfWorkKind.OTHER : kind;
      this.name = name;
      this.startNanos = System.nanoTime();
   }



   /*******************************************************************************
    ** JVM-unique id of this unit.
    *******************************************************************************/
   public long getId()
   {
      return (id);
   }



   /*******************************************************************************
    ** What started this unit.
    *******************************************************************************/
   public UnitOfWorkKind getKind()
   {
      return (kind);
   }



   /*******************************************************************************
    ** The unit's name: route template, job or process name.
    *******************************************************************************/
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Refine the name (e.g. once a request's route template is known).
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** System.nanoTime() when the unit began.
    *******************************************************************************/
   public long getStartNanos()
   {
      return (startNanos);
   }



   /*******************************************************************************
    ** Kind and name, for logs.
    *******************************************************************************/
   @Override
   public String toString()
   {
      return (kind + " " + name + " #" + id);
   }
}
