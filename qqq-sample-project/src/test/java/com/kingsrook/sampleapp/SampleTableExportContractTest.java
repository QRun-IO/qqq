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

package com.kingsrook.sampleapp;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.reporting.ListOfMapsExportStreamer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Table exports read canonical native H2 data through every ReportFormat.
 *******************************************************************************/
class SampleTableExportContractTest
{
   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getTable("person").setExposedJoins(List.of(new ExposedJoin().withJoinTable("pet").withJoinPath(List.of("personJoinPet"))));
      QSession session = new QSession();
      session.setUser(new QUser());
      session.setPermissions(Set.of());
      QContext.init(instance, session);
      ListOfMapsExportStreamer.reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("DROP VIEW IF EXISTS owned_export_rows");
      }
      finally
      {
         ListOfMapsExportStreamer.reset();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** LIST_OF_MAPS remains a single-use test utility with static output storage.
    *******************************************************************************/
   @Test
   void testAllFormatsReturnCanonicalRows() throws Exception
   {
      for(ReportFormat format : ReportFormat.values())
      {
         OutputProbe output = new OutputProbe();
         assertEquals(5, new ExportAction().execute(input(format, output)).getRecordCount(), format.name());
         assertEquals(List.of("Avery", "Blair", "Casey", "Drew", "Morgan"), readNames(format, output), format.name());
         assertTrue(output.closes > 0, format.name());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testEmptyExportsRemainReadable() throws Exception
   {
      for(ReportFormat format : ReportFormat.values())
      {
         OutputProbe output = new OutputProbe();
         ExportInput input = input(format, output);
         input.getQueryFilter().addCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -1));
         assertEquals(0, new ExportAction().execute(input).getRecordCount(), format.name());
         assertEquals(List.of(), readNames(format, output), format.name());
         assertTrue(output.closes > 0, format.name());
      }
   }



   /*******************************************************************************
    ** Explicit selected IDs remain a restriction in every output format.
    *******************************************************************************/
   @Test
   void testSelectedRowsStaySelectedInEveryFormat() throws Exception
   {
      for(ReportFormat format : ReportFormat.values())
      {
         OutputProbe output = new OutputProbe();
         ExportInput input = input(format, output);
         input.getQueryFilter().addCriteria(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(2, 4)));
         assertEquals(2, new ExportAction().execute(input).getRecordCount(), format.name());
         assertEquals(List.of("Blair", "Drew"), readNames(format, output), format.name());
         assertTrue(output.closes > 0, format.name());
      }
   }



   /*******************************************************************************
    ** Automatically added possible-value labels count against XLSX column limits.
    *******************************************************************************/
   @Test
   void testSpreadsheetPreflightCountsPossibleValueColumns() throws Exception
   {
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.XLSX, output);
      input.setTableName("pet");
      input.setFieldNames(Collections.nCopies(ReportFormat.XLSX.getMaxCols() / 2 + 1, "personId"));
      assertThrows(QUserFacingException.class, () -> new ExportAction().preExecute(input));
      assertEquals(0, output.size());
      input.setFieldNames(Collections.nCopies(ReportFormat.XLSX.getMaxCols() / 2, "personId"));
      new ExportAction().preExecute(input);
      assertEquals(0, output.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHeaderFreeFileFormatsReturnAllRows() throws Exception
   {
      for(ReportFormat format : List.of(ReportFormat.XLSX, ReportFormat.JSON, ReportFormat.CSV, ReportFormat.TSV))
      {
         OutputProbe output = new OutputProbe();
         ExportInput input = input(format, output);
         input.setIncludeHeaderRow(false);
         assertEquals(5, new ExportAction().execute(input).getRecordCount());
         assertEquals(List.of("Avery", "Blair", "Casey", "Drew", "Morgan"), readNames(format, output, false), format.name());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testZeroLimitReturnsNoRowsInEveryFormat() throws Exception
   {
      for(ReportFormat format : ReportFormat.values())
      {
         OutputProbe output = new OutputProbe();
         ExportInput input = input(format, output);
         input.setLimit(0);
         assertEquals(0, new ExportAction().execute(input).getRecordCount());
         assertEquals(List.of(), readNames(format, output), format.name());
         assertTrue(output.closes > 0);
      }
   }



   /*******************************************************************************
    ** CSV normalizes line feeds; TSV uses backslash escapes; other formats retain values.
    *******************************************************************************/
   @Test
   void testSpecialCharactersFollowFormatContract() throws Exception
   {
      String value = "Comma, \"quote\"\\path\nline\tend";
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          PreparedStatement statement = connection.prepareStatement("UPDATE person SET first_name = ? WHERE id = 1"))
      {
         statement.setString(1, value);
         assertEquals(1, statement.executeUpdate());
      }
      for(ReportFormat format : ReportFormat.values())
      {
         OutputProbe output = new OutputProbe();
         ExportInput input = input(format, output);
         input.setLimit(1);
         assertEquals(1, new ExportAction().execute(input).getRecordCount());
         String expected = switch(format)
         {
            case CSV -> "Comma, \"quote\"\\path line\tend";
            case TSV -> "Comma, \"quote\"\\\\path\\nline\\tend";
            default -> value;
         };
         assertEquals(List.of(expected), readNames(format, output), format.name());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueLabelsAreExported() throws Exception
   {
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.CSV, output);
      input.setTableName("pet");
      input.setFieldNames(List.of("name", "personId"));
      input.getQueryFilter().addCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1));
      assertEquals(1, new ExportAction().execute(input).getRecordCount());
      try(CSVParser parser = CSVParser.parse(output.toString(StandardCharsets.UTF_8), CSVFormat.DEFAULT.withFirstRecordAsHeader()))
      {
         CSVRecord row = parser.getRecords().getFirst();
         String label = instance.getTable("pet").getField("personId").getLabel();
         assertEquals("Charlie", row.get("Name"));
         assertEquals("1", row.get(label));
         assertEquals("Avery Sample", row.get(label + " Name"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinedSpreadsheetRetainsEveryRow() throws Exception
   {
      List<String> expected = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement();
          ResultSet rows = statement.executeQuery("SELECT person.first_name FROM person LEFT JOIN pet ON person.id = pet.person_id ORDER BY person.id, pet.id"))
      {
         while(rows.next())
         {
            expected.add(rows.getString(1));
         }
      }
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.XLSX, output);
      input.setFieldNames(List.of("firstName", "pet.name"));
      assertEquals(expected.size(), new ExportAction().execute(input).getRecordCount());
      assertEquals(expected, readNames(ReportFormat.XLSX, output));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinedReadIsRequiredBeforeOutput() throws Exception
   {
      instance.getTable("pet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.JSON, output);
      input.setFieldNames(List.of("firstName", "pet.name"));
      assertThrows(QPermissionDeniedException.class, () -> new ExportAction().execute(input));
      assertEquals(0, output.size());
      assertEquals(0, output.closes);
   }



   /*******************************************************************************
    ** Joined criteria need READ even when no joined column is exported.
    *******************************************************************************/
   @Test
   void testFilterOnlyJoinRequiresRead() throws Exception
   {
      instance.getTable("pet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.JSON, output);
      input.getQueryFilter().addCriteria(new QFilterCriteria("pet.name", QCriteriaOperator.EQUALS, "Charlie"));
      assertThrows(QPermissionDeniedException.class, () -> new ExportAction().execute(input));
      assertEquals(0, output.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUserPrivacyAppliesToExportedColumns() throws Exception
   {
      usePrivacy();
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.JSON, output);
      input.setFieldNames(List.of("firstName", "lastName", "pet.name"));
      new ExportAction().execute(input);
      JSONArray rows = new JSONArray(output.toString(StandardCharsets.UTF_8));
      assertFalse(rows.isEmpty());
      for(int index = 0; index < rows.length(); index++)
      {
         JSONObject row = rows.getJSONObject(index);
         assertFalse(row.has("firstName"));
         assertEquals("Sample", row.getString("lastName"));
         assertEquals("************", row.getString("pet:name"));
      }
      assertFalse(output.toString(StandardCharsets.UTF_8).contains("Charlie"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExplicitRevealRemainsAvailable() throws Exception
   {
      usePrivacy();
      instance.getTable("pet").getField("name").withFieldAdornment(new FieldAdornment().withType(AdornmentType.REVEAL));
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.JSON, output);
      input.setFieldNames(List.of("lastName", "pet.name"));
      input.getQueryFilter().addCriteria(new QFilterCriteria("pet.name", QCriteriaOperator.EQUALS, "Charlie"));
      assertEquals(1, new ExportAction().execute(input).getRecordCount());
      assertEquals("Charlie", new JSONArray(output.toString(StandardCharsets.UTF_8)).getJSONObject(0).getString("pet:name"));
   }



   /*******************************************************************************
    ** Trusted callers retain their explicit SYSTEM policy.
    *******************************************************************************/
   @Test
   void testSystemExportRetainsTrustedValues() throws Exception
   {
      usePrivacy();
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.JSON, output);
      input.setInputSource(QInputSource.SYSTEM);
      assertEquals(5, new ExportAction().execute(input).getRecordCount());
      assertEquals(List.of("Avery", "Blair", "Casey", "Drew", "Morgan"), readNames(ReportFormat.JSON, output));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownFieldIsRejectedDuringPreflight() throws Exception
   {
      OutputProbe output = new OutputProbe();
      ExportInput input = input(ReportFormat.JSON, output);
      input.setFieldNames(List.of("ownedMissingField"));
      assertThrows(QUserFacingException.class, () -> new ExportAction().preExecute(input));
      assertEquals(0, output.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void useLargeSource(int rows) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
          Statement statement = connection.createStatement())
      {
         statement.execute("CREATE OR REPLACE VIEW owned_export_rows AS SELECT X AS id, 'Avery' AS first_name, 'Sample' AS last_name FROM SYSTEM_RANGE(1," + rows + ")");
      }
      QTableMetaData table = instance.getTable("person");
      table.getFields().keySet().retainAll(Set.of("id", "firstName", "lastName"));
      table.setBackendDetails(new RDBMSTableBackendDetails().withTableName("owned_export_rows"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ExportInput input(ReportFormat format, OutputProbe output)
   {
      ExportInput input = new ExportInput();
      input.setTableName("person");
      input.setInputSource(QInputSource.USER);
      input.setFieldNames(List.of("firstName", "lastName"));
      input.setQueryFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      input.setReportDestination(new ReportDestination().withReportFormat(format).withReportOutputStream(output));
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> readNames(ReportFormat format, OutputProbe output) throws Exception
   {
      return readNames(format, output, true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> readNames(ReportFormat format, OutputProbe output, boolean includeHeader) throws Exception
   {
      List<String> names = new ArrayList<>();
      switch(format)
      {
         case CSV, TSV ->
         {
            CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(format == ReportFormat.CSV ? ',' : '\t');
            csvFormat = includeHeader ? csvFormat.withFirstRecordAsHeader() : csvFormat.withHeader("First Name", "Last Name");
            try(CSVParser parser = CSVParser.parse(output.toString(StandardCharsets.UTF_8), csvFormat))
            {
               for(CSVRecord row : parser)
               {
                  names.add(row.get("First Name"));
               }
            }
         }
         case JSON ->
         {
            JSONArray rows = new JSONArray(output.toString(StandardCharsets.UTF_8));
            for(int index = 0; index < rows.length(); index++)
            {
               names.add(rows.getJSONObject(index).getString("firstName"));
            }
         }
         case XLSX ->
         {
            try(XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray())))
            {
               for(int row = includeHeader ? 1 : 0; row <= workbook.getSheetAt(0).getLastRowNum(); row++)
               {
                  names.add(workbook.getSheetAt(0).getRow(row).getCell(0).getStringCellValue());
               }
            }
         }
         case LIST_OF_MAPS -> ListOfMapsExportStreamer.getList("Sheet 1").forEach(row -> names.add(row.get("First Name")));
         default -> throw new AssertionError("Unverified report format: " + format);
      }
      return names;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void usePrivacy()
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(ExportPrivacy.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ExportPrivacy implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() == QInputSource.USER && Set.of("person", "pet").contains(input.getTableName()))
         {
            QTableMetaData table = input.getTable().clone();
            if("person".equals(input.getTableName()))
            {
               table.getField("firstName").setIsHidden(true);
            }
            else
            {
               table.getField("name").setType(QFieldType.PASSWORD);
            }
            return table;
         }
         return input.getTable();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ExportRows implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() == QInputSource.USER && "person".equals(input.getTableName()))
         {
            return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withFieldName("id")
               .withSecurityKeyType("exportOwner").withLockScope(RecordSecurityLock.LockScope.READ)
               .withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
         }
         return input.getTable();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class OutputProbe extends ByteArrayOutputStream
   {
      private int closes;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void close()
      {
         closes++;
      }
   }
}
