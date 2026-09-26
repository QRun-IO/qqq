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

package com.kingsrook.qqq.backend.core.actions.values;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DateTimeDisplayValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QValueFormatter
 *******************************************************************************/
class QValueFormatterTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFormatValue()
   {
      assertNull(QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.COMMAS), null));

      assertEquals("1", QValueFormatter.formatValue(new QFieldMetaData(), 1));
      assertEquals("1", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat("%s"), 1));
      assertEquals("Hello", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat("%s"), "Hello"));

      assertEquals("1", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.COMMAS), 1));
      assertEquals("1,000", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.COMMAS), 1000));
      assertEquals("1000", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(null), 1000));
      assertEquals("$1,000.00", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.CURRENCY), 1000));
      assertEquals("1,000.00", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.DECIMAL2_COMMAS), 1000));
      assertEquals("1000.00", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.DECIMAL2), 1000));

      assertEquals("1", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.COMMAS), new BigDecimal("1")));
      assertEquals("1,000", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.COMMAS), new BigDecimal("1000")));
      assertEquals("1000", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.STRING), new BigDecimal("1000")));
      assertEquals("1000", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.STRING), 1000));

      assertEquals("1%", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.PERCENT), 1));
      assertEquals("1%", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.PERCENT), new BigDecimal("1.0")));
      assertEquals("1.0%", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.PERCENT_POINT1), 1));
      assertEquals("1.1%", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.PERCENT_POINT1), new BigDecimal("1.1")));
      assertEquals("1.1%", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.PERCENT_POINT1), new BigDecimal("1.12")));
      assertEquals("1.00%", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.PERCENT_POINT2), 1));
      assertEquals("1.10%", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.PERCENT_POINT2), new BigDecimal("1.1")));
      assertEquals("1.12%", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.PERCENT_POINT2), new BigDecimal("1.12")));

      assertNull(QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.BOOLEAN), null));
      assertEquals("Yes", QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.BOOLEAN), true));
      assertEquals("No", QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.BOOLEAN), false));
      assertEquals("Yes", QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.BOOLEAN), "true"));
      assertEquals("No", QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.BOOLEAN), "false"));
      assertEquals("true", QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.STRING), "true"));
      assertEquals("false", QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.STRING), "false"));

      assertNull(QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.TIME), null));
      assertEquals("5:00:00 AM", QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.TIME), LocalTime.of(5, 0)));
      assertEquals("5:00:47 PM", QValueFormatter.formatValue(new QFieldMetaData().withType(QFieldType.TIME), LocalTime.of(17, 0, 47)));

      //////////////////////////////////////////////////
      // this one flows through the exceptional cases //
      //////////////////////////////////////////////////
      assertEquals("1000.01", QValueFormatter.formatValue(new QFieldMetaData().withDisplayFormat(DisplayFormat.COMMAS), new BigDecimal("1000.01")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFormatRecordLabel()
   {
      QTableMetaData table = new QTableMetaData().withRecordLabelFormat("%s %s").withRecordLabelFields(List.of("firstName", "lastName"));
      assertEquals("Darin Kelkhoff", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("firstName", "Darin").withValue("lastName", "Kelkhoff")));
      assertEquals("Darin ", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("firstName", "Darin")));
      assertEquals("Darin ", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("firstName", "Darin").withValue("lastName", null)));

      table = new QTableMetaData().withRecordLabelFormat("%s " + DisplayFormat.CURRENCY).withRecordLabelFields("firstName", "price");
      assertEquals("Darin $10,000.00", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("firstName", "Darin").withValue("price", new BigDecimal(10000))));

      table = new QTableMetaData().withRecordLabelFormat(DisplayFormat.DEFAULT).withRecordLabelFields(List.of("id"));
      assertEquals("123456", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("id", "123456")));

      ///////////////////////////////////////////////////////
      // exceptional flow:  no recordLabelFormat specified //
      ///////////////////////////////////////////////////////
      table = new QTableMetaData().withPrimaryKeyField("id");
      assertEquals("42", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("id", 42)));

      ///////////////////////////////////////////////////////////////////////////////////////
      // exceptional flow:  no recordLabelFormat specified, and record already had a label //
      ///////////////////////////////////////////////////////////////////////////////////////
      table = new QTableMetaData().withPrimaryKeyField("id");
      assertEquals("my label", QValueFormatter.formatRecordLabel(table, new QRecord().withRecordLabel("my label").withValue("id", 42)));

      /////////////////////////////////////////////////
      // exceptional flow:  no fields for the format //
      /////////////////////////////////////////////////
      table = new QTableMetaData().withRecordLabelFormat("%s %s").withPrimaryKeyField("id");
      assertEquals("128", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("id", 128)));

      /////////////////////////////////////////////////////////
      // exceptional flow:  not enough fields for the format //
      /////////////////////////////////////////////////////////
      table = new QTableMetaData().withRecordLabelFormat("%s %s").withRecordLabelFields("a").withPrimaryKeyField("id");
      assertEquals("256", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("a", 47).withValue("id", 256)));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      // exceptional flow (kinda):  too many fields for the format (just get the ones that are in the format) //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////
      table = new QTableMetaData().withRecordLabelFormat("%s %s").withRecordLabelFields(List.of("a", "b", "c")).withPrimaryKeyField("id");
      assertEquals("47 48", QValueFormatter.formatRecordLabel(table, new QRecord().withValue("a", 47).withValue("b", 48).withValue("c", 49).withValue("id", 256)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetDisplayValuesInRecords()
   {
      QTableMetaData table = new QTableMetaData()
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("firstName", "lastName")
         .withField(new QFieldMetaData("firstName", QFieldType.STRING))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING))
         .withField(new QFieldMetaData("price", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.CURRENCY))
         .withField(new QFieldMetaData("quantity", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(new QFieldMetaData("homeStateId", QFieldType.INTEGER).withPossibleValueSourceName(TestUtils.POSSIBLE_VALUE_SOURCE_STATE));

      /////////////////////////////////////////////////////////////////
      // first, make sure it doesn't crash with null or empty inputs //
      /////////////////////////////////////////////////////////////////
      QValueFormatter.setDisplayValuesInRecords(table, null);
      QValueFormatter.setDisplayValuesInRecords(table, Collections.emptyList());

      List<QRecord> records = List.of(
         new QRecord()
            .withValue("firstName", "Tim")
            .withValue("lastName", "Chamberlain")
            .withValue("price", new BigDecimal("3.50"))
            .withValue("quantity", 1701)
            .withValue("homeStateId", 1),
         new QRecord()
            .withValue("firstName", "Tyler")
            .withValue("lastName", "Samples")
            .withValue("price", new BigDecimal("174999.99"))
            .withValue("quantity", 47)
            .withValue("homeStateId", 2)
      );

      QValueFormatter.setDisplayValuesInRecords(table, records);

      assertEquals("Tim Chamberlain", records.get(0).getRecordLabel());
      assertEquals("$3.50", records.get(0).getDisplayValue("price"));
      assertEquals("1,701", records.get(0).getDisplayValue("quantity"));
      assertEquals("1", records.get(0).getDisplayValue("homeStateId")); // PVS NOT translated by this class.

      assertEquals("Tyler Samples", records.get(1).getRecordLabel());
      assertEquals("$174,999.99", records.get(1).getDisplayValue("price"));
      assertEquals("47", records.get(1).getDisplayValue("quantity"));
      assertEquals("2", records.get(1).getDisplayValue("homeStateId")); // PVS NOT translated by this class.
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSetDisplayValuesInRecordsIncludingPossibleValueTranslations()
   {
      QTableMetaData table = new QTableMetaData()
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("firstName", "lastName")
         .withField(new QFieldMetaData("firstName", QFieldType.STRING))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING))
         .withField(new QFieldMetaData("price", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.CURRENCY))
         .withField(new QFieldMetaData("quantity", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(new QFieldMetaData("homeStateId", QFieldType.INTEGER).withPossibleValueSourceName(TestUtils.POSSIBLE_VALUE_SOURCE_STATE));

      /////////////////////////////////////////////////////////////////
      // first, make sure it doesn't crash with null or empty inputs //
      /////////////////////////////////////////////////////////////////
      QValueFormatter.setDisplayValuesInRecordsIncludingPossibleValueTranslations(table, null);
      QValueFormatter.setDisplayValuesInRecordsIncludingPossibleValueTranslations(table, Collections.emptyList());

      List<QRecord> records = List.of(
         new QRecord()
            .withValue("firstName", "Tim")
            .withValue("lastName", "Chamberlain")
            .withValue("price", new BigDecimal("3.50"))
            .withValue("quantity", 1701)
            .withValue("homeStateId", 1),
         new QRecord()
            .withValue("firstName", "Tyler")
            .withValue("lastName", "Samples")
            .withValue("price", new BigDecimal("174999.99"))
            .withValue("quantity", 47)
            .withValue("homeStateId", 2)
      );

      QValueFormatter.setDisplayValuesInRecordsIncludingPossibleValueTranslations(table, records);

      assertEquals("Tim Chamberlain", records.get(0).getRecordLabel());
      assertEquals("$3.50", records.get(0).getDisplayValue("price"));
      assertEquals("1,701", records.get(0).getDisplayValue("quantity"));
      assertEquals("IL", records.get(0).getDisplayValue("homeStateId")); // PVS is translated by this method.

      assertEquals("Tyler Samples", records.get(1).getRecordLabel());
      assertEquals("$174,999.99", records.get(1).getDisplayValue("price"));
      assertEquals("47", records.get(1).getDisplayValue("quantity"));
      assertEquals("MO", records.get(1).getDisplayValue("homeStateId")); // PVS is translated by this method.
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFormatDates()
   {
      assertEquals("2023-02-01", QValueFormatter.formatDate(LocalDate.of(2023, Month.FEBRUARY, 1)));
      assertEquals("2023-02-01 07:15:00 PM", QValueFormatter.formatDateTime(LocalDateTime.of(2023, Month.FEBRUARY, 1, 19, 15, 0)));
      assertEquals("2023-02-01 07:15:47 PM CST", QValueFormatter.formatDateTimeWithZone(ZonedDateTime.of(LocalDateTime.of(2023, Month.FEBRUARY, 1, 19, 15, 47), ZoneId.of("US/Central"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldDisplayBehaviors()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      table.withField(new QFieldMetaData("timeZone", QFieldType.STRING));
      table.getField("createDate").withBehavior(new DateTimeDisplayValueBehavior().withZoneIdFromFieldName("timeZone"));

      QRecord record = new QRecord().withValue("createDate", Instant.parse("2024-04-04T19:12:00Z")).withValue("timeZone", "America/Chicago");
      QValueFormatter.setDisplayValuesInRecords(table, List.of(record));
      assertEquals("2024-04-04 02:12:00 PM CDT", record.getDisplayValue("createDate"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBlobValuesToDownloadUrls()
   {
      byte[] blobBytes = "hello".getBytes();
      {
         QTableMetaData table = new QTableMetaData()
            .withName("testTable")
            .withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withField(new QFieldMetaData("blobField", QFieldType.BLOB)
               .withFieldAdornment(new FieldAdornment().withType(AdornmentType.FILE_DOWNLOAD)
                  .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT, "blob-%s.txt")
                  .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT_FIELDS, new ArrayList<>(List.of("id")))));

         //////////////////////////////////////////////////////////////////
         // verify display value gets set to formated file-name + fields //
         // and raw value becomes URL for downloading the byte           //
         //////////////////////////////////////////////////////////////////
         QRecord record = new QRecord().withValue("id", 47).withValue("blobField", blobBytes);
         QValueFormatter.setBlobValuesToDownloadUrls(table, List.of(record));
         assertEquals("/data/testTable/47/blobField/blob-47.txt", record.getValueString("blobField"));
         assertEquals("blob-47.txt", record.getDisplayValue("blobField"));

         ////////////////////////////////////////////////////////
         // verify that w/ no blob value, we don't do anything //
         ////////////////////////////////////////////////////////
         QRecord recordWithoutBlobValue = new QRecord().withValue("id", 47);
         QValueFormatter.setBlobValuesToDownloadUrls(table, List.of(recordWithoutBlobValue));
         assertNull(recordWithoutBlobValue.getValue("blobField"));
         assertNull(recordWithoutBlobValue.getDisplayValue("blobField"));
      }

      {
         FieldAdornment adornment = new FieldAdornment().withType(AdornmentType.FILE_DOWNLOAD)
            .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FIELD, "fileName");

         QTableMetaData table = new QTableMetaData()
            .withName("testTable")
            .withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withField(new QFieldMetaData("fileName", QFieldType.STRING))
            .withField(new QFieldMetaData("blobField", QFieldType.BLOB)
               .withFieldAdornment(adornment));

         ////////////////////////////////////////////////////
         // here get the file name directly from one field //
         ////////////////////////////////////////////////////
         QRecord record = new QRecord().withValue("id", 47).withValue("blobField", blobBytes).withValue("fileName", "myBlob.txt");
         QValueFormatter.setBlobValuesToDownloadUrls(table, List.of(record));
         assertEquals("/data/testTable/47/blobField/myBlob.txt", record.getValueString("blobField"));
         assertEquals("myBlob.txt", record.getDisplayValue("blobField"));

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // switch to use dynamic url, rerun, and assert we get the values as they were on the record before the call //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
         adornment.withValue(AdornmentType.FileDownloadValues.DOWNLOAD_URL_DYNAMIC, true);
         record = new QRecord().withValue("id", 47).withValue("blobField", blobBytes).withValue("fileName", "myBlob.txt")
            .withDisplayValue("blobField:" + AdornmentType.FileDownloadValues.DOWNLOAD_URL_DYNAMIC, "/something-custom/")
            .withDisplayValue("blobField", "myDisplayValue");
         QValueFormatter.setBlobValuesToDownloadUrls(table, List.of(record));
         assertArrayEquals(blobBytes, record.getValueByteArray("blobField"));
         assertEquals("myDisplayValue", record.getDisplayValue("blobField"));
      }

      {
         FieldAdornment adornment = new FieldAdornment().withType(AdornmentType.FILE_DOWNLOAD);

         QTableMetaData table = new QTableMetaData()
            .withName("testTable")
            .withLabel("Test Table")
            .withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withField(new QFieldMetaData("blobField", QFieldType.BLOB).withLabel("Blob").withFieldAdornment(adornment));

         ///////////////////////////////////////////////////////////////////////////////////////////
         // w/o file name format or whatever, generate a file name from table & id & field labels //
         ///////////////////////////////////////////////////////////////////////////////////////////
         QRecord record = new QRecord().withValue("id", 47).withValue("blobField", blobBytes);
         QValueFormatter.setBlobValuesToDownloadUrls(table, List.of(record));
         assertEquals("/data/testTable/47/blobField/Test%20Table%2047%20Blob", record.getValueString("blobField"));
         assertEquals("Test Table 47 Blob", record.getDisplayValue("blobField"));

         ////////////////////////////////////////
         // add a default extension and re-run //
         ////////////////////////////////////////
         adornment.withValue(AdornmentType.FileDownloadValues.DEFAULT_EXTENSION, "html");
         record = new QRecord().withValue("id", 47).withValue("blobField", blobBytes);
         QValueFormatter.setBlobValuesToDownloadUrls(table, List.of(record));
         assertEquals("/data/testTable/47/blobField/Test%20Table%2047%20Blob.html", record.getValueString("blobField"));
         assertEquals("Test Table 47 Blob.html", record.getDisplayValue("blobField"));
      }
   }

}