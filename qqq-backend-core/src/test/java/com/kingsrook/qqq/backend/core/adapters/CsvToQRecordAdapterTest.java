/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.adapters;


import java.time.LocalDate;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QIndexBasedFieldMapping;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QKeyBasedFieldMapping;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for CsvToQRecordAdapter
 **
 *******************************************************************************/
class CsvToQRecordAdapterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_nullInput() throws QException
   {
      testExpectedToThrow(null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_emptyStringInput() throws QException
   {
      testExpectedToThrow("");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void testExpectedToThrow(String csv) throws QException
   {
      try
      {
         CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
         List<QRecord>       qRecords            = csvToQRecordAdapter.buildRecordsFromCsv(csv, TestUtils.defineTablePerson(), null);
         System.out.println(qRecords);
      }
      catch(IllegalArgumentException iae)
      {
         System.out.println("Threw expected exception");
         return;
      }

      fail("Didn't throw expected exception");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_emptyList() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      List<QRecord>       qRecords            = csvToQRecordAdapter.buildRecordsFromCsv(getPersonCsvHeader(), TestUtils.defineTablePerson(), null);
      assertNotNull(qRecords);
      assertTrue(qRecords.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getPersonCsvHeader()
   {
      return ("""
         "id","createDate","modifyDate","firstName","lastName","birthDate","email"\r
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getPersonCsvRow2()
   {
      return ("""
         "0","2021-10-26 14:39:37","2021-10-26 14:39:37","Jane","Doe","1981-01-01","john@doe.com"\r
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String getPersonCsvRow1()
   {
      return ("""
         "0","2021-10-26 14:39:37","2021-10-26 14:39:37","John","Doe","1980-01-01","john@doe.com"\r
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_oneRowStandardHeaderNoMapping() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      List<QRecord>       qRecords            = csvToQRecordAdapter.buildRecordsFromCsv(getPersonCsvHeader() + getPersonCsvRow1(), TestUtils.defineTablePerson(), null);
      assertNotNull(qRecords);
      assertEquals(1, qRecords.size());
      QRecord qRecord = qRecords.get(0);
      assertEquals("John", qRecord.getValue("firstName"));
      assertEquals("1980-01-01", qRecord.getValue("birthDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_twoRowsStandardHeaderNoMapping() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      List<QRecord>       qRecords            = csvToQRecordAdapter.buildRecordsFromCsv(getPersonCsvHeader() + getPersonCsvRow1() + getPersonCsvRow2(), TestUtils.defineTablePerson(), null);
      assertNotNull(qRecords);
      assertEquals(2, qRecords.size());
      QRecord qRecord1 = qRecords.get(0);
      assertEquals("John", qRecord1.getValue("firstName"));
      assertEquals("1980-01-01", qRecord1.getValue("birthDate"));
      QRecord qRecord2 = qRecords.get(1);
      assertEquals("Jane", qRecord2.getValue("firstName"));
      assertEquals("1981-01-01", qRecord2.getValue("birthDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_oneRowCustomKeyBasedMapping() throws QException
   {
      String csvCustomHeader = """
         "id","created","modified","first","last","birthday","email"\r
         """;

      QKeyBasedFieldMapping mapping = new QKeyBasedFieldMapping()
         .withMapping("id", "id")
         .withMapping("createDate", "created")
         .withMapping("modifyDate", "modified")
         .withMapping("firstName", "first")
         .withMapping("lastName", "last")
         .withMapping("birthDate", "birthday")
         .withMapping("email", "email");

      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      List<QRecord>       qRecords            = csvToQRecordAdapter.buildRecordsFromCsv(csvCustomHeader + getPersonCsvRow1(), TestUtils.defineTablePerson(), mapping);
      assertNotNull(qRecords);
      assertEquals(1, qRecords.size());
      QRecord qRecord = qRecords.get(0);
      assertEquals("John", qRecord.getValue("firstName"));
      assertEquals("1980-01-01", qRecord.getValue("birthDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_twoRowsCustomIndexBasedMapping() throws QException
   {
      int index = 1;
      QIndexBasedFieldMapping mapping = new QIndexBasedFieldMapping()
         .withMapping("id", index++)
         .withMapping("createDate", index++)
         .withMapping("modifyDate", index++)
         .withMapping("firstName", index++)
         .withMapping("lastName", index++)
         .withMapping("birthDate", index++)
         .withMapping("email", index++);

      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      List<QRecord>       qRecords            = csvToQRecordAdapter.buildRecordsFromCsv(getPersonCsvRow1() + getPersonCsvRow2(), TestUtils.defineTablePerson(), mapping);
      assertNotNull(qRecords);
      assertEquals(2, qRecords.size());
      QRecord qRecord1 = qRecords.get(0);
      assertEquals("John", qRecord1.getValue("firstName"));
      assertEquals("1980-01-01", qRecord1.getValue("birthDate"));
      QRecord qRecord2 = qRecords.get(1);
      assertEquals("Jane", qRecord2.getValue("firstName"));
      assertEquals("1981-01-01", qRecord2.getValue("birthDate"));
   }



   /*******************************************************************************
    ** In this test - we've got CSV data with duplicated header names.
    ** In our mapping, we're seeing the suffixes of " 2" and " 3" addd to those
    ** header names on the RHS.
    *******************************************************************************/
   @Test
   public void test_duplicatedColumnHeaders() throws QException
   {
      QKeyBasedFieldMapping mapping = new QKeyBasedFieldMapping()
         .withMapping("id", "id")
         .withMapping("createDate", "date")
         .withMapping("modifyDate", "date 2")
         .withMapping("firstName", "name")
         .withMapping("lastName", "name 2")
         .withMapping("birthDate", "date 3")
         .withMapping("email", "email");

      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      List<QRecord> qRecords = csvToQRecordAdapter.buildRecordsFromCsv("""
         id,date,date,name,name,date,email
         1,2022-06-26,2022-06-26,John,Doe,1980-01-01,john@kingsrook.com
         """, TestUtils.defineTablePerson(), mapping);
      assertNotNull(qRecords);
      assertEquals(1, qRecords.size());
      QRecord qRecord1 = qRecords.get(0);
      assertEquals("John", qRecord1.getValue("firstName"));
      assertEquals("Doe", qRecord1.getValue("lastName"));
      assertEquals("1980-01-01", qRecord1.getValue("birthDate"));
      assertEquals("2022-06-26", qRecord1.getValue("createDate"));
      assertEquals("2022-06-26", qRecord1.getValue("modifyDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testMakeHeadersUnique()
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      Assertions.assertEquals(List.of("A", "B", "C"), csvToQRecordAdapter.makeHeadersUnique(List.of("A", "B", "C")));
      Assertions.assertEquals(List.of("A", "B", "C", "C 2", "C 3"), csvToQRecordAdapter.makeHeadersUnique(List.of("A", "B", "C", "C", "C")));
      Assertions.assertEquals(List.of("C", "A", "C 2", "B", "C 3"), csvToQRecordAdapter.makeHeadersUnique(List.of("C", "A", "C", "B", "C")));
      Assertions.assertEquals(List.of("A", "B", "C", "C 2", "C 3"), csvToQRecordAdapter.makeHeadersUnique(List.of("A", "B", "C", "C 2", "C")));
      Assertions.assertEquals(List.of("A", "B", "C", "C 2", "C 3"), csvToQRecordAdapter.makeHeadersUnique(List.of("A", "B", "C", "C 2", "C 3")));
      // todo - this is what the method header comment means when it says we don't handle all cases well...
      //  Assertions.assertEquals(List.of("A", "B", "C", "C 2", "C 3"), csvToQRecordAdapter.makeHeadersUnique(List.of("A", "B", "C 2", "C", "C 3")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testByteOrderMarker() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // note - there's a zero-width non-breaking-space character (0xFEFF or some-such)                       //
      // at the start of this string!!  You may not be able to see it, depending on where you view this file. //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QRecord> records = csvToQRecordAdapter.buildRecordsFromCsv("""
         ﻿id,firstName
         1,John""", TestUtils.defineTablePerson(), null);

      assertEquals(1, records.get(0).getValueInteger("id"));
      assertEquals("John", records.get(0).getValueString("firstName"));
   }



   /*******************************************************************************
    ** Fix an IndexOutOfBounds that we used to throw.
    *******************************************************************************/
   @Test
   void testTooFewBodyColumns() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      List<QRecord> records = csvToQRecordAdapter.buildRecordsFromCsv("""
         id,firstName,lastName
         1,John""", TestUtils.defineTablePerson(), null);

      assertEquals(1, records.get(0).getValueInteger("id"));
      assertEquals("John", records.get(0).getValueString("firstName"));
      assertNull(records.get(0).getValueString("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testTooFewColumnsIndexMapping() throws QException
   {
      int index = 1;
      QIndexBasedFieldMapping mapping = new QIndexBasedFieldMapping()
         .withMapping("id", index++)
         .withMapping("firstName", index++)
         .withMapping("lastName", index++);

      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      List<QRecord>       records             = csvToQRecordAdapter.buildRecordsFromCsv("1,John", TestUtils.defineTablePerson(), mapping);

      assertEquals(1, records.get(0).getValueInteger("id"));
      assertEquals("John", records.get(0).getValueString("firstName"));
      assertNull(records.get(0).getValueString("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCaseSensitiveHeaders() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      csvToQRecordAdapter.buildRecordsFromCsv(new CsvToQRecordAdapter.InputWrapper()
         .withTable(TestUtils.defineTablePerson())
         .withCaseSensitiveHeaders(true)
         .withCsv("""
            id,FirstName,lastName
            1,John,Doe
            """));
      List<QRecord> records = csvToQRecordAdapter.getRecordList();

      assertEquals(1, records.get(0).getValueInteger("id"));
      assertNull(records.get(0).getValueString("firstName"));
      assertEquals("Doe", records.get(0).getValueString("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCaseInsensitiveHeaders() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      csvToQRecordAdapter.buildRecordsFromCsv(new CsvToQRecordAdapter.InputWrapper()
         .withTable(TestUtils.defineTablePerson())
         // this is default, so don't set it:  withCaseSensitiveHeaders(false)
         .withCsv("""
            id,FirstName,lastName,EMAIL
            1,John,Doe,john@doe.com
            """));
      List<QRecord> records = csvToQRecordAdapter.getRecordList();

      assertEquals(1, records.get(0).getValueInteger("id"));
      assertEquals("John", records.get(0).getValueString("firstName"));
      assertEquals("Doe", records.get(0).getValueString("lastName"));
      assertEquals("john@doe.com", records.get(0).getValueString("email"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_doCorrectValueTypes() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      csvToQRecordAdapter.buildRecordsFromCsv(new CsvToQRecordAdapter.InputWrapper()
         .withDoCorrectValueTypes(true)
         .withTable(TestUtils.defineTablePerson().withField(new QFieldMetaData("isEmployed", QFieldType.BOOLEAN)))
         .withCsv("""
            firstName,birthDate,isEmployed
            John,1/1/1980,true
            Paul,1970-06-15,Yes
            George,,anything-else
            """));
      List<QRecord> qRecords = csvToQRecordAdapter.getRecordList();

      QRecord qRecord = qRecords.get(0);
      assertEquals("John", qRecord.getValue("firstName"));
      assertEquals(LocalDate.parse("1980-01-01"), qRecord.getValue("birthDate"));
      assertEquals(true, qRecord.getValue("isEmployed"));

      qRecord = qRecords.get(1);
      assertEquals("Paul", qRecord.getValue("firstName"));
      assertEquals(LocalDate.parse("1970-06-15"), qRecord.getValue("birthDate"));
      assertEquals(true, qRecord.getValue("isEmployed"));

      qRecord = qRecords.get(2);
      assertEquals("George", qRecord.getValue("firstName"));
      assertNull(qRecord.getValue("birthDate"));
      assertEquals(false, qRecord.getValue("isEmployed"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildRecordsFromCsv_doCorrectValueTypesErrorsForUnparseable() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      csvToQRecordAdapter.buildRecordsFromCsv(new CsvToQRecordAdapter.InputWrapper()
         .withDoCorrectValueTypes(true)
         .withTable(TestUtils.defineTablePerson())
         .withCsv("""
            firstName,birthDate,favoriteShapeId
            John,1980,1
            Paul,1970-06-15,green
            """));
      List<QRecord> qRecords = csvToQRecordAdapter.getRecordList();

      QRecord qRecord = qRecords.get(0);
      assertEquals("John", qRecord.getValue("firstName"));
      assertThat(qRecord.getErrors()).hasSize(1);
      assertThat(qRecord.getErrors().get(0).toString()).isEqualTo("Error parsing line #1: Could not parse value [1980] to a local date");

      qRecord = qRecords.get(1);
      assertEquals("Paul", qRecord.getValue("firstName"));
      assertThat(qRecord.getErrors()).hasSize(1);
      assertThat(qRecord.getErrors().get(0).toString()).isEqualTo("Error parsing line #2: Value [green] could not be converted to an Integer.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCsvHeadersAsFields() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      csvToQRecordAdapter.buildRecordsFromCsv(new CsvToQRecordAdapter.InputWrapper()
         .withCsvHeadersAsFieldNames(true)
         .withCaseSensitiveHeaders(true)
         .withCsv("""
            firstName,birthDate,favoriteShapeId
            John,1980,1
            Paul,1970-06-15,green
            """));

      List<QRecord> qRecords = csvToQRecordAdapter.getRecordList();

      QRecord qRecord = qRecords.get(0);
      assertEquals("John", qRecord.getValue("firstName"));
      assertEquals("1980", qRecord.getValue("birthDate"));
      assertEquals("1", qRecord.getValue("favoriteShapeId"));

      qRecord = qRecords.get(1);
      assertEquals("Paul", qRecord.getValue("firstName"));
      assertEquals("1970-06-15", qRecord.getValue("birthDate"));
      assertEquals("green", qRecord.getValue("favoriteShapeId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCsvHeadersAsFieldsDuplicatedNames() throws QException
   {
      CsvToQRecordAdapter csvToQRecordAdapter = new CsvToQRecordAdapter();
      csvToQRecordAdapter.buildRecordsFromCsv(new CsvToQRecordAdapter.InputWrapper()
         .withCsvHeadersAsFieldNames(true)
         .withCaseSensitiveHeaders(true)
         .withCsv("""
            orderId,sku,sku
            10001,BASIC1,BASIC2
            """));

      List<QRecord> qRecords = csvToQRecordAdapter.getRecordList();

      QRecord qRecord = qRecords.get(0);
      assertEquals("10001", qRecord.getValue("orderId"));
      assertEquals("BASIC1", qRecord.getValue("sku"));
      assertEquals("BASIC2", qRecord.getValue("sku 2"));
   }

}
