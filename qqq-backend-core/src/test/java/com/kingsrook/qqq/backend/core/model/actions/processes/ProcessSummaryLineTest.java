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

package com.kingsrook.qqq.backend.core.model.actions.processes;


import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for ProcessSummaryLine — covering count tracking, primary key
 ** accumulation, message selection, and list-gating logic.
 *******************************************************************************/
class ProcessSummaryLineTest
{

   /*******************************************************************************
    ** Default count is 0; incrementCount() adds 1 per call.
    *******************************************************************************/
   @Test
   void testIncrementCount_defaultIsZero_incrementsByOne()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK);

      assertEquals(0, line.getCount());
      line.incrementCount();
      assertEquals(1, line.getCount());
      line.incrementCount();
      assertEquals(2, line.getCount());
   }



   /*******************************************************************************
    ** incrementCount(int) should add the specified amount.
    *******************************************************************************/
   @Test
   void testIncrementCount_withAmount_addsThatAmount()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK);
      line.incrementCount(5);
      assertEquals(5, line.getCount());
   }



   /*******************************************************************************
    ** incrementCount on a null count should initialise to 0 then add 1.
    *******************************************************************************/
   @Test
   void testIncrementCount_nullCount_initialisesToZeroThenAdds()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK);
      line.setCount(null);
      line.incrementCount();
      assertEquals(1, line.getCount());
   }



   /*******************************************************************************
    ** incrementCountAndAddPrimaryKey should lazily initialise the list and append.
    *******************************************************************************/
   @Test
   void testIncrementCountAndAddPrimaryKey_lazyInit_appendsKeys()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK);

      assertNull(line.getPrimaryKeys());

      line.incrementCountAndAddPrimaryKey(42);
      line.incrementCountAndAddPrimaryKey(99);

      assertEquals(2, line.getCount());
      assertThat(line.getPrimaryKeys()).containsExactly(42, 99);
   }



   /*******************************************************************************
    ** addSelfToListIfAnyCount should add only when count > 0.
    *******************************************************************************/
   @Test
   void testAddSelfToListIfAnyCount_countZero_notAdded()
   {
      ProcessSummaryLine   line = new ProcessSummaryLine(Status.OK, 0, "zero");
      ArrayList<ProcessSummaryLineInterface> list = new ArrayList<>();
      line.addSelfToListIfAnyCount(list);

      assertThat(list).isEmpty();
   }



   /*******************************************************************************
    ** addSelfToListIfAnyCount adds the instance when count >= 1.
    *******************************************************************************/
   @Test
   void testAddSelfToListIfAnyCount_countPositive_added()
   {
      ProcessSummaryLine   line = new ProcessSummaryLine(Status.ERROR, 3, "three errors");
      ArrayList<ProcessSummaryLineInterface> list = new ArrayList<>();
      line.addSelfToListIfAnyCount(list);

      assertThat(list).hasSize(1).contains(line);
   }



   /*******************************************************************************
    ** addSelfToListIfAnyCount should not add when count is null.
    *******************************************************************************/
   @Test
   void testAddSelfToListIfAnyCount_countNull_notAdded()
   {
      ProcessSummaryLine   line = new ProcessSummaryLine(Status.OK);
      line.setCount(null);
      ArrayList<ProcessSummaryLineInterface> list = new ArrayList<>();
      line.addSelfToListIfAnyCount(list);

      assertThat(list).isEmpty();
   }



   /*******************************************************************************
    ** pickMessage(false) with count == 1 should select singularFutureMessage.
    *******************************************************************************/
   @Test
   void testPickMessage_future_singularCount_selectsSingularFutureMessage()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK, 1, null);
      line.withSingularFutureMessage("will be processed");
      line.withPluralFutureMessage("will be processed (plural)");

      line.pickMessage(false);

      assertEquals("will be processed", line.getMessage());
   }



   /*******************************************************************************
    ** pickMessage(false) with count > 1 should select pluralFutureMessage.
    *******************************************************************************/
   @Test
   void testPickMessage_future_pluralCount_selectsPluralFutureMessage()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK, 3, null);
      line.withSingularFutureMessage("will be processed");
      line.withPluralFutureMessage("records will be processed");

      line.pickMessage(false);

      assertEquals("records will be processed", line.getMessage());
   }



   /*******************************************************************************
    ** pickMessage(true) with count == 1 should select singularPastMessage.
    *******************************************************************************/
   @Test
   void testPickMessage_past_singularCount_selectsSingularPastMessage()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK, 1, null);
      line.withSingularPastMessage("was inserted");
      line.withPluralPastMessage("were inserted");

      line.pickMessage(true);

      assertEquals("was inserted", line.getMessage());
   }



   /*******************************************************************************
    ** pickMessage(true) with count > 1 should select pluralPastMessage.
    *******************************************************************************/
   @Test
   void testPickMessage_past_pluralCount_selectsPluralPastMessage()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK, 5, null);
      line.withSingularPastMessage("was inserted");
      line.withPluralPastMessage("were inserted");

      line.pickMessage(true);

      assertEquals("were inserted", line.getMessage());
   }



   /*******************************************************************************
    ** pickMessage should append messageSuffix when present.
    *******************************************************************************/
   @Test
   void testPickMessage_withMessageSuffix_suffixAppended()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK, 2, null);
      line.withPluralFutureMessage("records")
         .withMessageSuffix(" (see details)");

      line.pickMessage(false);

      assertEquals("records (see details)", line.getMessage());
   }



   /*******************************************************************************
    ** pickMessage with no base message (null singular/plural) should leave
    ** message unchanged — guards against accidental message erasure.
    *******************************************************************************/
   @Test
   void testPickMessage_noBaseMessage_messageUnchanged()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK, 2, "original");

      line.pickMessage(false);

      assertEquals("original", line.getMessage());
   }



   /*******************************************************************************
    ** withSingularMessage sets both singularFutureMessage and singularPastMessage.
    *******************************************************************************/
   @Test
   void testWithSingularMessage_setsBothFutureAndPast()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK)
         .withSingularMessage("record will/was processed");

      assertEquals("record will/was processed", line.getSingularFutureMessage());
      assertEquals("record will/was processed", line.getSingularPastMessage());
   }



   /*******************************************************************************
    ** withPluralMessage sets both pluralFutureMessage and pluralPastMessage.
    *******************************************************************************/
   @Test
   void testWithPluralMessage_setsBothFutureAndPast()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK)
         .withPluralMessage("records will/were processed");

      assertEquals("records will/were processed", line.getPluralFutureMessage());
      assertEquals("records will/were processed", line.getPluralPastMessage());
   }



   /*******************************************************************************
    ** prepareForFrontend(false) should call pickMessage when message is blank.
    *******************************************************************************/
   @Test
   void testPrepareForFrontend_blankMessage_picksMessage()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK, 1, null);
      line.withSingularFutureMessage("will be created");

      line.prepareForFrontend(false);

      assertEquals("will be created", line.getMessage());
   }



   /*******************************************************************************
    ** prepareForFrontend should not overwrite an existing non-blank message.
    *******************************************************************************/
   @Test
   void testPrepareForFrontend_existingMessage_notOverwritten()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK, 1, "already set");
      line.withSingularFutureMessage("should not win");

      line.prepareForFrontend(false);

      assertEquals("already set", line.getMessage());
   }



   /*******************************************************************************
    ** toString should include status, count, and message.
    *******************************************************************************/
   @Test
   void testToString_includesKeyFields()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.ERROR, 7, "validation failed");
      String str = line.toString();

      assertThat(str).contains("ERROR");
      assertThat(str).contains("7");
      assertThat(str).contains("validation failed");
   }



   /*******************************************************************************
    ** Four-arg constructor should populate all fields.
    *******************************************************************************/
   @Test
   void testFourArgConstructor_allFieldsPopulated()
   {
      ArrayList<java.io.Serializable> keys = new ArrayList<>();
      keys.add(1);

      ProcessSummaryLine line = new ProcessSummaryLine(Status.WARNING, 1, "a warning", keys);

      assertEquals(Status.WARNING, line.getStatus());
      assertEquals(1, line.getCount());
      assertEquals("a warning", line.getMessage());
      assertNotNull(line.getPrimaryKeys());
      assertThat(line.getPrimaryKeys()).containsExactly(1);
   }



   /*******************************************************************************
    ** getBulletsOfText returns null when no bullets have been set.
    *******************************************************************************/
   @Test
   void testGetBulletsOfText_notSet_returnsNull()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK);

      assertNull(line.getBulletsOfText());
   }



   /*******************************************************************************
    ** withBulletsOfText fluent setter stores the list and returns the same instance.
    *******************************************************************************/
   @Test
   void testWithBulletsOfText_setsList_returnsSameInstance()
   {
      ArrayList<String>  bullets = new ArrayList<>(List.of("First note", "Second note"));
      ProcessSummaryLine line    = new ProcessSummaryLine(Status.WARNING);

      ProcessSummaryLine returned = line.withBulletsOfText(bullets);

      assertThat(returned).isSameAs(line);
      assertThat(line.getBulletsOfText()).containsExactly("First note", "Second note");
   }



   /*******************************************************************************
    ** setBulletsOfText setter stores the list (imperative form).
    *******************************************************************************/
   @Test
   void testSetBulletsOfText_setsList()
   {
      ArrayList<String>  bullets = new ArrayList<>(List.of("Note A"));
      ProcessSummaryLine line    = new ProcessSummaryLine(Status.INFO);
      line.setBulletsOfText(bullets);

      assertThat(line.getBulletsOfText()).hasSize(1).contains("Note A");
   }



   /*******************************************************************************
    ** withBulletsOfText(null) stores null — allows explicit clearing.
    *******************************************************************************/
   @Test
   void testWithBulletsOfText_nullArg_storesNull()
   {
      ProcessSummaryLine line = new ProcessSummaryLine(Status.OK)
         .withBulletsOfText(new ArrayList<>(List.of("something")));

      line.withBulletsOfText(null);

      assertNull(line.getBulletsOfText());
   }

}
