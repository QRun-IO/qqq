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

package com.kingsrook.qqq.backend.core.processes.implementations.general;


import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit tests for ProcessSummaryWarningsAndErrorsRollup — covering the rollup
 ** threshold, error/warning bucketing, count tracking, and addToList() output.
 *******************************************************************************/
class ProcessSummaryWarningsAndErrorsRollupTest
{

   /*******************************************************************************
    ** build() factory sets error and warning templates; countErrors()/countWarnings()
    ** start at 0.
    *******************************************************************************/
   @Test
   void testBuild_freshInstance_countsAreZero()
   {
      ProcessSummaryWarningsAndErrorsRollup rollup = ProcessSummaryWarningsAndErrorsRollup.build("processed");

      assertEquals(0, rollup.countErrors());
      assertEquals(0, rollup.countWarnings());
   }



   /*******************************************************************************
    ** Each unique error message gets its own summary line; countErrors() sums them.
    *******************************************************************************/
   @Test
   void testAddError_uniqueMessages_createsDistinctSummaryLines()
   {
      ProcessSummaryWarningsAndErrorsRollup rollup = ProcessSummaryWarningsAndErrorsRollup.build("processed");

      rollup.addError("too old", 1);
      rollup.addError("too old", 2);
      rollup.addError("missing field", 3);

      assertEquals(3, rollup.countErrors());
      assertEquals(0, rollup.countWarnings());

      ArrayList<ProcessSummaryLineInterface> list = new ArrayList<>();
      rollup.addToList(list);
      assertThat(list).hasSize(2);
   }



   /*******************************************************************************
    ** Once uniqueErrorsToShow (default 50) is exceeded, additional unique messages
    ** are routed to the "other errors" bucket instead of creating new lines.
    *******************************************************************************/
   @Test
   void testAddError_exceedsUniqueLimit_routesToOtherErrorsBucket()
   {
      ProcessSummaryWarningsAndErrorsRollup rollup = ProcessSummaryWarningsAndErrorsRollup.build("processed")
         .withUniqueErrorsToShow(3);

      rollup.addError("msg1", 1);
      rollup.addError("msg2", 2);
      rollup.addError("msg3", 3);
      rollup.addError("msg4", 4);
      rollup.addError("msg5", 5);

      assertEquals(5, rollup.countErrors());

      ArrayList<ProcessSummaryLineInterface> list = new ArrayList<>();
      rollup.addToList(list);

      // 3 unique + 1 "other" bucket
      assertThat(list).hasSize(4);
   }



   /*******************************************************************************
    ** Same message added multiple times increments count on the same summary line.
    *******************************************************************************/
   @Test
   void testAddError_sameMessage_incrementsExistingLine()
   {
      ProcessSummaryWarningsAndErrorsRollup rollup = ProcessSummaryWarningsAndErrorsRollup.build("processed");

      rollup.addError("duplicate key", 10);
      rollup.addError("duplicate key", 20);
      rollup.addError("duplicate key", 30);

      assertEquals(3, rollup.countErrors());

      ArrayList<ProcessSummaryLineInterface> list = new ArrayList<>();
      rollup.addToList(list);
      assertThat(list).hasSize(1);
      assertThat(((ProcessSummaryLine) list.get(0)).getCount()).isEqualTo(3);
   }



   /*******************************************************************************
    ** Warnings are tracked independently from errors.
    *******************************************************************************/
   @Test
   void testAddWarning_independent_fromErrors()
   {
      ProcessSummaryWarningsAndErrorsRollup rollup = ProcessSummaryWarningsAndErrorsRollup.build("inserted");

      rollup.addError("bad value", 1);
      rollup.addWarning("possible duplicate", 2);
      rollup.addWarning("possible duplicate", 3);

      assertEquals(1, rollup.countErrors());
      assertEquals(2, rollup.countWarnings());
   }



   /*******************************************************************************
    ** addError() with a null primaryKey increments count without recording a PK.
    *******************************************************************************/
   @Test
   void testAddError_nullPrimaryKey_incrementsCountOnly()
   {
      ProcessSummaryWarningsAndErrorsRollup rollup = ProcessSummaryWarningsAndErrorsRollup.build("processed");

      rollup.addError("missing id", null);
      rollup.addError("missing id", null);

      assertEquals(2, rollup.countErrors());
   }



   /*******************************************************************************
    ** addToList() on a fresh instance (no errors/warnings) adds nothing to the list.
    *******************************************************************************/
   @Test
   void testAddToList_emptyRollup_addsNothingToList()
   {
      ProcessSummaryWarningsAndErrorsRollup rollup = ProcessSummaryWarningsAndErrorsRollup.build("processed");

      ArrayList<ProcessSummaryLineInterface> list = new ArrayList<>();
      rollup.addToList(list);

      assertThat(list).isEmpty();
   }



   /*******************************************************************************
    ** Errors appear before warnings in the list produced by addToList().
    *******************************************************************************/
   @Test
   void testAddToList_errorsBeforeWarnings_preservesOrder()
   {
      ProcessSummaryWarningsAndErrorsRollup rollup = ProcessSummaryWarningsAndErrorsRollup.build("saved");

      rollup.addWarning("needs review", 1);
      rollup.addError("invalid date", 2);

      ArrayList<ProcessSummaryLineInterface> list = new ArrayList<>();
      rollup.addToList(list);

      assertThat(list).hasSize(2);
      assertThat(list.get(0).getStatus()).isEqualTo(Status.ERROR);
      assertThat(list.get(1).getStatus()).isEqualTo(Status.WARNING);
   }

}
