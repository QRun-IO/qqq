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

package com.kingsrook.qqq.backend.core.actions.scripts.logging;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for StoreScriptLogAndScriptLogLineExecutionLogger
 *******************************************************************************/
class StoreScriptLogAndScriptLogLineExecutionLoggerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QInstance instance = QContext.getQInstance();
      new ScriptsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      ExecuteCodeInput executeCodeInput = new ExecuteCodeInput();
      executeCodeInput.setInput(Map.of("a", 1));

      StoreScriptLogAndScriptLogLineExecutionLogger logger = new StoreScriptLogAndScriptLogLineExecutionLogger(9999, 8888);
      logger.acceptExecutionStart(executeCodeInput);
      logger.acceptLogLine("This is a log");
      logger.acceptLogLine("This is also a log");
      logger.acceptExecutionEnd(true);

      List<QRecord> scriptLogRecords = TestUtils.queryTable(instance, "scriptLog");
      assertEquals(1, scriptLogRecords.size());
      QRecord scriptLog = scriptLogRecords.get(0);
      assertNotNull(scriptLog.getValueInteger("id"));
      assertNotNull(scriptLog.getValue("startTimestamp"));
      assertNotNull(scriptLog.getValue("endTimestamp"));
      assertNotNull(scriptLog.getValue("runTimeMillis"));
      assertEquals(9999, scriptLog.getValueInteger("scriptId"));
      assertEquals(8888, scriptLog.getValueInteger("scriptRevisionId"));
      assertEquals("{a=1}", scriptLog.getValueString("input"));
      assertEquals("true", scriptLog.getValueString("output"));
      assertNull(scriptLog.getValueString("exception"));
      assertFalse(scriptLog.getValueBoolean("hadError"));

      List<QRecord> scriptLogLineRecords = TestUtils.queryTable(instance, "scriptLogLine");
      assertEquals(2, scriptLogLineRecords.size());
      QRecord scriptLogLine = scriptLogLineRecords.get(0);
      assertEquals(scriptLog.getValueInteger("id"), scriptLogLine.getValueInteger("scriptLogId"));
      assertNotNull(scriptLogLine.getValue("timestamp"));
      assertEquals("This is a log", scriptLogLine.getValueString("text"));
      scriptLogLine = scriptLogLineRecords.get(1);
      assertEquals("This is also a log", scriptLogLine.getValueString("text"));
   }
}