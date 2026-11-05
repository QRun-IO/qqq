/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.context;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidationKey;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QContext
 *******************************************************************************/
class QContextTest
{

   /*******************************************************************************
    ** A second caller must wait for enrichment; recursion on the owner is allowed.
    *******************************************************************************/
   @Test
   void concurrentInitializationWaitsForValidation() throws Exception
   {
      var entered = new CountDownLatch(1);
      var release = new CountDownLatch(1);
      QInstance instance = newQInstance(new QInstance()
      {
         /***************************************************************************
          ** Pause validation after entering RUNNING, including a recursive init.
          ***************************************************************************/
         @Override
         public void setValidationIsRunning(QInstanceValidationKey key)
         {
            super.setValidationIsRunning(key);
            QContext.init(this, new QSession());
            entered.countDown();
            try
            {
               if(!release.await(5, TimeUnit.SECONDS))
               {
                  throw (new AssertionError("Validation was not released"));
               }
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               throw (new AssertionError(e));
            }
         }
      });
      try(var executor = Executors.newFixedThreadPool(2))
      {
         var owner = executor.submit(() -> QContext.init(instance, new QSession()));
         try
         {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var started = new CountDownLatch(1);
            var follower = executor.submit(() ->
            {
               started.countDown();
               QContext.init(instance, new QSession());
               return instance.getHasBeenValidated();
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
               () -> follower.get(200, TimeUnit.MILLISECONDS));
            release.countDown();
            owner.get(5, TimeUnit.SECONDS);
            assertTrue(follower.get(5, TimeUnit.SECONDS));
         }
         finally
         {
            release.countDown();
         }
      }
   }



   /*******************************************************************************
    ** A failed validation must not leave RUNNING authority for later callers.
    *******************************************************************************/
   @Test
   void failedValidationCannotBeSkipped()
   {
      QInstance instance = newQInstance();
      instance.getTable("table").setBackendName("missing");
      try
      {
         assertThrows(IllegalArgumentException.class, () -> QContext.init(instance, new QSession()));
         assertThrows(IllegalArgumentException.class, () -> QContext.init(instance, new QSession()));
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      QContext.clear();

      QInstance qInstance    = newQInstance();
      String    instanceUuid = UUID.randomUUID().toString();
      qInstance.setEnvironmentValues(Map.of("uuid", instanceUuid));

      QSession qSession    = new QSession();
      String   sessionUuid = qSession.getUuid();

      //////////////////////////////////////////////////////
      // init the context - make sure the uuids come back //
      //////////////////////////////////////////////////////
      QContext.init(qInstance, qSession);
      assertEquals(instanceUuid, QContext.getQInstance().getEnvironmentValues().get("uuid"));
      assertEquals(sessionUuid, QContext.getQSession().getUuid());

      //////////////////////////////////////////////////////////
      // capture the context - assert we got values we expect //
      //////////////////////////////////////////////////////////
      CapturedContext capturedContext = QContext.capture();
      assertEquals(instanceUuid, capturedContext.qInstance().getEnvironmentValues().get("uuid"));
      assertEquals(sessionUuid, capturedContext.qSession().getUuid());

      ///////////////////////////////////////
      // clear context - assert we're null //
      ///////////////////////////////////////
      QContext.clear();
      assertNull(QContext.getQInstance());
      assertNull(QContext.getQSession());

      //////////////////////////////////////////////////////////////////////////
      // init context with new values - make sure uuids aren't what we expect //
      //////////////////////////////////////////////////////////////////////////
      QContext.init(newQInstance(), new QSession());
      assertNotEquals(instanceUuid, QContext.getQInstance().getEnvironmentValues().get("uuid"));
      assertNotEquals(sessionUuid, QContext.getQSession().getUuid());

      /////////////////////////////////////////////////////////////////////
      // re-init to the captured context - make sure values are expected //
      /////////////////////////////////////////////////////////////////////
      QContext.init(capturedContext);
      assertEquals(instanceUuid, QContext.getQInstance().getEnvironmentValues().get("uuid"));
      assertEquals(sessionUuid, QContext.getQSession().getUuid());

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QInstance newQInstance()
   {
      return (newQInstance(new QInstance()));
   }



   /*******************************************************************************
    ** Populate an instance with the minimal valid application metadata.
    *******************************************************************************/
   private static QInstance newQInstance(QInstance qInstance)
   {
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withType(QAuthenticationType.FULLY_ANONYMOUS).withName("anonymous"));
      qInstance.addBackend(new QBackendMetaData().withName("backend"));
      qInstance.addTable(new QTableMetaData().withName("table").withBackendName("backend").withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      return qInstance;
   }

}
