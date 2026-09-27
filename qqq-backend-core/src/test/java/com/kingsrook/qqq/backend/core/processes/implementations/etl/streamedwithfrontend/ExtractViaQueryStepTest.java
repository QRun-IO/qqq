/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend;


import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallback;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ExtractViaQueryStep 
 *******************************************************************************/
class ExtractViaQueryStepTest extends BaseTest
{


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCustomizeInputPreQuery() throws QException
   {
      AtomicBoolean called = new AtomicBoolean(false);

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, TestUtils.TABLE_NAME_PERSON_MEMORY);
      input.addValue(StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER, "{}");
      input.setCallback(new QProcessCallback()
      {
         /***************************************************************************
          *
          ***************************************************************************/
         @Override
         public void customizeInputPreQuery(RunBackendStepInput runBackendStepInput, QueryInput queryInput)
         {
            called.set(true);
         }
      });

      RunBackendStepOutput output = new RunBackendStepOutput();
      ExtractViaQueryStep  extractViaQueryStep = new ExtractViaQueryStep();
      extractViaQueryStep.preRun(input, output);
      extractViaQueryStep.run(input, output);

      assertTrue(called.get());
   }
}