/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for BufferedRecordPipe — buffering threshold, flush-on-full,
 ** finalFlush, and bulk addRecords behaviour.
 *******************************************************************************/
class BufferedRecordPipeTest extends BaseTest
{

   /*******************************************************************************
    ** Records below the buffer threshold stay in the buffer and are NOT forwarded
    ** to the internal queue until the buffer is full or finalFlush is called.
    *******************************************************************************/
   @Test
   void testAddRecord_belowThreshold_staysInBuffer() throws Exception
   {
      List<QRecord>        consumed = new ArrayList<>();
      BufferedRecordPipe   pipe     = new BufferedRecordPipe(5);
      pipe.setPostRecordActions(consumed::addAll);

      for(int i = 0; i < 4; i++)
      {
         pipe.addRecord(new QRecord().withValue("id", i));
      }

      assertThat(consumed).isEmpty();
      assertThat(pipe.buffer).hasSize(4);
   }



   /*******************************************************************************
    ** When buffer reaches the threshold it is forwarded immediately.
    *******************************************************************************/
   @Test
   void testAddRecord_atThreshold_flushes() throws Exception
   {
      List<QRecord>      consumed = new ArrayList<>();
      BufferedRecordPipe pipe     = new BufferedRecordPipe(3);
      pipe.setPostRecordActions(consumed::addAll);

      pipe.addRecord(new QRecord().withValue("id", 1));
      pipe.addRecord(new QRecord().withValue("id", 2));
      pipe.addRecord(new QRecord().withValue("id", 3));

      assertThat(consumed).hasSize(3);
      assertThat(pipe.buffer).isEmpty();
   }



   /*******************************************************************************
    ** finalFlush pushes remaining buffered records to the queue.
    *******************************************************************************/
   @Test
   void testFinalFlush_drainsPendingBuffer() throws Exception
   {
      List<QRecord>      consumed = new ArrayList<>();
      BufferedRecordPipe pipe     = new BufferedRecordPipe(100);
      pipe.setPostRecordActions(consumed::addAll);

      pipe.addRecord(new QRecord().withValue("id", 1));
      pipe.addRecord(new QRecord().withValue("id", 2));

      assertThat(consumed).isEmpty();

      pipe.finalFlush();

      assertThat(consumed).hasSize(2);
      assertThat(pipe.buffer).isEmpty();
   }



   /*******************************************************************************
    ** finalFlush on an already-empty buffer is a safe no-op.
    *******************************************************************************/
   @Test
   void testFinalFlush_emptyBuffer_noException() throws Exception
   {
      BufferedRecordPipe pipe = new BufferedRecordPipe(10);

      pipe.finalFlush();

      assertThat(pipe.buffer).isEmpty();
   }



   /*******************************************************************************
    ** Custom buffer size is respected — default is 100; override to 2.
    *******************************************************************************/
   @Test
   void testCustomBufferSize_smallerThanDefault_flushesEarlier() throws Exception
   {
      List<QRecord>      consumed = new ArrayList<>();
      BufferedRecordPipe pipe     = new BufferedRecordPipe(2);
      pipe.setPostRecordActions(consumed::addAll);

      pipe.addRecord(new QRecord().withValue("id", 1));
      assertThat(consumed).isEmpty();

      pipe.addRecord(new QRecord().withValue("id", 2));
      assertThat(consumed).hasSize(2);
   }



   /*******************************************************************************
    ** Default no-arg constructor uses buffer size 100 — adding 99 records does not flush.
    *******************************************************************************/
   @Test
   void testDefaultConstructor_bufferSize100_doesNotFlushAt99() throws Exception
   {
      List<QRecord>      consumed = new ArrayList<>();
      BufferedRecordPipe pipe     = new BufferedRecordPipe();
      pipe.setPostRecordActions(consumed::addAll);

      for(int i = 0; i < 99; i++)
      {
         pipe.addRecord(new QRecord().withValue("id", i));
      }

      assertThat(consumed).isEmpty();
      assertThat(pipe.buffer).hasSize(99);
   }



   /*******************************************************************************
    ** addRecords(List) bulk path flushes when the cumulative size hits threshold.
    *******************************************************************************/
   @Test
   void testAddRecords_bulk_flushesWhenThresholdReached() throws Exception
   {
      List<QRecord>      consumed = new ArrayList<>();
      BufferedRecordPipe pipe     = new BufferedRecordPipe(3);
      pipe.setPostRecordActions(consumed::addAll);

      List<QRecord> batch = List.of(
         new QRecord().withValue("id", 1),
         new QRecord().withValue("id", 2),
         new QRecord().withValue("id", 3)
      );

      pipe.addRecords(batch);

      assertThat(consumed).hasSize(3);
      assertThat(pipe.buffer).isEmpty();
   }



   /*******************************************************************************
    ** addRecords bulk path accumulates below-threshold records in the buffer.
    *******************************************************************************/
   @Test
   void testAddRecords_bulk_belowThreshold_staysInBuffer() throws Exception
   {
      List<QRecord>      consumed = new ArrayList<>();
      BufferedRecordPipe pipe     = new BufferedRecordPipe(10);
      pipe.setPostRecordActions(consumed::addAll);

      pipe.addRecords(List.of(
         new QRecord().withValue("id", 1),
         new QRecord().withValue("id", 2)
      ));

      assertThat(consumed).isEmpty();
      assertThat(pipe.buffer).hasSize(2);
   }



   /*******************************************************************************
    ** Multiple flush cycles accumulate the total record count correctly.
    *******************************************************************************/
   @Test
   void testMultipleFlushCycles_totalCountCorrect() throws Exception
   {
      List<QRecord>      consumed = new ArrayList<>();
      BufferedRecordPipe pipe     = new BufferedRecordPipe(2);
      pipe.setPostRecordActions(consumed::addAll);

      for(int i = 0; i < 6; i++)
      {
         pipe.addRecord(new QRecord().withValue("id", i));
      }

      assertThat(consumed).hasSize(6);
      assertEquals(6, pipe.getTotalRecordCount());
   }

}
