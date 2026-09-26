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

package com.kingsrook.qqq.backend.module.postgres.fieldfunctions;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Integration tests for field function SQL generation in the PostgreSQL backend.
 ** These tests execute real SQL against a live PostgreSQL container and verify results.
 *******************************************************************************/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgreSQLFieldFunctionSQLTest extends BaseTest
{

   /***************************************************************************
    ** Test data (from prime-test-database.sql):
    **   id=1  firstName='Darin'  (len=5)  birthDate=1980-05-31 (Saturday, ISO=6)
    **   id=2  firstName='James'  (len=5)  birthDate=1980-05-15 (Thursday, ISO=4)
    **   id=3  firstName='Tim'    (len=3)  birthDate=1976-05-28 (Friday,   ISO=5)
    **   id=4  firstName='Tyler'  (len=5)  birthDate=NULL
    **   id=5  firstName='Garret' (len=6)  birthDate=1981-01-01 (Thursday, ISO=4)
    ***************************************************************************/


   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testStringLengthAsFilterCriteria() throws Exception
   {
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, 5)
            .withFieldFunction(new FieldFunction()
               .withFieldName("firstName")
               .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER))));

      assertEquals(3, records.size(), "Darin(5), James(5), Tyler(5) have firstName length 5");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testStringLengthAsVirtualField() throws Exception
   {
      QVirtualFieldMetaData firstNameLengthField = new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withIsQueryCriteria(true)
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction()
            .withFieldName("firstName")
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(firstNameLengthField);

      ///////////////////////////////////////////////////////////////
      // filter: firstName length == 3 → only Tim                 //
      ///////////////////////////////////////////////////////////////
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("firstNameLength", QCriteriaOperator.EQUALS, 3)));

      assertEquals(1, records.size());
      assertEquals("Tim", records.get(0).getValueString("firstName"));
      assertEquals(3, records.get(0).getValueInteger("firstNameLength"));

      ////////////////////////////////////////////////////////////////
      // order by firstName length descending → Garret(6) first    //
      ////////////////////////////////////////////////////////////////
      records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("firstNameLength", false)));

      assertEquals(5, records.size());
      assertEquals("Garret", records.get(0).getValueString("firstName"));
      assertEquals(6, records.get(0).getValueInteger("firstNameLength"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSubStringAsFilterCriteria() throws Exception
   {
      ////////////////////////////////////////////////////////////////////
      // SUBSTRING(first_name FROM 1 FOR 3) == 'Dar' → only Darin      //
      ////////////////////////////////////////////////////////////////////
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Dar")
            .withFieldFunction(new FieldFunction()
               .withFieldName("firstName")
               .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
               .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1, SubStringFunction.LENGTH_PARAM, 3)))));

      assertEquals(1, records.size());
      assertEquals("Darin", records.get(0).getValueString("firstName"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testSubStringAsVirtualField() throws Exception
   {
      QVirtualFieldMetaData firstNameSub2Field = new QVirtualFieldMetaData("firstNameFrom2", QFieldType.STRING)
         .withIsQueryCriteria(true)
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction()
            .withFieldName("firstName")
            .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
            .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2)));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(firstNameSub2Field);

      ///////////////////////////////////////////////
      // SUBSTRING(first_name FROM 2) == 'im' → Tim
      ///////////////////////////////////////////////
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("firstNameFrom2", QCriteriaOperator.EQUALS, "im")));

      assertEquals(1, records.size());
      assertEquals("Tim", records.get(0).getValueString("firstName"));
      assertEquals("im", records.get(0).getValueString("firstNameFrom2"));

      /////////////////////////////////////////////
      // order by substring descending          //
      /////////////////////////////////////////////
      records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("firstNameFrom2", false)));

      assertEquals(5, records.size());
      // "yler" (Tyler) sorts last alphabetically descending → first here
      assertEquals("Tyler", records.get(0).getValueString("firstName"));
   }



   /***************************************************************************
    ** birthDate=1980-05-31 is a Saturday (ISO weekday 6): only Darin
    ***************************************************************************/
   @Test
   void testWeekdayOfDateAsFilterCriteria() throws Exception
   {
      int saturdayISO = LocalDate.of(1980, 5, 31).getDayOfWeek().getValue(); // = 6

      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, saturdayISO)
            .withFieldFunction(new FieldFunction()
               .withFieldName("birthDate")
               .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER))));

      assertEquals(1, records.size(), "Only Darin was born on a Saturday");
      assertEquals("Darin", records.get(0).getValueString("firstName"));
   }



   /***************************************************************************
    ** birthDate=1980-05-15 and 1981-01-01 are both Thursdays (ISO weekday 4)
    ***************************************************************************/
   @Test
   void testWeekdayOfDateMultipleMatchesAsFilterCriteria() throws Exception
   {
      int thursdayISO = LocalDate.of(1980, 5, 15).getDayOfWeek().getValue(); // = 4

      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("birthDate", QCriteriaOperator.EQUALS, thursdayISO)
            .withFieldFunction(new FieldFunction()
               .withFieldName("birthDate")
               .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER))));

      assertEquals(2, records.size(), "James (1980-05-15) and Garret (1981-01-01) were both born on Thursdays");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateAsVirtualField() throws Exception
   {
      QVirtualFieldMetaData birthWeekdayField = new QVirtualFieldMetaData("birthWeekday", QFieldType.INTEGER)
         .withIsQueryCriteria(true)
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction()
            .withFieldName("birthDate")
            .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(birthWeekdayField);

      ////////////////////////////////////////////////////////////////////
      // filter by Friday (ISO=5) → only Tim (born 1976-05-28, Friday) //
      ////////////////////////////////////////////////////////////////////
      int fridayISO = LocalDate.of(1976, 5, 28).getDayOfWeek().getValue(); // = 5

      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("birthWeekday", QCriteriaOperator.EQUALS, fridayISO)));

      assertEquals(1, records.size());
      assertEquals("Tim", records.get(0).getValueString("firstName"));
      assertEquals(fridayISO, records.get(0).getValueInteger("birthWeekday"));

      ///////////////////////////////////////////////////////////////////////////////////
      // order by birthWeekday ascending, Monday-first (no sundayFirst):               //
      // Thursday=4 (James, Garret), Friday=5 (Tim), Saturday=6 (Darin), NULL (Tyler) //
      ///////////////////////////////////////////////////////////////////////////////////
      records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("birthWeekday", true)));

      assertEquals(5, records.size());
      // NULL sorts last in ascending (PostgreSQL default), so non-null records come first
      // First non-null should be lowest weekday value (Thursday=4 → James or Garret)
      assertNotNull(records.get(0).getValueString("firstName"));
      int firstWeekday = records.get(0).getValueInteger("birthWeekday");
      assertEquals(4, firstWeekday, "Lowest weekday (Thursday=4) should sort first");
   }



   /***************************************************************************
    ** Insert records with known UTC timestamps and filter by weekday.
    ** Uses UTC timezone so the test is deterministic regardless of server locale.
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeAsFilterCriteria() throws Exception
   {
      //////////////////////////////////////////////////////////////////////////////
      // Clear existing persons and insert known records with specific timestamps //
      // Use raw SQL to bypass DynamicDefaultValueBehavior.CREATE_DATE           //
      //////////////////////////////////////////////////////////////////////////////
      new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_PERSON).withQueryFilter(new QQueryFilter()));

      // 2024-01-01T12:00:00Z = Monday in UTC  → ISO weekday 1
      // 2024-01-03T12:00:00Z = Wednesday in UTC → ISO weekday 3
      // 2024-01-07T12:00:00Z = Sunday in UTC  → ISO weekday 7
      runTestSql("INSERT INTO person (first_name, last_name, email, create_date) VALUES ('Monday', 'Person', 'mon@test.com', '2024-01-01 12:00:00')", null);
      runTestSql("INSERT INTO person (first_name, last_name, email, create_date) VALUES ('Wednesday', 'Person', 'wed@test.com', '2024-01-03 12:00:00')", null);
      runTestSql("INSERT INTO person (first_name, last_name, email, create_date) VALUES ('Sunday', 'Person', 'sun@test.com', '2024-01-07 12:00:00')", null);

      int mondayISO    = 1;
      int wednesdayISO = 3;
      int sundayISO    = 7;

      FieldFunction weekdayFF = new FieldFunction()
         .withFieldName("createDate")
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false,
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC"));

      ///////////////////////////////////////////////////////////////////
      // filter Monday → should match the Monday record               //
      ///////////////////////////////////////////////////////////////////
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("createDate", QCriteriaOperator.EQUALS, mondayISO)
            .withFieldFunction(weekdayFF)));

      assertEquals(1, records.size());
      assertEquals("Monday", records.get(0).getValueString("firstName"));

      ///////////////////////////////////////////////////////////////////
      // filter Wednesday                                              //
      ///////////////////////////////////////////////////////////////////
      records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("createDate", QCriteriaOperator.EQUALS, wednesdayISO)
            .withFieldFunction(weekdayFF)));

      assertEquals(1, records.size());
      assertEquals("Wednesday", records.get(0).getValueString("firstName"));

      ///////////////////////////////////////////////////////////////////
      // filter Sunday                                                 //
      ///////////////////////////////////////////////////////////////////
      records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("createDate", QCriteriaOperator.EQUALS, sundayISO)
            .withFieldFunction(weekdayFF)));

      assertEquals(1, records.size());
      assertEquals("Sunday", records.get(0).getValueString("firstName"));
   }



   /***************************************************************************
    ** Verify that the timezone parameter shifts which weekday is returned.
    ** A timestamp at 2024-01-01T02:00:00Z is:
    **   - Monday (ISO=1) in UTC
    **   - Sunday (ISO=7) in America/Chicago (UTC-6: 2024-01-01T02:00Z = 2023-12-31T20:00 local)
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeTimezoneShift() throws Exception
   {
      new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_PERSON).withQueryFilter(new QQueryFilter()));

      // 2024-01-01T02:00:00Z = Monday at 2am UTC = Sunday at 8pm in America/Chicago (UTC-6)
      // Use raw SQL to bypass DynamicDefaultValueBehavior.CREATE_DATE
      runTestSql("INSERT INTO person (first_name, last_name, email, create_date) VALUES ('TZShift', 'Person', 'tz@test.com', '2024-01-01 02:00:00')", null);

      FieldFunction utcFF = new FieldFunction()
         .withFieldName("createDate")
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false,
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC"));

      FieldFunction chicagoFF = new FieldFunction()
         .withFieldName("createDate")
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false,
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "America/Chicago"));

      ///////////////////////////////////////////////////////////
      // in UTC: 2024-01-01 02:00 → Monday → ISO weekday 1   //
      ///////////////////////////////////////////////////////////
      List<QRecord> utcRecords = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("createDate", QCriteriaOperator.EQUALS, 1)
            .withFieldFunction(utcFF)));
      assertEquals(1, utcRecords.size(), "2024-01-01T02:00Z is Monday in UTC");

      ////////////////////////////////////////////////////////////////////////
      // in America/Chicago: 2024-01-01 02:00 UTC = 2023-12-31 20:00 local //
      // → Sunday → ISO weekday 7                                           //
      ////////////////////////////////////////////////////////////////////////
      List<QRecord> chicagoRecords = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("createDate", QCriteriaOperator.EQUALS, 7)
            .withFieldFunction(chicagoFF)));
      assertEquals(1, chicagoRecords.size(), "2024-01-01T02:00Z is Sunday night in America/Chicago");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeAsVirtualField() throws Exception
   {
      new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_PERSON).withQueryFilter(new QQueryFilter()));

      // Use raw SQL to bypass DynamicDefaultValueBehavior.CREATE_DATE
      runTestSql("INSERT INTO person (first_name, last_name, email, create_date) VALUES ('Mon', 'P', 'm@t.com', '2024-01-01 12:00:00')", null);  // Monday
      runTestSql("INSERT INTO person (first_name, last_name, email, create_date) VALUES ('Fri', 'P', 'f@t.com', '2024-01-05 12:00:00')", null);   // Friday

      QVirtualFieldMetaData createWeekdayField = new QVirtualFieldMetaData("createWeekday", QFieldType.INTEGER)
         .withIsQueryCriteria(true)
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction()
            .withFieldName("createDate")
            .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
            .withArguments(Map.of(
               WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false,
               WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC")));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(createWeekdayField);

      ///////////////////////////////////////////////
      // filter Friday (ISO=5) → only "Fri" record //
      ///////////////////////////////////////////////
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter(
         new QFilterCriteria("createWeekday", QCriteriaOperator.EQUALS, 5)));

      assertEquals(1, records.size());
      assertEquals("Fri", records.get(0).getValueString("firstName"));
      assertEquals(5, records.get(0).getValueInteger("createWeekday"));

      /////////////////////////////////////////////////////////////////////////
      // order by weekday ascending → Monday(1) before Friday(5)            //
      /////////////////////////////////////////////////////////////////////////
      records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("createWeekday", true)));

      assertEquals(2, records.size());
      assertEquals("Mon", records.get(0).getValueString("firstName"));
      assertEquals("Fri", records.get(1).getValueString("firstName"));
   }


   /***************************************************************************
    ** Per-record zones and null fallback must agree for filtering and ordering.
    ***************************************************************************/
   @Test
   void testWeekdayOfDateTimeWithZoneFromField() throws Exception
   {
      new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_PERSON).withQueryFilter(new QQueryFilter()));
      runTestSql("ALTER TABLE person ADD COLUMN time_zone_id TEXT", null);
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withField(new QFieldMetaData("timeZoneId", QFieldType.STRING).withBackendName("time_zone_id"));
      runTestSql("INSERT INTO person (id, first_name, last_name, email, create_date, time_zone_id) VALUES "
         + "(101, 'Chicago', 'Person', 'chicago@example.invalid', '2024-01-01 02:00:00', 'America/Chicago'), "
         + "(102, 'UTC', 'Person', 'utc@example.invalid', '2024-01-01 02:00:00', 'UTC'), "
         + "(103, 'Fallback', 'Person', 'fallback@example.invalid', '2024-01-01 02:00:00', NULL)", null);

      FieldFunction weekdayFunction = new FieldFunction()
         .withFieldName("createDate")
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER);
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)
         .withVirtualField(new QVirtualFieldMetaData("createWeekday", QFieldType.INTEGER)
            .withIsQueryCriteria(true).withIsQuerySelectable(true).withFieldFunction(weekdayFunction));

      for(Boolean sundayFirst : List.of(false, true))
      {
         weekdayFunction.withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false,
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC",
            WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME, "timeZoneId",
            WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, sundayFirst));
         List<QRecord> sunday = QueryAction.execute(TestUtils.TABLE_NAME_PERSON,
            new QQueryFilter(new QFilterCriteria("createWeekday", QCriteriaOperator.EQUALS, 7)));
         assertEquals(List.of("Chicago"), sunday.stream().map(record -> record.getValueString("firstName")).toList());

         List<QRecord> ordered = QueryAction.execute(TestUtils.TABLE_NAME_PERSON, new QQueryFilter()
            .withOrderBy(new QFilterOrderBy("createWeekday", true)).withOrderBy(new QFilterOrderBy("id", true)));
         assertEquals(sundayFirst ? List.of("Chicago", "UTC", "Fallback") : List.of("UTC", "Fallback", "Chicago"),
            ordered.stream().map(record -> record.getValueString("firstName")).toList());
         assertEquals(sundayFirst ? List.of(7, 1, 1) : List.of(1, 1, 7),
            ordered.stream().map(record -> record.getValueInteger("createWeekday")).toList());
      }
   }

}
