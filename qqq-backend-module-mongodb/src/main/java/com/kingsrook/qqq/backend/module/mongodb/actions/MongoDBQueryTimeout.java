/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ActionTimeoutHelper;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import org.bson.BsonArray;
import org.bson.BsonDocument;


/*******************************************************************************
 ** Cancel only this query's session until first-row delivery or action completion.
 ** A separate client avoids waiting for the query's checked-out connection.
 ******************************************************************************/
class MongoDBQueryTimeout implements Runnable, AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(MongoDBQueryTimeout.class);

   private final Integer timeoutSeconds;
   private final BsonDocument sessionId;
   private final Supplier<MongoClientContainer> openControlClient;
   private final ActionTimeoutHelper timeoutHelper;
   private final CountDownLatch cancellationFinished = new CountDownLatch(1);
   private boolean finished;
   private boolean running;
   private volatile boolean timedOut;



   /*******************************************************************************
    ** Capture the target session on the action thread, before scheduling work.
    ******************************************************************************/
   MongoDBQueryTimeout(Integer timeoutSeconds, MongoClientContainer client, Supplier<MongoClientContainer> openControlClient)
   {
      this.timeoutSeconds = timeoutSeconds;
      this.sessionId = client.getMongoSession().getServerSession().getIdentifier().clone();
      this.openControlClient = openControlClient;
      this.timeoutHelper = new ActionTimeoutHelper(timeoutSeconds, TimeUnit.SECONDS, this);
   }



   /*******************************************************************************
    ** Keep the existing positive-only timeout and its original starting point.
    ******************************************************************************/
   void start()
   {
      timeoutHelper.start();
   }



   /*******************************************************************************
    ** Do not dispatch or return a successful result after this timeout won.
    ******************************************************************************/
   void throwIfTimedOut() throws QUserFacingException
   {
      if(timedOut)
      {
         throw new QUserFacingException("Query timed out.");
      }
   }



   /*******************************************************************************
    ** Report whether cancellation won the race with completion.
    ******************************************************************************/
   boolean hasTimedOut()
   {
      return timedOut;
   }



   /*******************************************************************************
    ** A kill can race with initial command dispatch. Keep targeting this session
    ** until the action acknowledges completion; never target a reused session.
    ******************************************************************************/
   @Override
   public void run()
   {
      synchronized(this)
      {
         if(finished)
         {
            return;
         }
         running = true;
         timedOut = true;
      }

      MongoClientContainer controlClient = null;
      try
      {
         synchronized(this)
         {
            while(!finished)
            {
               if(controlClient == null)
               {
                  controlClient = openControlClient.get();
               }
               controlClient.getMongoClient().getDatabase("admin").withTimeout(timeoutSeconds, TimeUnit.SECONDS)
                  .runCommand(new BsonDocument("killSessions", new BsonArray(List.of(sessionId))));
               wait(25);
            }
         }
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
      }
      catch(Exception e)
      {
         LOG.warn("Unable to cancel the timed out MongoDB query session", e);
      }
      finally
      {
         try
         {
            if(controlClient != null)
            {
               controlClient.closeIfNeeded();
            }
         }
         finally
         {
            cancellationFinished.countDown();
         }
      }
   }



   /*******************************************************************************
    ** Disarm and drain cancellation before closing or reusing the target session.
    ** Preserve caller interruption while waiting for owned control resources.
    ******************************************************************************/
   @Override
   public void close()
   {
      boolean awaitCancellation;
      synchronized(this)
      {
         finished = true;
         notifyAll();
         awaitCancellation = running;
      }

      boolean interrupted = false;
      if(awaitCancellation)
      {
         while(true)
         {
            try
            {
               cancellationFinished.await();
               break;
            }
            catch(InterruptedException e)
            {
               interrupted = true;
            }
         }
      }
      timeoutHelper.cancel();
      if(interrupted)
      {
         Thread.currentThread().interrupt();
      }
   }
}
