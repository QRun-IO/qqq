/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.permissions.CustomPermissionChecker;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.middleware.picocli.QPicoCliImplementation;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** CLI workflows over the owned sample database, checked independently with SQL.
 *******************************************************************************/
class SampleCliContractTest
{
   private static Set<String> grants = Set.of();
   private final AtomicInteger calls = new AtomicInteger();
   private QInstance instance;
   private QSession caller;
   @TempDir
   Path directory;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      grants = Set.of();
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(OwnedIdentity.class));
      instance.addProcess(new QProcessMetaData().withName("ownedCli")
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withStep(new QBackendStepMetaData().withName("run")
            .withInputData(new QFunctionInputMetaData().withFieldList(List.of(new QFieldMetaData("amount", QFieldType.INTEGER))))
            .withOutputMetaData(new QFunctionOutputMetaData().withFieldList(List.of(new QFieldMetaData("result", QFieldType.INTEGER).withLabel("Result"))))
            .withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
            {
               calls.incrementAndGet();
               if(input.getValueInteger("amount") < 0)
               {
                  throw new QException("Owned CLI failure");
               }
               output.addValue("result", input.getValueInteger("amount") * 2);
            }))));
      new QInstanceEnricher(instance).enrich();
      caller = new QSession().withPermissions("caller-only");
      QContext.init(instance, caller);
      sql("DELETE FROM field_lab");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      grants = Set.of();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHelpMetadataAndInvalidArguments() throws Exception
   {
      String help = success("--help");
      assertTrue(help.contains("person") && help.contains("fieldLab") && help.contains("processes"), help);
      JSONObject metadata = new JSONObject(success("--meta-data"));
      assertTrue(metadata.getJSONObject("tables").has("fieldLab"));
      assertTrue(metadata.getJSONObject("processes").has("ownedCli"));
      assertTrue(success("processes").contains("ownedCli"));
      assertTrue(success("fieldLab", "meta-data").contains("decimalValue"));
      for(String[] args : List.of(new String[] { "unknown-command" }, new String[] { "fieldLab", "query", "--unknown" },
         new String[] { "fieldLab", "query", "--limit=wrong" }, new String[] { "fieldLab", "insert", "--field-longValue=wrong" },
         new String[] { "fieldLab", "insert", "--field-dateValue=wrong" }, new String[] { "fieldLab", "export" }))
      {
         CliResult result = run(args);
         assertNotEquals(0, result.code(), result.output() + result.error());
         assertFalse(result.error().isBlank());
      }
      assertEquals(List.of(), names());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTypedCrudQueryCountAndNativeReadback() throws Exception
   {
      JSONObject inserted = new JSONObject(success("fieldLab", "insert", "--field-name=CLIΩ", "--field-longValue=9000000000",
         "--field-decimalValue=12.3456", "--field-booleanValue=true", "--field-dateValue=2026-09-24", "--field-timeValue=12:34:56",
         "--field-dateTimeValue=2026-09-24T12:34:56", "--field-textValue=Line Ω", "--field-htmlValue=<b>Owned</b>"));
      Integer id = inserted.getJSONArray("records").getJSONObject(0).getJSONObject("values").getInt("id");
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM field_lab"))
      {
         assertTrue(rows.next());
         assertEquals(id, rows.getInt("id"));
         assertEquals(9000000000L, rows.getLong("long_value"));
         assertEquals(new BigDecimal("12.3456"), rows.getBigDecimal("decimal_value"));
         assertTrue(rows.getBoolean("boolean_value"));
         assertEquals(LocalDate.of(2026, 9, 24), rows.getObject("date_value", LocalDate.class));
         assertEquals(LocalTime.of(12, 34, 56), rows.getObject("time_value", LocalTime.class));
         assertEquals("Line Ω", rows.getString("text_value"));
         assertEquals("<b>Owned</b>", rows.getString("html_value"));
         assertFalse(rows.next());
      }
      assertEquals("CLIΩ", new JSONObject(success("fieldLab", "get", id.toString())).getJSONObject("values").getString("name"));
      assertEquals(1, new JSONObject(success("fieldLab", "count", "--criteria", "name EQUALS CLIΩ")).getInt("count"));
      assertEquals(1, new JSONObject(success("fieldLab", "query", "--criteria", "name EQUALS CLIΩ", "--limit=1", "--skip=0")).getJSONArray("records").length());
      success("fieldLab", "update", "--primaryKey=" + id, "--field-name=Updated");
      assertEquals(List.of("Updated"), names());
      success("fieldLab", "delete", "--primaryKey=" + id);
      assertEquals(List.of(), names());
      assertNotEquals(0, run("fieldLab", "get", id.toString()).code());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJsonCsvMappingAndCriteriaMutations() throws Exception
   {
      success("fieldLab", "insert", "--mapping={\"name\":\"label\"}", "--jsonBody={\"label\":\"Body\"}");
      Path json = directory.resolve("rows.json");
      Files.writeString(json, "[{\"label\":\"File Ω\"}]");
      success("fieldLab", "insert", "--mapping={\"name\":\"label\"}", "--jsonFile=" + json);
      Path csv = directory.resolve("rows.csv");
      Files.writeString(csv, "label\nCSV\n");
      success("fieldLab", "insert", "--mapping={\"name\":\"label\"}", "--csvFile=" + csv);
      assertEquals(List.of("Body", "File Ω", "CSV"), names());
      success("fieldLab", "update", "--criteria", "name EQUALS Body", "--field-name=Mapped");
      success("fieldLab", "delete", "--criteria", "name EQUALS CSV");
      assertEquals(List.of("Mapped", "File Ω"), names());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCsvAndExcelExportsContainSelectedNativeRows() throws Exception
   {
      sql("INSERT INTO field_lab (name) VALUES ('ExportΩ'), ('Other')");
      for(String extension : List.of("csv", "xlsx"))
      {
         Path file = directory.resolve("owned." + extension);
         String output = success("fieldLab", "export", "--filename=" + file, "--fieldNames=name", "--criteria", "name EQUALS ExportΩ");
         assertTrue(output.contains("Wrote 1 records"), output);
         if(extension.equals("csv"))
         {
            String contents = Files.readString(file);
            assertTrue(contents.contains("ExportΩ"), contents);
            assertFalse(contents.contains("Other"));
         }
         else
         {
            try(XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file)))
            {
               assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
               assertEquals("ExportΩ", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
            }
         }
      }
      assertEquals(List.of("ExportΩ", "Other"), names());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessResultAndFailureExit() throws Exception
   {
      String output = success("processes", "ownedCli", "--field-amount=3");
      assertTrue(output.contains("Result: 6"), output);
      assertEquals(1, calls.get());
      CliResult failed = run("processes", "ownedCli", "--field-amount=-1");
      assertNotEquals(0, failed.code(), failed.output() + failed.error());
      assertTrue((failed.output() + failed.error()).contains("Exception"), failed.output() + failed.error());
      assertEquals(2, calls.get());
   }



   /*******************************************************************************
    ** Configured object permissions must govern CLI requests as well as metadata.
    *******************************************************************************/
   @Test
   void testDeniedTableAndProcessActions() throws Exception
   {
      sql("INSERT INTO field_lab (id, name) VALUES (1, 'Protected')");
      instance.getTable("fieldLab").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getProcess("ownedCli").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      List<String> violations = new ArrayList<>();
      for(String[] args : List.of(new String[] { "fieldLab", "count" }, new String[] { "fieldLab", "get", "1" },
         new String[] { "fieldLab", "query" }, new String[] { "fieldLab", "export", "--filename=" + directory.resolve("denied.csv") },
         new String[] { "fieldLab", "insert", "--field-name=Denied" }, new String[] { "fieldLab", "update", "--primaryKey=1", "--field-name=Denied" },
         new String[] { "fieldLab", "delete", "--primaryKey=1" }, new String[] { "processes", "ownedCli", "--field-amount=3" }))
      {
         sql("DELETE FROM field_lab");
         sql("INSERT INTO field_lab (id, name) VALUES (1, 'Protected')");
         CliResult denied = run(args);
         if(denied.code().equals(0) || !names().equals(List.of("Protected")))
         {
            violations.add(String.join(" ", args) + ": exit=" + denied.code() + ", rows=" + names() + ", output=" + denied.output());
         }
      }
      assertEquals(List.of(), violations);
      assertFalse(Files.exists(directory.resolve("denied.csv")));
      assertEquals(0, calls.get());
      grants = Set.of("fieldLab.read", "fieldLab.write", "ownedCli.hasAccess");
      assertEquals(1, new JSONObject(success("fieldLab", "count")).getInt("count"));
      success("fieldLab", "update", "--primaryKey=1", "--field-name=Allowed");
      assertEquals(List.of("Allowed"), names());
      success("processes", "ownedCli", "--field-amount=3");
      assertEquals(1, calls.get());
   }



   /*******************************************************************************
    ** Custom policy and USER input remain authoritative after standard grants.
    *******************************************************************************/
   @Test
   void testCustomPermissionCheckerAndUserInput() throws Exception
   {
      sql("INSERT INTO field_lab (id, name) VALUES (1, 'Protected')");
      instance.getTable("fieldLab").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withCustomPermissionChecker(new QCodeReference(OwnedChecker.class)));
      instance.getProcess("ownedCli").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
         .withCustomPermissionChecker(new QCodeReference(OwnedChecker.class)));
      grants = Set.of("fieldLab.hasAccess", "ownedCli.hasAccess");
      assertNotEquals(0, run("fieldLab", "count").code());
      assertNotEquals(0, run("fieldLab", "update", "--primaryKey=1", "--field-name=Denied").code());
      assertNotEquals(0, run("processes", "ownedCli", "--field-amount=3").code());
      assertEquals(List.of("Protected"), names());
      assertEquals(0, calls.get());
      grants = Set.of("owned.override");
      assertEquals(1, new JSONObject(success("fieldLab", "count")).getInt("count"));
      success("fieldLab", "update", "--primaryKey=1", "--field-name=CustomAllowed");
      success("processes", "ownedCli", "--field-amount=3");
      assertEquals(List.of("CustomAllowed"), names());
      assertEquals(1, calls.get());
   }



   /*******************************************************************************
    ** A readable base table does not grant criteria access to a protected join.
    *******************************************************************************/
   @Test
   void testJoinedCriteriaPermissions() throws Exception
   {
      instance.getTable("person").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("pet").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      grants = Set.of("person.read", "person.write");
      for(String command : List.of("count", "query", "update", "delete", "export"))
      {
         List<String> args = new ArrayList<>(List.of("person", command, "--criteria", "pet.name EQUALS Charlie"));
         if(command.equals("update"))
         {
            args.add("--field-firstName=Denied");
         }
         if(command.equals("export"))
         {
            args.add("--filename=" + directory.resolve("joined.csv"));
         }
         assertNotEquals(0, run(args.toArray(String[]::new)).code(), command);
      }
      assertFalse(Files.exists(directory.resolve("joined.csv")));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT first_name FROM person WHERE id=1"))
      {
         assertTrue(rows.next());
         assertEquals("Avery", rows.getString(1));
      }
      grants = Set.of("person.read", "person.write", "pet.read");
      assertEquals(1, new JSONObject(success("person", "count", "--criteria", "pet.name EQUALS Charlie")).getInt("count"));
      assertEquals(1, new JSONObject(success("person", "query", "--criteria", "pet.name EQUALS Charlie")).getJSONArray("records").length());
   }



   /*******************************************************************************
    ** Issue #548 remains a limitation, not evidence of correct multiword parsing.
    *******************************************************************************/
   @Test
   void testMultiwordCriteriaLimitation() throws Exception
   {
      sql("INSERT INTO field_lab (name) VALUES ('Multi'), ('Multi Word')");
      JSONObject row = new JSONObject(success("fieldLab", "query", "--criteria", "name EQUALS Multi Word"))
         .getJSONArray("records").getJSONObject(0).getJSONObject("values");
      assertEquals("Multi", row.getString("name"));
      assertEquals(List.of("Multi", "Multi Word"), names());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String success(String... args)
   {
      CliResult result = run(args);
      assertEquals(0, result.code(), result.output() + result.error());
      return result.output();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CliResult run(String... args)
   {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      ByteArrayOutputStream error = new ByteArrayOutputStream();
      try(PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8); PrintStream err = new PrintStream(error, true, StandardCharsets.UTF_8))
      {
         Integer code = new QPicoCliImplementation(instance).runCli("owned-sample", args, out, err);
         assertSame(instance, QContext.getQInstance());
         assertSame(caller, QContext.getQSession());
         return new CliResult(code, output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void sql(String command) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(command);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> names() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT name FROM field_lab ORDER BY id"))
      {
         List<String> result = new ArrayList<>();
         while(rows.next())
         {
            result.add(rows.getString(1));
         }
         return result;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record CliResult(Integer code, String output, String error)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OwnedChecker implements CustomPermissionChecker
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void checkPermissionsThrowing(AbstractActionInput input, MetaDataWithPermissionRules metadata) throws QPermissionDeniedException
      {
         if(input instanceof AbstractTableActionInput tableInput)
         {
            assertEquals(QInputSource.USER, tableInput.getInputSource());
         }
         else if(input instanceof RunProcessInput processInput)
         {
            assertEquals(QInputSource.USER, processInput.getInputSource());
         }
         if(!QContext.getQSession().hasPermission("owned.override"))
         {
            throw new QPermissionDeniedException("Owned CLI policy denied access");
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OwnedIdentity implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions(grants.toArray(String[]::new));
      }
   }
}
