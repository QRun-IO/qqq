/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.fieldfunctions;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for RDBMS FieldFunction adapter implementations.
 ** These test SQL generation without requiring a real database.
 *******************************************************************************/
class RDBMSFieldFunctionAdapterTest extends BaseTest
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testStringLengthWrapColumnName()
   {
      RDBMSStringLengthFunction adapter = new RDBMSStringLengthFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
         .withFieldName("firstName");

      assertEquals("CHAR_LENGTH(first_name)", adapter.wrapColumnName("first_name", ff, s -> s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSubStringWrapColumnNameFromOnly()
   {
      RDBMSSubStringFunction adapter = new RDBMSSubStringFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("firstName")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2));

      assertEquals("SUBSTRING(first_name FROM ?)", adapter.wrapColumnName("first_name", ff, s -> s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSubStringWrapColumnNameFromAndLength()
   {
      RDBMSSubStringFunction adapter = new RDBMSSubStringFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("firstName")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2, SubStringFunction.LENGTH_PARAM, 3));

      assertEquals("SUBSTRING(first_name FROM ? FOR ?)", adapter.wrapColumnName("first_name", ff, s -> s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSubStringGetParamsFromOnly()
   {
      RDBMSSubStringFunction adapter = new RDBMSSubStringFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("firstName")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2));

      List<Serializable> params = adapter.getParams(ff);
      assertEquals(1, params.size());
      assertEquals(2, params.get(0));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSubStringGetParamsFromAndLength()
   {
      RDBMSSubStringFunction adapter = new RDBMSSubStringFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("firstName")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2, SubStringFunction.LENGTH_PARAM, 3));

      List<Serializable> params = adapter.getParams(ff);
      assertEquals(2, params.size());
      assertEquals(2, params.get(0));
      assertEquals(3, params.get(1));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateWrapColumnName()
   {
      RDBMSWeekdayOfDateFunction adapter = new RDBMSWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate");

      assertEquals("WEEKDAY(birth_date) + 1", adapter.wrapColumnName("birth_date", ff, s -> s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateWrapColumnNameForOrderBySundayFirst()
   {
      RDBMSWeekdayOfDateFunction adapter = new RDBMSWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, true));

      String result = adapter.wrapColumnNameForOrderBy("birth_date", ff, s -> s);
      assertTrue(result.contains("% 7"), "Should contain modulo 7 for sunday-first sorting");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateWrapColumnNameForOrderByMondayFirst()
   {
      RDBMSWeekdayOfDateFunction adapter = new RDBMSWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, false));

      String result = adapter.wrapColumnNameForOrderBy("birth_date", ff, s -> s);
      assertFalse(result.contains("% 7"), "Should NOT contain modulo 7 for monday-first sorting");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeWrapColumnName()
   {
      RDBMSWeekdayOfDateTimeFunction adapter = new RDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate");

      assertEquals("WEEKDAY(CONVERT_TZ(create_date, ?, ?)) + 1", adapter.wrapColumnName("create_date", ff, s -> s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeGetParams()
   {
      RDBMSWeekdayOfDateTimeFunction adapter = new RDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "America/Chicago"));

      List<Serializable> params = adapter.getParams(ff);
      assertEquals(2, params.size());
      assertEquals("UTC", params.get(0));
      assertEquals("America/Chicago", params.get(1));
   }

}
