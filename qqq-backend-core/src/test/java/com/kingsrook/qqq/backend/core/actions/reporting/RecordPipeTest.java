/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RecordPipe and subclasses
 *******************************************************************************/
class RecordPipeTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      sendRecordsThroughPipeAndAssertCount(new RecordPipe());

      ////////////////////////////////////////////////
      // buffered pipes, w/o specifying buffer size //
      ////////////////////////////////////////////////
      sendRecordsThroughPipeAndAssertCount(new BufferedRecordPipe());
      sendRecordsThroughPipeAndAssertCount(new RecordPipeBufferedWrapper(new RecordPipe()));

      ////////////////////////////////////////////////////////////////////////
      // buffered pipes, w/ buffer sizes that will trigger multiple flushes //
      ////////////////////////////////////////////////////////////////////////
      for(int i = 1; i <= 5; i++)
      {
         sendRecordsThroughPipeAndAssertCount(new BufferedRecordPipe(i));
         sendRecordsThroughPipeAndAssertCount(new RecordPipeBufferedWrapper(i, new RecordPipe()));
      }

      sendRecordsThroughPipeAndAssertCount(new DistinctFilteringRecordPipe(new UniqueKey("id")));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void sendRecordsThroughPipeAndAssertCount(RecordPipe recordPipe) throws QException
   {
      /////////////////////////////////////////////////////////
      // add records, with alternating scalar and list calls //
      /////////////////////////////////////////////////////////
      recordPipe.addRecord(new QRecord().withValue("id", 1));
      recordPipe.addRecords(List.of(new QRecord().withValue("id", 2), new QRecord().withValue("id", 3)));
      recordPipe.addRecord(new QRecord().withValue("id", 4));
      recordPipe.addRecord(new QRecord().withValue("id", 5));

      if(recordPipe instanceof BufferedRecordPipe bufferedRecordPipe)
      {
         /////////////////////////////////////////////
         // execute a final flush on buffered pipes //
         /////////////////////////////////////////////
         bufferedRecordPipe.finalFlush();
      }

      if(recordPipe instanceof RecordPipeBufferedWrapper recordPipeBufferedWrapper)
      {
         ///////////////////////////////////////////////////////////
         // get data from the wrapped pipe, for buffered wrappers //
         ///////////////////////////////////////////////////////////
         recordPipe = recordPipeBufferedWrapper.getWrappedPipe();
      }

      ///////////////////////////////////////////////
      // assert about the number of output records //
      ///////////////////////////////////////////////
      List<QRecord> outputRecords = recordPipe.consumeAvailableRecords();
      assertEquals(5, outputRecords.size());
      assertEquals(5, recordPipe.getTotalRecordCount());

      ///////////////////////////////////////////////////////
      // assert records came through in the expected order //
      ///////////////////////////////////////////////////////
      assertEquals(1, outputRecords.get(0).getValue("id"));
      assertEquals(2, outputRecords.get(1).getValue("id"));
      assertEquals(3, outputRecords.get(2).getValue("id"));
      assertEquals(4, outputRecords.get(3).getValue("id"));
      assertEquals(5, outputRecords.get(4).getValue("id"));
   }

}