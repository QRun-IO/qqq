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

package com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions;


import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ThisOrLastPeriod, as of fixed moments, across daylight-saving changes.
 *******************************************************************************/
class ThisOrLastPeriodTest extends BaseTest
{
   private static final QFieldMetaData DATE_TIME_FIELD = new QFieldMetaData("when", QFieldType.DATE_TIME);
   private static final QFieldMetaData DATE_FIELD      = new QFieldMetaData("day", QFieldType.DATE);



   /*******************************************************************************
    ** "start of last month", asked in November (CST), is Oct 1 at midnight CDT -
    ** not Oct 1 at midnight in the offset in effect right now.
    *******************************************************************************/
   @Test
   void testStartOfLastMonthAcrossDaylightSaving() throws QException
   {
      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE, "America/Chicago");
      Instant nov14 = Instant.parse("2026-11-14T18:00:00Z");

      assertEquals(Instant.parse("2026-10-01T05:00:00Z"), ThisOrLastPeriod.last(ChronoUnit.MONTHS).evaluate(DATE_TIME_FIELD, nov14));
      assertEquals(Instant.parse("2026-11-01T05:00:00Z"), ThisOrLastPeriod.this_(ChronoUnit.MONTHS).evaluate(DATE_TIME_FIELD, nov14));
      assertEquals(Instant.parse("2025-01-01T06:00:00Z"), ThisOrLastPeriod.last(ChronoUnit.YEARS).evaluate(DATE_TIME_FIELD, nov14));

      // and the reverse: in April (CDT), last month is March 1, at midnight CST
      Instant apr14 = Instant.parse("2026-04-14T18:00:00Z");
      assertEquals(Instant.parse("2026-03-01T06:00:00Z"), ThisOrLastPeriod.last(ChronoUnit.MONTHS).evaluate(DATE_TIME_FIELD, apr14));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDatesAndDays() throws QException
   {
      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE, "America/Chicago");
      Instant wed = Instant.parse("2026-10-15T03:30:00Z"); // Wed Oct 14, 10:30 PM in Chicago

      assertEquals(LocalDate.of(2026, 10, 14), ThisOrLastPeriod.this_(ChronoUnit.DAYS).evaluate(DATE_FIELD, wed));
      assertEquals(LocalDate.of(2026, 10, 13), ThisOrLastPeriod.last(ChronoUnit.DAYS).evaluate(DATE_FIELD, wed));
      assertEquals(LocalDate.of(2026, 10, 11), ThisOrLastPeriod.this_(ChronoUnit.WEEKS).evaluate(DATE_FIELD, wed));
      assertEquals(LocalDate.of(2026, 10, 4), ThisOrLastPeriod.last(ChronoUnit.WEEKS).evaluate(DATE_FIELD, wed));
      assertEquals(LocalDate.of(2026, 10, 1), ThisOrLastPeriod.this_(ChronoUnit.MONTHS).evaluate(DATE_FIELD, wed));
      assertEquals(LocalDate.of(2025, 1, 1), ThisOrLastPeriod.last(ChronoUnit.YEARS).evaluate(DATE_FIELD, wed));

      assertEquals(Instant.parse("2026-10-14T05:00:00Z"), ThisOrLastPeriod.this_(ChronoUnit.DAYS).evaluate(DATE_TIME_FIELD, wed));
      assertEquals(Instant.parse("2026-10-11T05:00:00Z"), ThisOrLastPeriod.this_(ChronoUnit.WEEKS).evaluate(DATE_TIME_FIELD, wed));
      assertEquals(Instant.parse("2026-10-15T03:00:00Z"), ThisOrLastPeriod.this_(ChronoUnit.HOURS).evaluate(DATE_TIME_FIELD, wed));
   }

}
