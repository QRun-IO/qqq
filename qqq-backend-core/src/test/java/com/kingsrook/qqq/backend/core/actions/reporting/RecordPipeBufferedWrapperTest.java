/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit tests for RecordPipeBufferedWrapper.
 **
 ** The wrapper overrides flush() so that buffered records are forwarded into the
 ** wrapped RecordPipe rather than into its own queue, allowing callers who hold a
 ** reference to the original pipe to still consume the records.
 *******************************************************************************/
class RecordPipeBufferedWrapperTest extends BaseTest
{

   /*******************************************************************************
    ** Records flushed by the wrapper should arrive in the wrapped pipe's queue,
    ** not in the wrapper itself.
    *******************************************************************************/
   @Test
   void testFlush_recordsArrivedInWrappedPipe() throws Exception
   {
      List<QRecord> consumed  = new ArrayList<>();
      RecordPipe    inner     = new RecordPipe();
      inner.setPostRecordActions(consumed::addAll);

      RecordPipeBufferedWrapper wrapper = new RecordPipeBufferedWrapper(3, inner);

      wrapper.addRecord(new QRecord().withValue("id", 1));
      wrapper.addRecord(new QRecord().withValue("id", 2));
      assertThat(consumed).isEmpty();

      wrapper.addRecord(new QRecord().withValue("id", 3)); // triggers flush

      assertThat(consumed).hasSize(3);
      assertThat(wrapper.buffer).isEmpty();
   }



   /*******************************************************************************
    ** finalFlush drains remaining records into the wrapped pipe.
    *******************************************************************************/
   @Test
   void testFinalFlush_remainingRecordsGoToWrappedPipe() throws Exception
   {
      List<QRecord> consumed  = new ArrayList<>();
      RecordPipe    inner     = new RecordPipe();
      inner.setPostRecordActions(consumed::addAll);

      RecordPipeBufferedWrapper wrapper = new RecordPipeBufferedWrapper(100, inner);

      wrapper.addRecord(new QRecord().withValue("id", 1));
      wrapper.addRecord(new QRecord().withValue("id", 2));
      assertThat(consumed).isEmpty();

      wrapper.finalFlush();

      assertThat(consumed).hasSize(2);
      assertThat(wrapper.buffer).isEmpty();
   }



   /*******************************************************************************
    ** Default no-arg constructor (buffer size 100) works end-to-end.
    *******************************************************************************/
   @Test
   void testDefaultConstructor_flushesIntoWrappedPipeAtThreshold() throws Exception
   {
      List<QRecord> consumed = new ArrayList<>();
      RecordPipe    inner    = new RecordPipe();
      inner.setPostRecordActions(consumed::addAll);

      RecordPipeBufferedWrapper wrapper = new RecordPipeBufferedWrapper(inner);

      for(int i = 0; i < 99; i++)
      {
         wrapper.addRecord(new QRecord().withValue("id", i));
      }
      assertThat(consumed).isEmpty();

      wrapper.addRecord(new QRecord().withValue("id", 99)); // hits buffer size 100
      assertThat(consumed).hasSize(100);
   }



   /*******************************************************************************
    ** Full lifecycle: fill past threshold, then finalFlush remainder.
    *******************************************************************************/
   @Test
   void testFullLifecycle_multipleFlushesPlusFinalFlush() throws Exception
   {
      List<QRecord> consumed = new ArrayList<>();
      RecordPipe    inner    = new RecordPipe();
      inner.setPostRecordActions(consumed::addAll);

      RecordPipeBufferedWrapper wrapper = new RecordPipeBufferedWrapper(3, inner);

      for(int i = 0; i < 7; i++)
      {
         wrapper.addRecord(new QRecord().withValue("id", i));
      }
      // 7 records with buffer=3: two full flushes (6 records), 1 remaining in buffer
      assertThat(consumed).hasSize(6);

      wrapper.finalFlush();
      assertThat(consumed).hasSize(7);
      ////////////////////////////////////////////////////////////////////////////////////////
      // The wrapper delegates flush to wrappedPipe.addRecords() — so totalRecordCount is  //
      // tracked on the inner pipe, not on the wrapper itself.                             //
      ////////////////////////////////////////////////////////////////////////////////////////
      assertEquals(7, inner.getTotalRecordCount());
   }

}
