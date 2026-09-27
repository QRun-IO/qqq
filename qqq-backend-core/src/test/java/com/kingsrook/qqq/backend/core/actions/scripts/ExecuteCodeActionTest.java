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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.Log4jCodeExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.NoopCodeExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.QCodeExecutionLoggerInterface;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.StoreScriptLogAndScriptLogLineExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.SystemOutExecutionLogger;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QCodeException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for ExecuteCodeAction
 *******************************************************************************/
class ExecuteCodeActionTest extends BaseTest
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
      QInstance         qInstance         = QContext.getQInstance();
      ExecuteCodeInput  executeCodeInput  = setupInput(qInstance, Map.of("x", 4), new NoopCodeExecutionLogger());
      ExecuteCodeOutput executeCodeOutput = new ExecuteCodeOutput();
      new ExecuteCodeAction().run(executeCodeInput, executeCodeOutput);
      assertEquals(16, executeCodeOutput.getOutput());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ExecuteCodeInput setupInput(QInstance qInstance, Map<String, Serializable> context, QCodeExecutionLoggerInterface executionLogger)
   {
      ExecuteCodeInput executeCodeInput = new ExecuteCodeInput();
      executeCodeInput.setCodeReference(new QCodeReference(ScriptInJava.class));
      executeCodeInput.setContext(context);
      executeCodeInput.setExecutionLogger(executionLogger);
      return executeCodeInput;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLog4jLogger() throws QException
   {
      QInstance         qInstance         = QContext.getQInstance();
      ExecuteCodeInput  executeCodeInput  = setupInput(qInstance, Map.of("x", 4), new Log4jCodeExecutionLogger());
      ExecuteCodeOutput executeCodeOutput = new ExecuteCodeOutput();
      new ExecuteCodeAction().run(executeCodeInput, executeCodeOutput);
      assertEquals(16, executeCodeOutput.getOutput());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSystemOutLogger() throws QException
   {
      QInstance         qInstance         = QContext.getQInstance();
      ExecuteCodeInput  executeCodeInput  = setupInput(qInstance, Map.of("x", 4), new SystemOutExecutionLogger());
      ExecuteCodeOutput executeCodeOutput = new ExecuteCodeOutput();
      new ExecuteCodeAction().run(executeCodeInput, executeCodeOutput);
      assertEquals(16, executeCodeOutput.getOutput());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableLogger() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      new ScriptsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      ExecuteCodeInput  executeCodeInput  = setupInput(qInstance, Map.of("x", 4), new StoreScriptLogAndScriptLogLineExecutionLogger(1701, 1702));
      ExecuteCodeOutput executeCodeOutput = new ExecuteCodeOutput();
      new ExecuteCodeAction().run(executeCodeInput, executeCodeOutput);
      assertEquals(16, executeCodeOutput.getOutput());

      List<QRecord> scriptLogRecords     = TestUtils.queryTable(qInstance, "scriptLog");
      List<QRecord> scriptLogLineRecords = TestUtils.queryTable(qInstance, "scriptLogLine");
      assertEquals(1, scriptLogRecords.size());
      assertEquals(1701, scriptLogRecords.get(0).getValueInteger("scriptId"));
      assertEquals(1702, scriptLogRecords.get(0).getValueInteger("scriptRevisionId"));
      assertEquals(1, scriptLogLineRecords.size());
      assertEquals(1, scriptLogLineRecords.get(0).getValueInteger("scriptLogId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testException()
   {
      QInstance         qInstance         = QContext.getQInstance();
      ExecuteCodeInput  executeCodeInput  = setupInput(qInstance, Map.of(), new NoopCodeExecutionLogger());
      ExecuteCodeOutput executeCodeOutput = new ExecuteCodeOutput();
      assertThrows(QCodeException.class, () ->
      {
         new ExecuteCodeAction().run(executeCodeInput, executeCodeOutput);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ScriptInJava implements Function<Map<String, Object>, Serializable>
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Serializable apply(Map<String, Object> context)
      {
         ((QCodeExecutionLoggerInterface) context.get("logger")).log("Test a log");

         int x = ValueUtils.getValueAsInteger(context.get("x"));
         return (x * x);
      }

   }
}