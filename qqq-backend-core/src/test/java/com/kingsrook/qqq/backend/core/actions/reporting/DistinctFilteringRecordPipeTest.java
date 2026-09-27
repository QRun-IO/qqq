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

package com.kingsrook.qqq.backend.core.actions.reporting;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for DistinctFilteringRecordPipe 
 *******************************************************************************/
class DistinctFilteringRecordPipeTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSingleFieldKey() throws QException
   {
      DistinctFilteringRecordPipe pipe = new DistinctFilteringRecordPipe(new UniqueKey("id"));
      pipe.addRecord(new QRecord().withValue("id", 1));
      pipe.addRecord(new QRecord().withValue("id", 1));
      assertEquals(1, pipe.consumeAvailableRecords().size());

      pipe.addRecord(new QRecord().withValue("id", 1));
      assertEquals(0, pipe.consumeAvailableRecords().size());
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultiFieldKey() throws QException
   {
      DistinctFilteringRecordPipe pipe = new DistinctFilteringRecordPipe(new UniqueKey("type", "name"));

      ////////////////////////////
      // add 3 distinct records //
      ////////////////////////////
      pipe.addRecord(new QRecord().withValue("type", 1).withValue("name", "A"));
      pipe.addRecord(new QRecord().withValue("type", 1).withValue("name", "B"));
      pipe.addRecord(new QRecord().withValue("type", 2).withValue("name", "B"));
      assertEquals(3, pipe.consumeAvailableRecords().size());

      ///////////////////////////////////////////////////////////////////
      // now re-add those 3 (should all be discarded) plus one new one //
      ///////////////////////////////////////////////////////////////////
      pipe.addRecord(new QRecord().withValue("type", 1).withValue("name", "A"));
      pipe.addRecord(new QRecord().withValue("type", 1).withValue("name", "B"));
      pipe.addRecord(new QRecord().withValue("type", 2).withValue("name", "B"));
      pipe.addRecord(new QRecord().withValue("type", 2).withValue("name", "A"));
      assertEquals(1, pipe.consumeAvailableRecords().size());
   }

}