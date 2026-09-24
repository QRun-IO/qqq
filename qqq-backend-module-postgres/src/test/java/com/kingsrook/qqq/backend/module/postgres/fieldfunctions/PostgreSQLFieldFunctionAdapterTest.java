/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.fieldfunctions;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.module.rdbms.fieldfunctions.RDBMSStringLengthFunction;
import com.kingsrook.qqq.backend.module.rdbms.fieldfunctions.RDBMSSubStringFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit tests for PostgreSQL FieldFunction adapter implementations.
 ** These test SQL generation without requiring a real database connection.
 *******************************************************************************/
class PostgreSQLFieldFunctionAdapterTest
{

   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      QInstance qInstance = new QInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));
      qInstance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      qInstance.addTable(new QTableMetaData().withName("test").withBackendName("memory")
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      QContext.init(qInstance, new QSession());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateWrapColumnName()
   {
      PostgreSQLRDBMSWeekdayOfDateFunction adapter = new PostgreSQLRDBMSWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate");

      assertEquals("EXTRACT(ISODOW FROM birth_date)", adapter.wrapColumnName("birth_date", ff, s -> s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateWrapColumnNameForOrderBySundayFirst()
   {
      PostgreSQLRDBMSWeekdayOfDateFunction adapter = new PostgreSQLRDBMSWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, true));

      String result = adapter.wrapColumnNameForOrderBy("birth_date", ff, s -> s);
      assertTrue(result.contains("EXTRACT(ISODOW FROM birth_date)"), "Should contain ISODOW expression");
      assertTrue(result.contains("% 7"), "Should contain modulo 7 for sunday-first sorting");
      assertEquals("EXTRACT(ISODOW FROM birth_date) % 7", result);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateWrapColumnNameForOrderByMondayFirst()
   {
      PostgreSQLRDBMSWeekdayOfDateFunction adapter = new PostgreSQLRDBMSWeekdayOfDateFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("birthDate")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, false));

      String result = adapter.wrapColumnNameForOrderBy("birth_date", ff, s -> s);
      assertTrue(result.contains("EXTRACT(ISODOW FROM birth_date)"), "Should contain ISODOW expression");
      assertFalse(result.contains("% 7"), "Should NOT contain modulo 7 for monday-first sorting");
      assertEquals("EXTRACT(ISODOW FROM birth_date)", result);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeWrapColumnName()
   {
      PostgreSQLRDBMSWeekdayOfDateTimeFunction adapter = new PostgreSQLRDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate");

      assertEquals("EXTRACT(ISODOW FROM (create_date AT TIME ZONE 'UTC') AT TIME ZONE ?)", adapter.wrapColumnName("create_date", ff, s -> s));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeWrapColumnNameForOrderBySundayFirst()
   {
      PostgreSQLRDBMSWeekdayOfDateTimeFunction adapter = new PostgreSQLRDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, true));

      String result = adapter.wrapColumnNameForOrderBy("create_date", ff, s -> s);
      assertTrue(result.contains("EXTRACT(ISODOW FROM (create_date AT TIME ZONE 'UTC') AT TIME ZONE ?)"), "Should contain ISODOW+timezone expression");
      assertTrue(result.contains("% 7"), "Should contain modulo 7 for sunday-first sorting");
      assertEquals("EXTRACT(ISODOW FROM (create_date AT TIME ZONE 'UTC') AT TIME ZONE ?) % 7", result);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeWrapColumnNameForOrderByMondayFirst()
   {
      PostgreSQLRDBMSWeekdayOfDateTimeFunction adapter = new PostgreSQLRDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, false));

      String result = adapter.wrapColumnNameForOrderBy("create_date", ff, s -> s);
      assertTrue(result.contains("EXTRACT(ISODOW FROM (create_date AT TIME ZONE 'UTC') AT TIME ZONE ?)"), "Should contain ISODOW+timezone expression");
      assertFalse(result.contains("% 7"), "Should NOT contain modulo 7 for monday-first sorting");
      assertEquals("EXTRACT(ISODOW FROM (create_date AT TIME ZONE 'UTC') AT TIME ZONE ?)", result);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeGetParamsWithExplicitTimezone()
   {
      PostgreSQLRDBMSWeekdayOfDateTimeFunction adapter = new PostgreSQLRDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "America/Chicago"));

      List<Serializable> params = adapter.getParams(ff);
      assertEquals(1, params.size(), "Should have exactly one bind parameter");
      assertEquals("America/Chicago", params.get(0), "Explicit timezone should be used");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeGetParamsWithInstanceDefaultTimezone()
   {
      QContext.getQInstance().setDefaultTimeZoneId("America/New_York");

      PostgreSQLRDBMSWeekdayOfDateTimeFunction adapter = new PostgreSQLRDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false));

      List<Serializable> params = adapter.getParams(ff);
      assertEquals(1, params.size(), "Should have exactly one bind parameter");
      assertEquals("America/New_York", params.get(0), "Instance default timezone should be used");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeGetParamsDefaultsToUTC()
   {
      // default QInstance has defaultTimeZoneId = "UTC"
      PostgreSQLRDBMSWeekdayOfDateTimeFunction adapter = new PostgreSQLRDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false));

      List<Serializable> params = adapter.getParams(ff);
      assertEquals(1, params.size(), "Should have exactly one bind parameter");
      assertEquals("UTC", params.get(0), "Default timezone should be UTC");
   }



   /***************************************************************************
    ** Unlike the RDBMS (MySQL) adapter which needs two params (UTC source + target),
    ** the PostgreSQL adapter needs only one param (the target timezone), because
    ** PostgreSQL's AT TIME ZONE handles UTC-stored timestamps natively.
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeGetParamsHasOnlyOneParam()
   {
      PostgreSQLRDBMSWeekdayOfDateTimeFunction adapter = new PostgreSQLRDBMSWeekdayOfDateTimeFunction();
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("createDate")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "Europe/London"));

      List<Serializable> params = adapter.getParams(ff);
      assertEquals(1, params.size(), "PostgreSQL adapter needs only one timezone bind parameter (unlike MySQL's two-param CONVERT_TZ)");
   }



   /***************************************************************************
    ** PostgreSQL uses the base RDBMS CHAR_LENGTH implementation (valid standard SQL).
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
    ** PostgreSQL uses the base RDBMS SUBSTRING implementation (valid standard SQL).
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

}
