/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.util.Collections;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for QueryOutputList 
 *******************************************************************************/
class QueryOutputListTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QueryOutputList.resetDefaultLogSizeOverValues();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLogSize() throws QException
   {
      QueryInput  queryInput  = new QueryInput(TestUtils.TABLE_NAME_PERSON);
      QueryOutput queryOutput = new QueryOutput(queryInput);

      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(QueryOutputList.class);

      ///////////////////////
      // set up our limits //
      ///////////////////////
      int infoLimit  = 10;
      int warnLimit  = 20;
      int errorLimit = 30;

      QueryOutputList.setLogSizeInfoOver(infoLimit);
      QueryOutputList.setLogSizeWarnOver(warnLimit);
      QueryOutputList.setLogSizeErrorOver(errorLimit);

      ////////////////////////////
      // add records one-by-one //
      ////////////////////////////
      for(int i = 0; i < errorLimit; i++)
      {
         queryOutput.addRecord(new QRecord());
      }

      ///////////////////////////////////////////////////////////////
      // assert we got the expected logs as each level was crossed //
      ///////////////////////////////////////////////////////////////
      assertEquals(3, collectingLogger.getCollectedMessages().size());

      assertEquals(Level.INFO, collectingLogger.getCollectedMessages().get(0).getLevel());
      assertThat(collectingLogger.getCollectedMessages().get(0).getMessage())
         .contains("\"noRecords\":" + infoLimit)
         .contains("\"tableName\":\"" + TestUtils.TABLE_NAME_PERSON + "\"");

      assertEquals(Level.WARN, collectingLogger.getCollectedMessages().get(1).getLevel());
      assertThat(collectingLogger.getCollectedMessages().get(1).getMessage())
         .contains("\"noRecords\":" + warnLimit)
         .contains("\"tableName\":\"" + TestUtils.TABLE_NAME_PERSON + "\"");

      assertEquals(Level.ERROR, collectingLogger.getCollectedMessages().get(2).getLevel());
      assertThat(collectingLogger.getCollectedMessages().get(2).getMessage())
         .contains("\"noRecords\":" + errorLimit)
         .contains("\"tableName\":\"" + TestUtils.TABLE_NAME_PERSON + "\"");

      //////////////////////////////////////////////////////////////////////////////////////////
      // reset the logger - then run again, doing a bulk add that goes straight to error size //
      //////////////////////////////////////////////////////////////////////////////////////////
      collectingLogger.clear();
      queryOutput = new QueryOutput(queryInput);
      int bulkSize = errorLimit + 1;
      queryOutput.addRecords(Collections.nCopies(bulkSize, new QRecord()));

      assertEquals(1, collectingLogger.getCollectedMessages().size());
      assertEquals(Level.ERROR, collectingLogger.getCollectedMessages().get(0).getLevel());
      assertThat(collectingLogger.getCollectedMessages().get(0).getMessage())
         .contains("\"noRecords\":" + bulkSize)
         .contains("\"tableName\":\"" + TestUtils.TABLE_NAME_PERSON + "\"");

   }

}