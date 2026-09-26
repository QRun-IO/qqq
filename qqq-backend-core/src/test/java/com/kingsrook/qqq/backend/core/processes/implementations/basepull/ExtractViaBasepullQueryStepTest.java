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

package com.kingsrook.qqq.backend.core.processes.implementations.basepull;


import java.time.Instant;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ExtractViaBasepullQueryStep
 *******************************************************************************/
class ExtractViaBasepullQueryStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      Instant timestamp = Instant.parse("1980-05-31T15:36:00Z");
      Instant now       = Instant.now();

      RunBackendStepInput input = new RunBackendStepInput();
      input.addValue(RunProcessAction.BASEPULL_TIMESTAMP_FIELD, "createDate");
      input.addValue(RunProcessAction.BASEPULL_THIS_RUNTIME_KEY, now);
      input.setBasepullLastRunTime(timestamp);
      QQueryFilter queryFilter = new ExtractViaBasepullQueryStep().getQueryFilter(input);

      System.out.println(queryFilter);

      assertEquals(2, queryFilter.getCriteria().size());
      assertEquals("createDate", queryFilter.getCriteria().get(0).getFieldName());
      assertEquals(QCriteriaOperator.GREATER_THAN, queryFilter.getCriteria().get(0).getOperator());
      assertEquals(timestamp.toString(), queryFilter.getCriteria().get(0).getValues().get(0));

      assertEquals("createDate", queryFilter.getCriteria().get(1).getFieldName());
      assertEquals(QCriteriaOperator.LESS_THAN_OR_EQUALS, queryFilter.getCriteria().get(1).getOperator());
      assertEquals(now.toString(), queryFilter.getCriteria().get(1).getValues().get(0));

      assertEquals(1, queryFilter.getOrderBys().size());
      assertEquals("createDate", queryFilter.getOrderBys().get(0).getFieldName());
      assertTrue(queryFilter.getOrderBys().get(0).getIsAscending());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWillTheBasePullQueryBeUsed()
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // only time the base-pull query will be used is if there isn't a filter or records in the process input. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertTrue(new ExtractViaBasepullQueryStep().willTheBasePullQueryBeUsed(new RunBackendStepInput()));

      assertFalse(new ExtractViaBasepullQueryStep().willTheBasePullQueryBeUsed(new RunBackendStepInput()
         .withValues(Map.of("recordIds", "1,2,3", StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, "person"))));

      assertFalse(new ExtractViaBasepullQueryStep().willTheBasePullQueryBeUsed(new RunBackendStepInput()
         .withValues(Map.of(StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER, new QQueryFilter()))));

      assertFalse(new ExtractViaBasepullQueryStep().willTheBasePullQueryBeUsed(new RunBackendStepInput()
         .withValues(Map.of("queryFilterJson", "{}"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSubtractingSeconds() throws QException
   {
      String originalLastRunTime       = "2023-12-28T15:00:00Z";
      String lastRunTimeMinusOneMinute = "2023-12-28T14:59:00Z";

      String originalThisRunTime        = "2023-12-28T15:05:00Z";
      String thisRunTimePlusFiveSeconds = "2023-12-28T15:05:05Z";

      ///////////////////////////
      // cases for lastRunTime //
      ///////////////////////////
      {
         ///////////////////////////////////////////////////////////////////////////////
         // confirm we don't fail (and don't subtract) if config is absent from input //
         ///////////////////////////////////////////////////////////////////////////////
         RunBackendStepInput input = new RunBackendStepInput();
         input.setBasepullLastRunTime(Instant.parse(originalLastRunTime));
         String lastRunTimeString = new ExtractViaBasepullQueryStep().getLastRunTimeString(input);
         assertEquals(originalLastRunTime, lastRunTimeString);
      }
      {
         //////////////////////////////////////////////////////////////////////////////////
         // confirm we don't fail or subtract if secondsToSubtract isn't given in config //
         //////////////////////////////////////////////////////////////////////////////////
         RunBackendStepInput input = new RunBackendStepInput();
         input.setBasepullLastRunTime(Instant.parse(originalLastRunTime));
         input.addValue(RunProcessAction.BASEPULL_CONFIGURATION, new BasepullConfiguration());
         String lastRunTimeString = new ExtractViaBasepullQueryStep().getLastRunTimeString(input);
         assertEquals(originalLastRunTime, lastRunTimeString);
      }
      {
         ///////////////////////////////////////////////////////////////////////
         // confirm we do subtract if a subtract value is given in the config //
         ///////////////////////////////////////////////////////////////////////
         RunBackendStepInput input = new RunBackendStepInput();
         input.setBasepullLastRunTime(Instant.parse(originalLastRunTime));
         input.addValue(RunProcessAction.BASEPULL_CONFIGURATION, new BasepullConfiguration()
            .withSecondsToSubtractFromLastRunTimeForTimestampQuery(60));
         String lastRunTimeString = new ExtractViaBasepullQueryStep().getLastRunTimeString(input);
         assertEquals(lastRunTimeMinusOneMinute, lastRunTimeString);
      }

      ///////////////////////////
      // cases for thisRunTime //
      ///////////////////////////
      {
         ///////////////////////////////////////////////////////////////////////////////
         // confirm we don't fail (and don't subtract) if config is absent from input //
         ///////////////////////////////////////////////////////////////////////////////
         RunBackendStepInput input = new RunBackendStepInput();
         input.addValue(RunProcessAction.BASEPULL_THIS_RUNTIME_KEY, originalThisRunTime);
         String thisRunTimeString = new ExtractViaBasepullQueryStep().getThisRunTimeString(input);
         assertEquals(originalThisRunTime, thisRunTimeString);
      }
      {
         //////////////////////////////////////////////////////////////////////////////////
         // confirm we don't fail or subtract if secondsToSubtract isn't given in config //
         //////////////////////////////////////////////////////////////////////////////////
         RunBackendStepInput input = new RunBackendStepInput();
         input.addValue(RunProcessAction.BASEPULL_THIS_RUNTIME_KEY, originalThisRunTime);
         input.addValue(RunProcessAction.BASEPULL_CONFIGURATION, new BasepullConfiguration());
         String thisRunTimeString = new ExtractViaBasepullQueryStep().getThisRunTimeString(input);
         assertEquals(originalThisRunTime, thisRunTimeString);
      }
      {
         ///////////////////////////////////////////////////////////////////////
         // confirm we do subtract if a subtract value is given in the config //
         ///////////////////////////////////////////////////////////////////////
         RunBackendStepInput input = new RunBackendStepInput();
         input.addValue(RunProcessAction.BASEPULL_THIS_RUNTIME_KEY, originalThisRunTime);
         input.addValue(RunProcessAction.BASEPULL_CONFIGURATION, new BasepullConfiguration()
            .withSecondsToSubtractFromThisRunTimeForTimestampQuery(-5));
         String thisRunTimeString = new ExtractViaBasepullQueryStep().getThisRunTimeString(input);
         assertEquals(thisRunTimePlusFiveSeconds, thisRunTimeString);
      }
   }

}
