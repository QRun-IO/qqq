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

package com.kingsrook.qqq.middleware.javalin;


import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;


/*******************************************************************************
 ** base class for javalin implementation tests.
 *******************************************************************************/
public class QJavalinTestBase
{
   private static final   int    PORT     = 6262;
   protected static final String BASE_URL = "http://localhost:" + PORT;

   protected static QJavalinImplementation qJavalinImplementation;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.fullReset();
   }



   /*******************************************************************************
    ** Before the class (all) runs, start a javalin server.
    **
    *******************************************************************************/
   @BeforeAll
   public static void beforeAll() throws QInstanceValidationException
   {
      qJavalinImplementation = new QJavalinImplementation(TestUtils.defineInstance());
      QJavalinProcessHandler.setAsyncStepTimeoutMillis(250);
      qJavalinImplementation.startJavalinServer(PORT);
   }



   /*******************************************************************************
    ** Before the class (all) runs, start a javalin server.
    **
    *******************************************************************************/
   @AfterAll
   public static void afterAll()
   {
      qJavalinImplementation.stopJavalinServer();
   }



   /*******************************************************************************
    ** Fully rebuild the test-database before each test runs, for completely known state.
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      TestUtils.primeTestDatabase();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected static void restartServerWithInstance(QInstance qInstance) throws QInstanceValidationException
   {
      if(qJavalinImplementation != null)
      {
         qJavalinImplementation.stopJavalinServer();
      }
      qJavalinImplementation = new QJavalinImplementation(qInstance, new QJavalinMetaData());
      QJavalinProcessHandler.setAsyncStepTimeoutMillis(250);
      qJavalinImplementation.startJavalinServer(PORT);
   }

}
