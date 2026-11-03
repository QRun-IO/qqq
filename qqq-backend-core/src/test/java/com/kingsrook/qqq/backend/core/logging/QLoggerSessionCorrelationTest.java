/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.logging;


import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Session context preserves correlation without exporting the login credential.
 *******************************************************************************/
class QLoggerSessionCorrelationTest extends BaseTest
{
   /*******************************************************************************
    ** Exercise the real DEBUG sink with same/different UUIDs and null contexts.
    *******************************************************************************/
   @Test
   void sessionContextUsesStableNonCredentialCorrelation()
   {
      String firstUuid = UUID.randomUUID().toString();
      String secondUuid = UUID.randomUUID().toString();
      QLoggerTest.ListAppender appender = QLoggerTest.ListAppender.createAppender();
      org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(QLoggerSessionCorrelationTest.class);
      Level previous = logger.getLevel();
      logger.setLevel(Level.DEBUG);
      appender.start();
      logger.addAppender(appender);
      try
      {
         QLogger log = QLogger.getLogger(QLoggerSessionCorrelationTest.class);
         for(String uuid : new String[] {firstUuid, firstUuid, secondUuid, null})
         {
            QSession session = new QSession();
            session.setUuid(uuid);
            QContext.setQSession(session);
            log.debug("session-correlation-event");
         }
         QContext.setQSession(null);
         log.debug("session-correlation-event");
         assertEquals(5, appender.getEventList().size());
         for(var event : appender.getEventList())
         {
            String captured = event.getMessage().getFormattedMessage() + event.getContextData().toMap();
            assertEquals(Level.DEBUG, event.getLevel());
            assertTrue(captured.contains("session-correlation-event"));
            assertFalse(captured.contains(firstUuid));
            assertFalse(captured.contains(secondUuid));
         }
         String first = correlation(appender, 0);
         assertTrue(first.matches("sha256:[0-9a-f]{64}"));
         assertEquals(first, correlation(appender, 1));
         assertNotEquals(first, correlation(appender, 2));
         assertEquals("unknown", correlation(appender, 3));
         assertEquals("unknown", correlation(appender, 4));
      }
      finally
      {
         logger.removeAppender(appender);
         logger.setLevel(previous);
         appender.stop();
      }
   }



   /*******************************************************************************
    ** Inspect the emitted field, rather than retesting the hash implementation.
    *******************************************************************************/
   private String correlation(QLoggerTest.ListAppender appender, int index)
   {
      return (new JSONObject(appender.getEventList().get(index).getMessage().getFormattedMessage())
         .getJSONObject("session").getString("id"));
   }
}
