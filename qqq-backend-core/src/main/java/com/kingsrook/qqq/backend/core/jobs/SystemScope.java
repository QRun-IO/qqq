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


package com.kingsrook.qqq.backend.core.jobs;


import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;


/*******************************************************************************
 ** Runs kernel bookkeeping (the jobs tables themselves) as the system user, so
 ** the owner lock does not hide rows from the kernel. Never used to run a job's
 ** own work: that runs as the job's user.
 *******************************************************************************/
public final class SystemScope
{

   /*******************************************************************************
    ** A body that returns a value and may throw.
    *******************************************************************************/
   @FunctionalInterface
   public interface Body<T>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      T run() throws QException;
   }



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private SystemScope()
   {
   }



   /*******************************************************************************
    ** Run the body under a system session in the current instance, keeping the
    ** current transaction, then restore the caller's context.
    *******************************************************************************/
   public static <T> T call(Body<T> body) throws QException
   {
      CapturedContext original = QContext.capture();
      try
      {
         QContext.init(new CapturedContext(QContext.getQInstance(), new QSystemUserSession(), QContext.getQBackendTransaction(), null));
         return (body.run());
      }
      finally
      {
         QContext.init(original);
      }
   }

}
