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


package com.kingsrook.qqq.backend.core.jobs.plans;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Thrown from a cooperative check to stop a chunk at a safe point: the job was
 ** paused or cancelled, this pod is draining, or the lease was lost.
 *******************************************************************************/
public class JobInterruptedException extends QException
{
   /*******************************************************************************
    ** Why the chunk stopped.
    *******************************************************************************/
   public enum Reason
   {
      PAUSE,
      CANCEL,
      DRAIN,
      LEASE_LOST
   }



   private final Reason reason;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public JobInterruptedException(Reason reason)
   {
      super("Job chunk stopped: " + reason);
      this.reason = reason;
   }



   /*******************************************************************************
    ** Getter for reason
    *******************************************************************************/
   public Reason getReason()
   {
      return (this.reason);
   }



   /*******************************************************************************
    ** Find a JobInterruptedException in a cause chain, or null.
    *******************************************************************************/
   public static JobInterruptedException find(Throwable t)
   {
      int depth = 0;
      for(Throwable cause = t; cause != null && depth < 50; cause = cause.getCause(), depth++)
      {
         if(cause instanceof JobInterruptedException jie)
         {
            return (jie);
         }
      }
      return (null);
   }

}
