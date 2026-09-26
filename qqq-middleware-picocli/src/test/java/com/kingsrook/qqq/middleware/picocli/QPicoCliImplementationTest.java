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

package com.kingsrook.qqq.middleware.picocli;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for the QPicoCliImplementation.
 **
 *******************************************************************************/
class QPicoCliImplementationTest
{
   private static final boolean VERBOSE  = true;
   private static final String  CLI_NAME = "cli-unit-test";



   /*******************************************************************************
    ** Fully rebuild the test-database before each test runs, for completely known state.
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      System.setProperty("picocli.ansi", "false");
      TestUtils.primeTestDatabase();
      QContext.init(TestUtils.defineInstance(), new QSession());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** test that w/ no arguments you just get usage.
    **
    *******************************************************************************/
   @Test
   public void test_noArgs()
   {
      TestOutput testOutput = testCli();
      assertTestOutputContains(testOutput, "Usage: " + CLI_NAME);
   }



   /*******************************************************************************
    ** test that --help gives you usage.
    **
    *******************************************************************************/
   @Test
   public void test_help()
   {
      TestOutput testOutput = testCli("--help");
      assertTestOutputContains(testOutput, "Usage: " + CLI_NAME);
      assertTestOutputContains(testOutput, "Commands:.*person");
   }



   /*******************************************************************************
    ** test the --verion argument
    **
    *******************************************************************************/
   @Test
   public void test_version()
   {
      TestOutput testOutput = testCli("--version");
      assertTestOutputContains(testOutput, CLI_NAME + " v1.0");
   }



   /*******************************************************************************
    ** Test that an unrecognized opttion gives an error
    **
    *******************************************************************************/
   @Test
   public void test_badOption()
   {
      String     badOption  = "--asdf";
      TestOutput testOutput = testCli(badOption);
      assertTestErrorContains(testOutput, "Unknown option: '" + badOption + "'");
      assertTestErrorContains(testOutput, "Usage: " + CLI_NAME);
   }



   /*******************************************************************************
    ** test the top-level --meta-data option
    **
    *******************************************************************************/
   @Test
   public void test_metaData()
   {
      TestOutput testOutput = testCli("--meta-data");
      JSONObject metaData   = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(metaData);

      assertTrue(metaData.has("tables"));
      JSONObject tables      = metaData.getJSONObject("tables");
      JSONObject personTable = tables.getJSONObject("person");
      assertEquals("person", personTable.getString("name"));
      assertEquals("Person", personTable.getString("label"));

      assertTrue(metaData.has("processes"));
      JSONObject processes    = metaData.getJSONObject("processes");
      JSONObject greetProcess = processes.getJSONObject("greet");
      assertEquals("greet", greetProcess.getString("name"));
      assertEquals("Greet", greetProcess.getString("label"));
      assertEquals("person", greetProcess.getString("tableName"));
   }



   /*******************************************************************************
    ** test giving a table-name, gives usage for that table
    **
    *******************************************************************************/
   @Test
   public void test_table()
   {
      TestOutput testOutput = testCli("person");
      assertTestOutputContains(testOutput, "Usage: " + CLI_NAME + " person \\[COMMAND\\]");
      assertTestOutputContains(testOutput, "Commands:.*query.*process");

      ///////////////////////////////////////////////////////
      // make sure that if there are no processes for the  //
      // table, that the processes sub-command isn't given //
      ///////////////////////////////////////////////////////
      QInstance qInstanceWithoutProcesses = TestUtils.defineInstance();
      qInstanceWithoutProcesses.setProcesses(new HashMap<>());
      qInstanceWithoutProcesses.getTable("person").withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
      testOutput = testCli(qInstanceWithoutProcesses, "person");
      assertTestOutputDoesNotContain(testOutput, "process");
   }



   /*******************************************************************************
    ** test unknown command under table, prints error and usage.
    **
    *******************************************************************************/
   @Test
   public void test_tableUnknownCommand()
   {
      String     badCommand = "qwuijibo";
      TestOutput testOutput = testCli("person", badCommand);
      assertTestErrorContains(testOutput, "Unmatched argument at index 1: '" + badCommand + "'");
      assertTestErrorContains(testOutput, "Usage: " + CLI_NAME + " person \\[COMMAND\\]");
   }



   /*******************************************************************************
    ** test requesting table meta-data
    **
    *******************************************************************************/
   @Test
   public void test_tableMetaData()
   {
      TestOutput testOutput = testCli("person", "meta-data");
      JSONObject metaData   = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(metaData);
      assertEquals(1, metaData.keySet().size(), "Number of top-level keys");
      JSONObject table = metaData.getJSONObject("table");
      assertEquals("person", table.getString("name"));
      assertEquals("Person", table.getString("label"));
      assertEquals("id", table.getString("primaryKeyField"));
      JSONObject fields = table.getJSONObject("fields");
      JSONObject field0 = fields.getJSONObject("id");
      assertEquals("id", field0.getString("name"));
      assertEquals("INTEGER", field0.getString("type"));
   }



   /*******************************************************************************
    ** test running a count on a table
    **
    *******************************************************************************/
   @Test
   public void test_tableCount()
   {
      TestOutput testOutput  = testCli("person", "count", "--criteria", "id NOT_EQUALS 3");
      JSONObject countResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(countResult);
      int count = countResult.getInt("count");
      assertEquals(4, count);

      testOutput = testCli("person", "count", "--criteria", "id EQUALS 3");
      countResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(countResult);
      count = countResult.getInt("count");
      assertEquals(1, count);
   }



   /*******************************************************************************
    ** test running a query on a table
    **
    *******************************************************************************/
   @Test
   public void test_tableQuery()
   {
      TestOutput testOutput  = testCli("person", "query", "--skip=1", "--limit=2", "--criteria", "id NOT_EQUALS 3");
      JSONObject queryResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(queryResult);
      JSONArray records = queryResult.getJSONArray("records");
      assertEquals(2, records.length());
      // query for id != 3, and skipping 1, expect to get back rows 2 & 4
      assertEquals(2, records.getJSONObject(0).getJSONObject("values").getInt("id"));
      assertEquals(4, records.getJSONObject(1).getJSONObject("values").getInt("id"));
   }



   /*******************************************************************************
    ** test running a "get single record" action (singleton query) on a table
    **
    *******************************************************************************/
   @Test
   public void test_tableGetNoIdGiven()
   {
      TestOutput testOutput = testCli("person", "get");
      assertTestOutputContains(testOutput, "Usage: " + CLI_NAME + " person get PARAM");
      assertTestOutputContains(testOutput, "Primary key value from the table");
   }



   /*******************************************************************************
    ** test running a "get single record" action (singleton query) on a table
    **
    *******************************************************************************/
   @Test
   public void test_tableGet()
   {
      TestOutput testOutput = testCli("person", "get", "1");
      JSONObject getResult  = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(getResult);
      assertEquals(1, getResult.getJSONObject("values").getInt("id"));
      assertEquals("Darin", getResult.getJSONObject("values").getString("firstName"));
   }



   /*******************************************************************************
    ** test running a "get single record" action (singleton query) on a table
    **
    *******************************************************************************/
   @Test
   public void test_tableGetMissingId()
   {
      TestOutput testOutput = testCli("person", "get", "1976");
      assertTestOutputContains(testOutput, "No Person found for Id: 1976");
   }



   /*******************************************************************************
    ** test running an insert w/o specifying any fields, prints usage
    **
    *******************************************************************************/
   @Test
   public void test_tableInsertNoFieldsPrintsUsage()
   {
      TestOutput testOutput = testCli("person", "insert");
      assertTestOutputContains(testOutput, "Usage: " + CLI_NAME + " person insert");
   }



   /*******************************************************************************
    ** test running an insert w/ fields as arguments
    **
    *******************************************************************************/
   @Test
   public void test_tableInsertFieldArguments()
   {
      TestOutput testOutput = testCli("person", "insert",
         "--field-firstName=Lucy",
         "--field-lastName=Lu",
         "--field-email=lucy@kingsrook.com");
      JSONObject insertResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(insertResult);
      assertEquals(1, insertResult.getJSONArray("records").length());
      assertEquals(6, insertResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id"));
   }



   /*******************************************************************************
    ** test running an insert w/ a mapping and json as an argument
    **
    *******************************************************************************/
   @Test
   public void test_tableInsertJsonObjectArgumentWithMapping()
   {
      String mapping = """
         --mapping={"firstName":"first","lastName":"ln","email":"email"}
         """;

      String jsonBody = """
         --jsonBody={"first":"Chester","ln":"Cheese","email":"chester@kingsrook.com"}
         """;

      TestOutput testOutput   = testCli("person", "insert", mapping, jsonBody);
      JSONObject insertResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(insertResult);
      assertEquals(1, insertResult.getJSONArray("records").length());
      assertEquals(6, insertResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id"));
      assertEquals("Chester", insertResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getString("firstName"));
      assertEquals("Cheese", insertResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getString("lastName"));
      assertEquals("chester@kingsrook.com", insertResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getString("email"));
   }



   /*******************************************************************************
    ** test running an insert w/ a mapping and json as a multi-record file
    **
    *******************************************************************************/
   @Test
   public void test_tableInsertJsonArrayFileWithMapping() throws IOException
   {
      String mapping = """
         --mapping={"firstName":"first","lastName":"ln","email":"email"}
         """;

      String jsonContents = """
         [
            {"first":"Charlie","ln":"Bear","email":"charlie-bear@kingsrook.com"},
            {"first":"Coco","ln":"Bean","email":"coco-bean@kingsrook.com"}
         ]
         """;

      File file = new File("/tmp/" + UUID.randomUUID() + ".json");
      file.deleteOnExit();
      FileUtils.writeStringToFile(file, jsonContents, StandardCharsets.UTF_8);

      TestOutput testOutput   = testCli("person", "insert", mapping, "--jsonFile=" + file.getAbsolutePath());
      JSONObject insertResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(insertResult);
      JSONArray records = insertResult.getJSONArray("records");
      assertEquals(2, records.length());
      assertEquals(6, insertResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id"));
      assertEquals(7, insertResult.getJSONArray("records").getJSONObject(1).getJSONObject("values").getInt("id"));
      assertEquals("Charlie", records.getJSONObject(0).getJSONObject("values").getString("firstName"));
      assertEquals("Bear", records.getJSONObject(0).getJSONObject("values").getString("lastName"));
      assertEquals("charlie-bear@kingsrook.com", records.getJSONObject(0).getJSONObject("values").getString("email"));
      assertEquals("Coco", records.getJSONObject(1).getJSONObject("values").getString("firstName"));
      assertEquals("Bean", records.getJSONObject(1).getJSONObject("values").getString("lastName"));
      assertEquals("coco-bean@kingsrook.com", records.getJSONObject(1).getJSONObject("values").getString("email"));
   }



   /*******************************************************************************
    ** test running an insert w/ an index-based mapping and csv file
    **
    *******************************************************************************/
   @Test
   public void test_tableInsertCsvFileWithIndexMapping() throws IOException
   {
      String mapping = """
         --mapping={"firstName":1,"lastName":3,"email":5}
         """;

      String csvContents = """
         "Louis","P","Willikers",1024,"louis@kingsrook.com",
         "Nestle","G","Crunch",1701,"nestle@kingsrook.com",
                  
         """;

      File file = new File("/tmp/" + UUID.randomUUID() + ".csv");
      file.deleteOnExit();
      FileUtils.writeStringToFile(file, csvContents, StandardCharsets.UTF_8);

      TestOutput testOutput   = testCli("person", "insert", mapping, "--csvFile=" + file.getAbsolutePath());
      JSONObject insertResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(insertResult);
      JSONArray records = insertResult.getJSONArray("records");
      assertEquals(2, records.length());
      assertEquals(6, insertResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id"));
      assertEquals(7, insertResult.getJSONArray("records").getJSONObject(1).getJSONObject("values").getInt("id"));
      assertEquals("Louis", records.getJSONObject(0).getJSONObject("values").getString("firstName"));
      assertEquals("Willikers", records.getJSONObject(0).getJSONObject("values").getString("lastName"));
      assertEquals("Nestle", records.getJSONObject(1).getJSONObject("values").getString("firstName"));
      assertEquals("Crunch", records.getJSONObject(1).getJSONObject("values").getString("lastName"));
   }



   /*******************************************************************************
    ** test running an update w/o specifying any fields, prints usage
    **
    *******************************************************************************/
   @Test
   public void test_tableUpdateNoFieldsPrintsUsage()
   {
      TestOutput testOutput = testCli("person", "update");
      assertTestOutputContains(testOutput, "Usage: " + CLI_NAME + " person update");
   }



   /*******************************************************************************
    ** test running an update w/o specifying any pkeys or criteria, prints usage
    **
    *******************************************************************************/
   @Test
   public void test_tableUpdateNoRecordsPrintsUsage()
   {
      TestOutput testOutput = testCli("person", "update", "--field-firstName=Lucy");
      assertTestOutputContains(testOutput, "Usage: " + CLI_NAME + " person update");
   }



   /*******************************************************************************
    ** test running an update w/ fields as arguments and one primary key
    **
    *******************************************************************************/
   @Test
   public void test_tableUpdateFieldArgumentsOnePrimaryKey() throws Exception
   {
      assertRowValueById("person", "first_name", "Garret", 5);
      TestOutput testOutput = testCli("person", "update",
         "--primaryKey=5",
         "--field-firstName=Lucy",
         "--field-lastName=Lu");
      JSONObject updateResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(updateResult);
      assertEquals(1, updateResult.getJSONArray("records").length());
      assertEquals(5, updateResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id"));
      assertRowValueById("person", "first_name", "Lucy", 5);
   }



   /*******************************************************************************
    ** test running an update w/ fields as arguments and multiple primary keys
    **
    *******************************************************************************/
   @Test
   public void test_tableUpdateFieldArgumentsManyPrimaryKeys() throws Exception
   {
      assertRowValueById("person", "first_name", "Tyler", 4);
      assertRowValueById("person", "first_name", "Garret", 5);
      TestOutput testOutput = testCli("person", "update",
         "--primaryKey=4,5",
         "--field-birthDate=1980-05-31",
         "--field-firstName=Lucy",
         "--field-lastName=Lu");
      JSONObject updateResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(updateResult);
      assertEquals(2, updateResult.getJSONArray("records").length());
      assertEquals(4, updateResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id"));
      assertEquals(5, updateResult.getJSONArray("records").getJSONObject(1).getJSONObject("values").getInt("id"));
      assertRowValueById("person", "first_name", "Lucy", 4);
      assertRowValueById("person", "first_name", "Lucy", 5);
   }



   /*******************************************************************************
    ** test running an update w/ fields as arguments and a criteria
    **
    *******************************************************************************/
   @Test
   public void test_tableUpdateFieldArgumentsCriteria() throws Exception
   {
      assertRowValueById("person", "first_name", "Tyler", 4);
      assertRowValueById("person", "first_name", "Garret", 5);
      TestOutput testOutput = testCli("person", "update",
         "--criteria",
         "id GREATER_THAN_OR_EQUALS 4",
         "--field-firstName=Lucy",
         "--field-lastName=Lu");
      JSONObject updateResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(updateResult);
      assertEquals(2, updateResult.getJSONArray("records").length());
      assertEquals(4, updateResult.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id"));
      assertEquals(5, updateResult.getJSONArray("records").getJSONObject(1).getJSONObject("values").getInt("id"));
      assertRowValueById("person", "first_name", "Lucy", 4);
      assertRowValueById("person", "first_name", "Lucy", 5);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRowValueById(String tableName, String columnName, String value, Integer id) throws Exception
   {
      TestUtils.runTestSql("SELECT " + columnName + " FROM " + tableName + " WHERE id=" + id, (rs ->
      {
         if(rs.next())
         {
            assertEquals(value, rs.getString(1));
         }
         else
         {
            fail("Row not found");
         }
      }));
   }



   /*******************************************************************************
    ** test running a delete without enough args
    **
    *******************************************************************************/
   @Test
   public void test_tableDeleteWithoutArgs() throws Exception
   {
      TestOutput testOutput = testCli("person", "delete");
      assertTestOutputContains(testOutput, "Usage: " + CLI_NAME + " person delete");
   }



   /*******************************************************************************
    ** test running a delete against a table
    **
    *******************************************************************************/
   @Test
   public void test_tableDelete() throws Exception
   {
      TestOutput testOutput   = testCli("person", "delete", "--primaryKey", "2,4");
      JSONObject deleteResult = JsonUtils.toJSONObject(testOutput.getOutput());
      assertNotNull(deleteResult);
      assertEquals(2, deleteResult.getInt("deletedRecordCount"));
      TestUtils.runTestSql("SELECT id FROM person", (rs ->
      {
         int rowsFound = 0;
         while(rs.next())
         {
            rowsFound++;
            assertTrue(rs.getInt(1) == 1 || rs.getInt(1) == 3 || rs.getInt(1) == 5);
         }
         assertEquals(3, rowsFound);
      }));
   }



   /*******************************************************************************
    ** test requesting the list of processes for a table
    **
    *******************************************************************************/
   @Test
   public void test_tableProcess()
   {
      TestOutput testOutput = testCli("person", "process");

      ////////////////////////////////////////////////
      // should list the processes under this table //
      ////////////////////////////////////////////////
      assertTestOutputContains(testOutput, "Commands.*greet");
   }



   /*******************************************************************************
    ** test trying to run a process, but giving an invalid name.
    **
    *******************************************************************************/
   @Test
   public void test_tableProcessUnknownName()
   {
      String     badProcessName = "not-a-process";
      TestOutput testOutput     = testCli("person", "process", badProcessName);
      assertTestErrorContains(testOutput, "Unmatched argument at index 2: '" + badProcessName + "'");
      assertTestErrorContains(testOutput, "Usage: " + CLI_NAME + " person process \\[COMMAND\\]");
   }



   /*******************************************************************************
    ** test running a process on a table
    **
    *******************************************************************************/
   @Test
   public void test_tableProcessGreetUsingCallbackForFields()
   {
      setStandardInputLines("Hi", "How are you?");
      TestOutput testOutput = testCli("person", "process", "greet");
      assertTestOutputContains(testOutput, "Please supply a value for the field.*Greeting Prefix");
      assertTestOutputContains(testOutput, "Hi X How are you?");
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportNoArgsExcel()
   {
      String     filename   = "/tmp/" + UUID.randomUUID() + ".xlsx";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename);
      assertTestOutputContains(testOutput, "Wrote 5 records to file " + filename);

      File file = new File(filename);
      assertTrue(file.exists());

      // todo - some day when we learn to read Excel, assert that we wrote as expected.

      deleteFile(file);
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportWithLimit() throws Exception
   {
      String     filename   = "/tmp/" + UUID.randomUUID() + ".csv";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename, "--limit=3");
      assertTestOutputContains(testOutput, "Wrote 3 records to file " + filename);

      File file = new File(filename);
      List<String> list = FileUtils.readLines(file, StandardCharsets.UTF_8);
      assertEquals(4, list.size());
      assertThat(list.get(0)).contains("""
         "Id","Create Date","Modify Date\"""");
      assertThat(list.get(1)).matches("""
         ^"1",.*"Darin.*""");
      assertThat(list.get(3)).matches("""
         ^"3",.*"Tim.*""");

      deleteFile(file);
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportWithCriteria() throws Exception
   {
      String     filename   = "/tmp/" + UUID.randomUUID() + ".csv";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename, "--criteria", "id NOT_EQUALS 3");
      assertTestOutputContains(testOutput, "Wrote 4 records to file " + filename);

      File file = new File(filename);
      List<String> list = FileUtils.readLines(file, StandardCharsets.UTF_8);
      assertEquals(5, list.size());
      assertThat(list.get(0)).contains("""
         "Id","Create Date","Modify Date\"""");
      assertThat(list.get(1)).matches("^\"1\",.*");
      assertThat(list.get(2)).matches("^\"2\",.*");
      assertThat(list.get(3)).matches("^\"4\",.*");
      assertThat(list.get(4)).matches("^\"5\",.*");

      deleteFile(file);
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   @Disabled("Not sure why failing sometimes in mvn...")
   public void test_tableExportWithoutFilename()
   {
      TestOutput testOutput = testCli("person", "export");
      assertTestErrorContains(testOutput, "Missing required option: '--filename=PARAM'");
      assertTestErrorContains(testOutput, "Usage: " + CLI_NAME + " person export");
      assertTestErrorContains(testOutput, "-f=PARAM");
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportNoFileExtension()
   {
      String     filename   = "/tmp/" + UUID.randomUUID();
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename);
      assertTestErrorContains(testOutput, "File name did not contain an extension");
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportBadFileType()
   {
      String     filename   = "/tmp/" + UUID.randomUUID() + ".docx";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename);
      assertTestErrorContains(testOutput, "Unsupported report format: docx.");
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportBadFilePath()
   {
      String     filename   = "/no-such/directory/" + UUID.randomUUID() + "report.csv";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename);
      assertTestErrorContains(testOutput, "No such file or directory");
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportBadFieldNams()
   {
      String     filename   = "/tmp/" + UUID.randomUUID() + ".csv";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename, "--fieldNames=foo");
      assertTestErrorContains(testOutput, "Field name foo was not found on the Person table");
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportBadFieldNames()
   {
      String     filename   = "/tmp/" + UUID.randomUUID() + ".csv";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename, "--fieldNames=foo,bar,baz");
      assertTestErrorContains(testOutput, "Fields names foo, bar, and baz were not found on the Person table");
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportGoodFieldNamesXslx() throws IOException
   {
      String     filename   = "/tmp/" + UUID.randomUUID() + ".xlsx";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename, "--fieldNames=id,lastName,birthDate");

      File file = new File(filename);
      assertTrue(file.exists());

      // todo - some day when we learn to read Excel, assert that we wrote as expected (with 3 columns)

      deleteFile(file);
   }



   /*******************************************************************************
    ** test exporting a table
    **
    *******************************************************************************/
   @Test
   public void test_tableExportGoodFieldNamesCSV() throws IOException
   {
      String     filename   = "/tmp/" + UUID.randomUUID() + ".csv";
      TestOutput testOutput = testCli("person", "export", "--filename=" + filename, "--fieldNames=id,lastName,birthDate");

      File file = new File(filename);
      List<String> list = FileUtils.readLines(file, StandardCharsets.UTF_8);
      assertEquals(6, list.size());
      assertThat(list.get(0)).isEqualTo("""
         "Id","Last Name","Birth Date\"""");
      assertThat(list.get(1)).isEqualTo("""
         "1","Kelkhoff","1980-05-31\"""");

      deleteFile(file);
   }



   /*******************************************************************************
    ** test running a process on a table
    **
    *******************************************************************************/
   @Test
   public void test_tableProcessGreetUsingOptionsForFields()
   {
      TestOutput testOutput = testCli("person", "process", "greet", "--field-greetingPrefix=Hello", "--field-greetingSuffix=World");
      assertTestOutputDoesNotContain(testOutput, "Please supply a value for the field");
      assertTestOutputContains(testOutput, "Hello X World");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertTestOutputContains(TestOutput testOutput, String expectedRegexSubstring)
   {
      if(!testOutput.getOutput().matches("(?s).*" + expectedRegexSubstring + ".*"))
      {
         fail("Expected output to contain this regex pattern:\n" + expectedRegexSubstring
            + "\nBut it did not.  The full output was:\n" + testOutput.getOutput());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertTestOutputDoesNotContain(TestOutput testOutput, String expectedRegexSubstring)
   {
      if(testOutput.getOutput().matches("(?s).*" + expectedRegexSubstring + ".*"))
      {
         fail("Expected output to not contain this regex pattern:\n" + expectedRegexSubstring
            + "\nBut it did.  The full output was:\n" + testOutput.getOutput());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertTestErrorContains(TestOutput testOutput, String expectedRegexSubstring)
   {
      if(!testOutput.getError().matches("(?s).*" + expectedRegexSubstring + ".*"))
      {
         fail("Expected error-output to contain this regex pattern:\n" + expectedRegexSubstring
            + "\nBut it did not.  The full error-output was:\n" + testOutput.getOutput());
      }
   }



   /*******************************************************************************
    ** delete a file, asserting that we did so.
    *******************************************************************************/
   private void deleteFile(File file)
   {
      assertTrue(file.delete());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private TestOutput testCli(String... args)
   {
      QInstance qInstance = QContext.getQInstance();
      return testCli(qInstance, args);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private TestOutput testCli(QInstance qInstance, String... args)
   {
      QPicoCliImplementation qPicoCliImplementation = new QPicoCliImplementation(qInstance);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      ByteArrayOutputStream errorStream  = new ByteArrayOutputStream();

      if(VERBOSE)
      {
         System.out.println("> " + CLI_NAME + (args == null ? "" : " " + StringUtils.join(" ", Arrays.stream(args).toList())));
      }

      qPicoCliImplementation.runCli(CLI_NAME, args, new PrintStream(outputStream, true), new PrintStream(errorStream, true));

      String output = outputStream.toString(StandardCharsets.UTF_8);
      String error  = errorStream.toString(StandardCharsets.UTF_8);

      if(VERBOSE)
      {
         System.out.println(output);
         System.err.println(error);
      }

      TestOutput testOutput = new TestOutput(output, error);
      return (testOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setStandardInputLines(String... lines)
   {
      StringBuilder stringBuilder = new StringBuilder();
      for(String line : lines)
      {
         stringBuilder.append(line);
         if(!line.endsWith("\n"))
         {
            stringBuilder.append("\n");
         }
      }
      ByteArrayInputStream stdin = new ByteArrayInputStream(stringBuilder.toString().getBytes(Charset.defaultCharset()));
      System.setIn(stdin);
   }

}
