/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import java.time.DayOfWeek;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for QQueryFilterFormatter 
 *******************************************************************************/
class QQueryFilterFormatterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      TestUtils.insertDefaultShapes(QContext.getQInstance());

      QQueryFilter filter = new QQueryFilter()
         .withCriteria("firstName", QCriteriaOperator.EQUALS, "Darin")
         .withCriteria("lastName", QCriteriaOperator.IN, List.of("Kelkhoff", "Smellkhoff", "Dumbkhoff"))
         .withCriteria("favoriteShapeId", QCriteriaOperator.NOT_EQUALS, List.of(1));

      assertEquals("First Name equals Darin AND Last Name is any of Kelkhoff and 2 other values AND Favorite Shape does not equal Triangle", QQueryFilterFormatter.formatQueryFilter(TestUtils.TABLE_NAME_PERSON, filter));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWeekdayOfDateTimeFunctionWithIN() throws QException
   {
      FieldFunction weekdayOfDateTime = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate");

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("createDate", QCriteriaOperator.IN, List.of(DayOfWeek.MONDAY.getValue(), DayOfWeek.FRIDAY.getValue()))
            .withFieldFunction(weekdayOfDateTime));

      String result = QQueryFilterFormatter.formatQueryFilter(TestUtils.TABLE_NAME_PERSON, filter);
      assertThat(result).contains("day is any of");
      assertThat(result).contains("Monday");
      assertThat(result).contains("Friday");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWeekdayOfDateFunctionWithIN() throws QException
   {
      FieldFunction weekdayOfDate = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate");

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.IN, List.of(DayOfWeek.WEDNESDAY.getValue()))
            .withFieldFunction(weekdayOfDate));

      String result = QQueryFilterFormatter.formatQueryFilter(TestUtils.TABLE_NAME_PERSON, filter);
      assertThat(result).contains("day is any of");
      assertThat(result).contains("Wednesday");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWeekdayFunctionWithNOT_IN() throws QException
   {
      FieldFunction weekdayOfDateTime = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate");

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("createDate", QCriteriaOperator.NOT_IN, List.of(DayOfWeek.SATURDAY.getValue(), DayOfWeek.SUNDAY.getValue()))
            .withFieldFunction(weekdayOfDateTime));

      String result = QQueryFilterFormatter.formatQueryFilter(TestUtils.TABLE_NAME_PERSON, filter);
      assertThat(result).contains("day is none of");
      assertThat(result).contains("Saturday");
      assertThat(result).contains("Sunday");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWeekdayFunctionWithSingleValue() throws QException
   {
      FieldFunction weekdayOfDate = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate");

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.IN, List.of(DayOfWeek.SUNDAY.getValue()))
            .withFieldFunction(weekdayOfDate));

      String result = QQueryFilterFormatter.formatQueryFilter(TestUtils.TABLE_NAME_PERSON, filter);
      assertThat(result).contains("day is any of Sunday");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWeekdayFunctionWithThreePlusValues() throws QException
   {
      FieldFunction weekdayOfDateTime = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate");

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("createDate", QCriteriaOperator.IN, List.of(
               DayOfWeek.MONDAY.getValue(), DayOfWeek.TUESDAY.getValue(), DayOfWeek.WEDNESDAY.getValue()))
            .withFieldFunction(weekdayOfDateTime));

      String result = QQueryFilterFormatter.formatQueryFilter(TestUtils.TABLE_NAME_PERSON, filter);
      assertThat(result).contains("day is any of Monday and 2 other values");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonWeekdayFunctionDoesNotAddDay() throws QException
   {
      FieldFunction subStringFunction = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("firstName");

      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.IN, List.of("D", "T"))
            .withFieldFunction(subStringFunction));

      String result = QQueryFilterFormatter.formatQueryFilter(TestUtils.TABLE_NAME_PERSON, filter);
      assertThat(result).contains("is any of");
      assertThat(result).doesNotContain("day is any of");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNoFieldFunctionINStillWorks() throws QException
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria("firstName", QCriteriaOperator.IN, List.of("Darin", "Tim"));

      String result = QQueryFilterFormatter.formatQueryFilter(TestUtils.TABLE_NAME_PERSON, filter);
      assertEquals("First Name is any of Darin and Tim", result);
      assertThat(result).doesNotContain("day");
   }

}