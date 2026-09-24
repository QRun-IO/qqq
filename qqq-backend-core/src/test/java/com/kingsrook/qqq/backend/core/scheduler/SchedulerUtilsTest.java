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

package com.kingsrook.qqq.backend.core.scheduler;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for SchedulerUtils.allowedToStart() — which gates scheduled job
 ** execution based on the qqq.scheduleManager.onlyStartNamesMatching system property.
 *******************************************************************************/
class SchedulerUtilsTest
{
   private static final String PROPERTY = "qqq.scheduleManager.onlyStartNamesMatching";


   /*******************************************************************************
    ** Clear system property before each test to ensure clean test state.
    *******************************************************************************/
   @BeforeEach
   void clearProperty()
   {
      System.clearProperty(PROPERTY);
   }


   /*******************************************************************************
    ** Clear system property after each test to avoid polluting other tests.
    *******************************************************************************/
   @AfterEach
   void restoreProperty()
   {
      System.clearProperty(PROPERTY);
   }



   /*******************************************************************************
    ** When the system property is absent (empty string default), all jobs are allowed.
    *******************************************************************************/
   @Test
   void testAllowedToStart_noPropertySet_alwaysReturnsTrue()
   {
      assertTrue(SchedulerUtils.allowedToStart("importOrdersJob"));
      assertTrue(SchedulerUtils.allowedToStart("sendEmailsJob"));
      assertTrue(SchedulerUtils.allowedToStart(""));
   }



   /*******************************************************************************
    ** When the property holds a literal name, only the exact match is allowed.
    *******************************************************************************/
   @Test
   void testAllowedToStart_literalPattern_matchesExactName()
   {
      System.setProperty(PROPERTY, "importOrdersJob");

      assertTrue(SchedulerUtils.allowedToStart("importOrdersJob"));
      assertFalse(SchedulerUtils.allowedToStart("sendEmailsJob"));
   }



   /*******************************************************************************
    ** A regex pattern like "import.*" allows jobs whose names start with "import".
    *******************************************************************************/
   @Test
   void testAllowedToStart_regexWildcard_matchesByPattern()
   {
      System.setProperty(PROPERTY, "import.*");

      assertTrue(SchedulerUtils.allowedToStart("importOrdersJob"));
      assertTrue(SchedulerUtils.allowedToStart("importLineItemsJob"));
      assertFalse(SchedulerUtils.allowedToStart("sendEmailsJob"));
   }



   /*******************************************************************************
    ** An alternation regex allows multiple distinct job names.
    *******************************************************************************/
   @Test
   void testAllowedToStart_alternationPattern_matchesEitherName()
   {
      System.setProperty(PROPERTY, "importOrdersJob|sendEmailsJob");

      assertTrue(SchedulerUtils.allowedToStart("importOrdersJob"));
      assertTrue(SchedulerUtils.allowedToStart("sendEmailsJob"));
      assertFalse(SchedulerUtils.allowedToStart("archiveRecordsJob"));
   }



   /*******************************************************************************
    ** The empty-string property value (explicit) behaves the same as absent —
    ** allows everything.
    *******************************************************************************/
   @Test
   void testAllowedToStart_emptyStringPropertyExplicit_allowsAll()
   {
      System.setProperty(PROPERTY, "");

      assertTrue(SchedulerUtils.allowedToStart("anyJobName"));
   }

}
