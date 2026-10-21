/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.locks;


import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;


/*******************************************************************************
 ** Lock thrown by ProcessLockUtils when you can't get the lock.
 *******************************************************************************/
public class UnableToObtainProcessLockException extends QUserFacingException
{
   private ProcessLock existingLock;



   /*******************************************************************************
    **
    *******************************************************************************/
   public UnableToObtainProcessLockException(String message)
   {
      super(message);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public UnableToObtainProcessLockException(String message, Throwable cause)
   {
      super(message, cause);
   }



   /*******************************************************************************
    ** Getter for existingLock
    *******************************************************************************/
   public ProcessLock getExistingLock()
   {
      return (this.existingLock);
   }



   /*******************************************************************************
    ** Setter for existingLock
    *******************************************************************************/
   public void setExistingLock(ProcessLock existingLock)
   {
      this.existingLock = existingLock;
   }



   /*******************************************************************************
    ** Fluent setter for existingLock
    *******************************************************************************/
   public UnableToObtainProcessLockException withExistingLock(ProcessLock existingLock)
   {
      this.existingLock = existingLock;
      return (this);
   }

}
