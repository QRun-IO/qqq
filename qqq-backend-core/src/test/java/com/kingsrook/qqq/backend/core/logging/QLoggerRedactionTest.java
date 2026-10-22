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

package com.kingsrook.qqq.backend.core.logging;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Assert real log events, including formatted parameters and string log pairs.
 *******************************************************************************/
class QLoggerRedactionTest extends BaseTest
{
   /*******************************************************************************
    ** Redactors and record masks reach appender.
    *******************************************************************************/
   @Test
   void redactorsAndRecordMasksReachAppender()
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("firstName").withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI));
      QRecord record = new QRecord().withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY).withValue("firstName", "private-name").withValue("lastName", "public-name");
      QLoggerTest.ListAppender appender = QLoggerTest.ListAppender.createAppender();
      org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(QLoggerRedactionTest.class);
      LogRedactorInterface redactor = message -> message.replace("pattern-secret", "[redacted]");
      QLogger.registerRedactor(redactor);
      logger.addAppender(appender);
      appender.start();
      try
      {
         QLogger log = QLogger.getLogger(QLoggerRedactionTest.class);
         log.info("pattern-secret", logPair("value", "pattern-secret"), logPair("record", record));
         log.info("{}", (Object) "pattern-secret");
         assertFalse(appender.getEventList().isEmpty());
         String captured = appender.getEventList().stream().map(e -> e.getMessage().getFormattedMessage()).reduce("", String::concat);
         assertFalse(captured.contains("private-name"));
         assertFalse(captured.contains("pattern-secret"));
         assertTrue(captured.contains("public-name"));
         assertTrue(captured.contains("***"));
         assertTrue(captured.contains("[redacted]"));
      }
      finally
      {
         QLogger.unregisterRedactor(redactor);
         logger.removeAppender(appender);
         appender.stop();
      }
   }
}
