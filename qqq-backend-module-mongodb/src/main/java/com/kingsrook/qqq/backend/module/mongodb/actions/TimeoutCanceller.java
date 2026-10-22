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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;


/*******************************************************************************
 ** Helper to cancel statements that timeout.
 *******************************************************************************/
public class TimeoutCanceller implements Runnable
{
   private static final QLogger              LOG = QLogger.getLogger(TimeoutCanceller.class);
   private final        MongoClientContainer mongoClientContainer;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TimeoutCanceller(MongoClientContainer mongoClientContainer)
   {
      this.mongoClientContainer = mongoClientContainer;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run()
   {
      try
      {
         mongoClientContainer.closeIfNeeded();
         LOG.info("Cancelled timed out query");
      }
      catch(Exception e)
      {
         LOG.warn("Error trying to cancel statement after timeout", e);
      }

      throw (new QRuntimeException("Statement timed out and was cancelled."));
   }
}
