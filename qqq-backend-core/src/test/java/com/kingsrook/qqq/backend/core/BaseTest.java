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

package com.kingsrook.qqq.backend.core;


import java.time.ZoneId;
import java.util.TimeZone;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


/*******************************************************************************
 **
 *******************************************************************************/
public class BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(BaseTest.class);

   public static final String DEFAULT_USER_ID = "001";

   static
   {
      TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void baseBeforeEach()
   {
      System.setProperty("qqq.logger.logSessionId.disabled", "true");

      QContext.init(TestUtils.defineInstance(), newSession());
      resetMemoryRecordStore();

      ExamplePersonalizer.reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QSession newSession()
   {
      return newSession(DEFAULT_USER_ID);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QSession newSession(String userId)
   {
      return new QSession().withUser(new QUser()
         .withIdReference(userId)
         .withFullName("Anonymous"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void baseAfterEach()
   {
      QContext.clear();
      resetMemoryRecordStore();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void resetMemoryRecordStore()
   {
      MemoryRecordStore.getInstance().reset();
      MemoryRecordStore.resetStatistics();
      MemoryRecordStore.setCollectStatistics(false);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected static void reInitInstanceInContext(QInstance qInstance)
   {
      if(qInstance.equals(QContext.getQInstance()))
      {
         LOG.warn("Unexpected condition - the same qInstance that is already in the QContext was passed into reInit.  You probably want a new QInstance object instance.");
      }
      QContext.init(qInstance, new QSession());
   }

}
