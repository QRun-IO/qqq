/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for NoCodeWidgetVelocityUtils
 *******************************************************************************/
class NoCodeWidgetVelocityUtilsTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFormatSecondsAsDuration()
   {
      int HOUR = 60 * 60;
      int DAY  = 24 * 60 * 60;

      NoCodeWidgetVelocityUtils utils = new NoCodeWidgetVelocityUtils(null, null);
      assertEquals("", utils.formatSecondsAsDuration(null));
      assertEquals("0 seconds", utils.formatSecondsAsDuration(0));
      assertEquals("1 second", utils.formatSecondsAsDuration(1));
      assertEquals("59 seconds", utils.formatSecondsAsDuration(59));

      assertEquals("1 minute", utils.formatSecondsAsDuration(60));
      assertEquals("1 minute 1 second", utils.formatSecondsAsDuration(61));
      assertEquals("2 minutes 1 second", utils.formatSecondsAsDuration(121));
      assertEquals("2 minutes 2 seconds", utils.formatSecondsAsDuration(122));
      assertEquals("3 minutes", utils.formatSecondsAsDuration(180));

      assertEquals("1 hour", utils.formatSecondsAsDuration(HOUR));
      assertEquals("1 hour 1 second", utils.formatSecondsAsDuration(HOUR + 1));
      assertEquals("1 hour 1 minute", utils.formatSecondsAsDuration(HOUR + 60));
      assertEquals("1 hour 1 minute 1 second", utils.formatSecondsAsDuration(HOUR + 60 + 1));
      assertEquals("1 hour 2 minutes 1 second", utils.formatSecondsAsDuration(HOUR + 120 + 1));
      assertEquals("2 hours 2 minutes 2 seconds", utils.formatSecondsAsDuration(2 * 60 * 60 + 120 + 2));
      assertEquals("23 hours 59 minutes 59 seconds", utils.formatSecondsAsDuration(DAY - 1));

      assertEquals("1 day", utils.formatSecondsAsDuration(DAY));
      assertEquals("1 day 1 second", utils.formatSecondsAsDuration(DAY + 1));
      assertEquals("1 day 1 minute", utils.formatSecondsAsDuration(DAY + 60));
      assertEquals("1 day 1 hour 1 minute 1 second", utils.formatSecondsAsDuration(DAY + HOUR + 60 + 1));
      assertEquals("2 days 2 hours 2 minutes 2 seconds", utils.formatSecondsAsDuration(2 * DAY + 2 * 60 * 60 + 120 + 2));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFormatSecondsAsRoundedDuration()
   {
      int HOUR = 60 * 60;
      int DAY  = 24 * 60 * 60;

      NoCodeWidgetVelocityUtils utils = new NoCodeWidgetVelocityUtils(null, null);
      assertEquals("", utils.formatSecondsAsRoundedDuration(null));
      assertEquals("0 seconds", utils.formatSecondsAsRoundedDuration(0));
      assertEquals("1 second", utils.formatSecondsAsRoundedDuration(1));
      assertEquals("59 seconds", utils.formatSecondsAsRoundedDuration(59));

      assertEquals("1 minute", utils.formatSecondsAsRoundedDuration(60));
      assertEquals("1 minute", utils.formatSecondsAsRoundedDuration(61));
      assertEquals("2 minutes", utils.formatSecondsAsRoundedDuration(121));
      assertEquals("2 minutes", utils.formatSecondsAsRoundedDuration(122));
      assertEquals("3 minutes", utils.formatSecondsAsRoundedDuration(180));

      assertEquals("1 hour", utils.formatSecondsAsRoundedDuration(HOUR));
      assertEquals("1 hour", utils.formatSecondsAsRoundedDuration(HOUR + 1));
      assertEquals("1 hour", utils.formatSecondsAsRoundedDuration(HOUR + 60));
      assertEquals("1 hour", utils.formatSecondsAsRoundedDuration(HOUR + 60 + 1));
      assertEquals("1 hour", utils.formatSecondsAsRoundedDuration(HOUR + 120 + 1));
      assertEquals("2 hours", utils.formatSecondsAsRoundedDuration(2 * 60 * 60 + 120 + 2));
      assertEquals("23 hours", utils.formatSecondsAsRoundedDuration(DAY - 1));

      assertEquals("1 day", utils.formatSecondsAsRoundedDuration(DAY));
      assertEquals("1 day", utils.formatSecondsAsRoundedDuration(DAY + 1));
      assertEquals("1 day", utils.formatSecondsAsRoundedDuration(DAY + 60));
      assertEquals("1 day", utils.formatSecondsAsRoundedDuration(DAY + HOUR + 60 + 1));
      assertEquals("2 days", utils.formatSecondsAsRoundedDuration(2 * DAY + 2 * 60 * 60 + 120 + 2));
   }

}