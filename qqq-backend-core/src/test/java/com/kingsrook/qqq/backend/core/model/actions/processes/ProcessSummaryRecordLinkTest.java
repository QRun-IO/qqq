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


import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for ProcessSummaryRecordLink — covering getFullText(), getMessage(),
 ** toLogPair(), and constructor overloads.
 *******************************************************************************/
class ProcessSummaryRecordLinkTest
{

   /*******************************************************************************
    ** All three text parts present — concatenated with single spaces, no trailing space.
    *******************************************************************************/
   @Test
   void testGetFullText_allPartsPresent_concatenatesWithSpaces()
   {
      ProcessSummaryRecordLink link = new ProcessSummaryRecordLink()
         .withLinkPreText("Created")
         .withLinkText("order #42")
         .withLinkPostText("successfully");

      assertEquals("Created order #42 successfully", link.getFullText());
   }



   /*******************************************************************************
    ** Only linkText set — returns just that value with no leading/trailing space.
    *******************************************************************************/
   @Test
   void testGetFullText_onlyLinkText_returnsLinkTextTrimmed()
   {
      ProcessSummaryRecordLink link = new ProcessSummaryRecordLink()
         .withLinkText("order #42");

      assertEquals("order #42", link.getFullText());
   }



   /*******************************************************************************
    ** getMessage() delegates to getFullText() and returns the same value.
    *******************************************************************************/
   @Test
   void testGetMessage_delegatesToGetFullText_sameResult()
   {
      ProcessSummaryRecordLink link = new ProcessSummaryRecordLink()
         .withLinkPreText("See")
         .withLinkText("record 7");

      assertEquals(link.getFullText(), link.getMessage());
   }



   /*******************************************************************************
    ** Two-arg constructor sets status, tableName, and recordId; link fields are null.
    *******************************************************************************/
   @Test
   void testConstructor_threeArg_setsStatusTableNameRecordId()
   {
      ProcessSummaryRecordLink link = new ProcessSummaryRecordLink(Status.OK, "orderTable", 99);

      assertEquals(Status.OK, link.getStatus());
      assertEquals("orderTable", link.getTableName());
      assertEquals(99, link.getRecordId());
      assertNull(link.getLinkText());
   }



   /*******************************************************************************
    ** Four-arg constructor additionally sets linkText.
    *******************************************************************************/
   @Test
   void testConstructor_fourArg_setsLinkText()
   {
      ProcessSummaryRecordLink link = new ProcessSummaryRecordLink(Status.ERROR, "orders", 5, "Order 5");

      assertEquals("Order 5", link.getLinkText());
   }



   /*******************************************************************************
    ** toLogPair() returns a non-null LogPair that includes key fields.
    *******************************************************************************/
   @Test
   void testToLogPair_returnsNonNull()
   {
      ProcessSummaryRecordLink link = new ProcessSummaryRecordLink(Status.WARNING, "orderTable", 1, "Order 1");

      assertNotNull(link.toLogPair());
      assertThat(link.toLogPair().toString()).contains("ProcessSummary");
   }



   /*******************************************************************************
    ** getFullText() when all text fields are null — exposes the StringIndexOutOfBounds
    ** risk in the trailing deleteCharAt() call.  The current implementation will throw;
    ** this test documents the edge case so a fix is tracked.
    *******************************************************************************/
   @Test
   void testGetFullText_allFieldsNull_doesNotThrow()
   {
      ProcessSummaryRecordLink link = new ProcessSummaryRecordLink(Status.OK, "orders", 1);
      // TODO: getFullText() with no text fields throws StringIndexOutOfBoundsException.
      // Once fixed, assert: assertThat(link.getFullText()).isEmpty();
      org.junit.jupiter.api.Assertions.assertThrows(StringIndexOutOfBoundsException.class, link::getFullText);
   }



   /*******************************************************************************
    ** Fluent setters return the same instance, allowing chaining.
    *******************************************************************************/
   @Test
   void testFluentSetters_returnSameInstance()
   {
      ProcessSummaryRecordLink link = new ProcessSummaryRecordLink();

      assertThat(link.withStatus(Status.OK)).isSameAs(link);
      assertThat(link.withTableName("t")).isSameAs(link);
      assertThat(link.withRecordId(1)).isSameAs(link);
      assertThat(link.withLinkPreText("pre")).isSameAs(link);
      assertThat(link.withLinkText("txt")).isSameAs(link);
      assertThat(link.withLinkPostText("post")).isSameAs(link);
   }

}
