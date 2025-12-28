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

package com.kingsrook.qqq.backend.javalin;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PVSValueFormatAndFields;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryModuleBackendVariantSetting;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.middleware.javalin.metadata.JavalinRouteProviderMetaData;
import com.kingsrook.qqq.middleware.javalin.routeproviders.ProcessBasedRouterPayload;
import com.kingsrook.qqq.middleware.javalin.routeproviders.authentication.SimpleRouteAuthenticator;
import org.apache.commons.io.IOUtils;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Utility methods for unit tests.
 **
 *******************************************************************************/
public class TestUtils
{
   public static final String BACKEND_NAME_MEMORY = "memory";

   public static final String TABLE_NAME_PERSON = "person";
   public static final String TABLE_NAME_PET    = "pet";

   public static final String MEMORY_BACKEND_WITH_VARIANTS_NAME = "memoryWithVariants";
   public static final String TABLE_NAME_MEMORY_VARIANT_OPTIONS = "memoryVariantOptions";
   public static final String TABLE_NAME_MEMORY_VARIANT_DATA    = "memoryVariantData";

   private static final String PERSON_JOIN_PET            = "personJoinPet";
   private static final String PERSON_JOIN_PARTNER_PERSON = "PersonJoinPartnerPerson";

   public static final String PROCESS_NAME_GREET_PEOPLE_INTERACTIVE = "greetInteractive";
   public static final String PROCESS_NAME_SIMPLE_SLEEP             = "simpleSleep";
   public static final String PROCESS_NAME_SIMPLE_THROW             = "simpleThrow";
   public static final String PROCESS_NAME_SLEEP_INTERACTIVE        = "sleepInteractive";

   public static final String PROCESS_NAME_PUTS_NULL_KEY_IN_MAP = "putsNullKeyInMap";

   public static final String STEP_NAME_SLEEPER = "sleeper";
   public static final String STEP_NAME_THROWER = "thrower";

   public static final String SCREEN_0 = "screen0";
   public static final String SCREEN_1 = "screen1";

   public static final String STATIC_SITE_PATH = "static-site";



   /*******************************************************************************
    ** Prime a test database (e.g., h2, in-memory)
    **
    *******************************************************************************/
   public static void primeTestDatabase() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineDefaultH2Backend()))
      {
         InputStream primeTestDatabaseSqlStream = TestUtils.class.getResourceAsStream("/prime-test-database.sql");
         assertNotNull(primeTestDatabaseSqlStream);
         List<String> lines = IOUtils.readLines(primeTestDatabaseSqlStream, StandardCharsets.UTF_8);
         lines = lines.stream().filter(line -> !line.startsWith("-- ")).toList();
         String joinedSQL = String.join("\n", lines);
         for(String sql : joinedSQL.split(";"))
         {
            QueryManager.executeUpdate(connection, sql);
         }
      }
   }



   /*******************************************************************************
    ** Run an SQL Query in the test database
    **
    *******************************************************************************/
   public static void runTestSql(String sql, QueryManager.ResultSetProcessor resultSetProcessor) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(defineDefaultH2Backend()))
      {
         QueryManager.executeStatement(connection, sql, resultSetProcessor);
      }
   }



   /*******************************************************************************
    ** Define the q-instance for testing (h2 rdbms and 'person' table)
    **
    *******************************************************************************/
   public static QInstance defineInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.setAuthentication(defineAuthentication());
      qInstance.addBackend(defineDefaultH2Backend());
      qInstance.addTable(defineTablePerson());
      qInstance.addTable(defineTablePet());
      qInstance.addJoin(definePersonJoinPartnerPerson());
      qInstance.addJoin(definePersonJoinPet());
      qInstance.addProcess(defineProcessGreetPeople());
      qInstance.addProcess(defineProcessGreetPeopleInteractive());
      qInstance.addProcess(defineProcessSimpleSleep());
      qInstance.addProcess(defineProcessScreenThenSleep());
      qInstance.addProcess(defineProcessPutsNullKeyInMap());
      qInstance.addProcess(defineProcessSimpleThrow());
      qInstance.addProcess(defineRouterProcess());
      qInstance.addReport(definePersonsReport());
      qInstance.addPossibleValueSource(definePossibleValueSourcePerson());
      defineWidgets(qInstance);

      defineMemoryBackendVariantUseCases(qInstance);

      List<JavalinRouteProviderMetaData> routeProviders = new ArrayList<>();
      routeProviders.add(new JavalinRouteProviderMetaData()
         .withHostedPath("/statically-served")
         .withFileSystemPath(STATIC_SITE_PATH));

      routeProviders.add(new JavalinRouteProviderMetaData()
         .withHostedPath("/protected-statically-served")
         .withFileSystemPath(STATIC_SITE_PATH)
         .withRouteAuthenticator(new QCodeReference(SimpleRouteAuthenticator.class)));

      routeProviders.add(new JavalinRouteProviderMetaData()
         .withHostedPath("/served-by-process/<pagePath>")
         .withMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"))
         .withProcessName("routerProcess"));

      routeProviders.add(new JavalinRouteProviderMetaData()
         .withHostedPath("/protected-served-by-process/<pagePath>")
         .withProcessName("routerProcess")
         .withRouteAuthenticator(new QCodeReference(SimpleRouteAuthenticator.class)));

      qInstance.withSupplementalMetaData(new QJavalinMetaData().withRouteProviders(routeProviders));

      qInstance.addBackend(defineMemoryBackend());
      try
      {
         new SavedViewsMetaDataProvider().defineAll(qInstance, defineMemoryBackend().getName(), null);
         new ScriptsMetaDataProvider().defineAll(qInstance, defineMemoryBackend().getName(), null);

         ////////////////////////////////////////////////////////////////////////////////////////////
         // the test for this was built at a time when script revision table had contents it       //
         // doesn't have that field anymore.  so, when memory backend module became stricter about //
         // only storing and returning actual fields, it became "convenient" to-re add this field. //
         // similarly tests use apiName & apiVersion, but those fields aren't in the table because //
         // the api module isn't available, so, add them to make tests work.                       //
         ////////////////////////////////////////////////////////////////////////////////////////////
         QTableMetaData table = qInstance.getTable("scriptRevision");
         table.addField(new QFieldMetaData("content", QFieldType.STRING));
         table.addField(new QFieldMetaData("apiName", QFieldType.STRING));
         table.addField(new QFieldMetaData("apiVersion", QFieldType.STRING));
         table.getSections().get(table.getSections().size() - 2).getFieldNames().addAll(List.of("content", "apiName", "apiVersion"));
      }
      catch(Exception e)
      {
         throw new IllegalStateException("Error adding script tables to instance");
      }

      defineApps(qInstance);

      return (qInstance);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static QProcessMetaData defineRouterProcess()
   {
      return (new QProcessMetaData()
         .withName("routerProcess")
         .withStep(new QBackendStepMetaData()
            .withName("step")
            .withCode(new QCodeReferenceLambda<BackendStep>((runBackendStepInput, runBackendStepOutput) ->
            {
               ProcessBasedRouterPayload processPayload = runBackendStepInput.getProcessPayload(ProcessBasedRouterPayload.class);
               String                    path           = processPayload.getPath();

               if(processPayload.getQueryParams().containsKey("requestedRedirect"))
               {
                  processPayload.setRedirectURL(processPayload.getQueryParams().get("requestedRedirect").get(0));
               }
               else
               {
                  String response = "So you've done a " + processPayload.getMethod() + " for: " + path;
                  if(processPayload.getQueryParams().containsKey("respondInBytes"))
                  {
                     processPayload.setResponseBytes(response.getBytes(StandardCharsets.UTF_8));
                  }
                  else if(processPayload.getQueryParams().containsKey("noResponse"))
                  {
                     ///////////////////////////////////////
                     // don't call any setResponse method //
                     ///////////////////////////////////////
                  }
                  else if(processPayload.getQueryParams().containsKey("doThrow"))
                  {
                     throw (new QException("Test Exception"));
                  }
                  else
                  {
                     processPayload.setResponseString(response);
                  }
                  processPayload.setResponseHeaders(Map.of("X-Test", "Yes, Test"));
                  processPayload.setStatusCode(200);
               }
               runBackendStepOutput.setProcessPayload(processPayload);
            }))
         ));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void defineApps(QInstance qInstance)
   {
      QAppMetaData childApp = new QAppMetaData()
         .withName("childApp")
         .withLabel("Child App")
         .withIcon(new QIcon().withName("child_friendly"))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withChild(qInstance.getProcess(PROCESS_NAME_GREET_PEOPLE_INTERACTIVE));
      qInstance.addApp(childApp);

      QAppMetaData exampleApp = new QAppMetaData()
         .withName("homeApp")
         .withLabel("Home App")
         .withIcon(new QIcon().withName("home"))
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withChild(childApp)
         .withChild(qInstance.getTable(TABLE_NAME_PERSON));
      qInstance.addApp(exampleApp);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineWidgets(QInstance qInstance)
   {
      qInstance.addWidget(new QWidgetMetaData()
         .withName(PersonsByCreateDateBarChart.class.getSimpleName())
         .withCodeReference(new QCodeReference(PersonsByCreateDateBarChart.class)));

      qInstance.addWidget(new QWidgetMetaData()
         .withName("timezoneWidget")
         .withType(WidgetType.HTML.getType())
         .withCodeReference(new QCodeReference(TimezoneWidgetRenderer.class)));

      qInstance.addWidget(new QWidgetMetaData()
         .withName(EchoWidgetRenderer.class.getSimpleName())
         .withType(WidgetType.HTML.getType())
         .withCodeReference(new QCodeReference(EchoWidgetRenderer.class)));
   }



   /*******************************************************************************
    ** Define the authentication used in standard tests - using 'mock' type.
    **
    *******************************************************************************/
   private static QAuthenticationMetaData defineAuthentication()
   {
      return new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK);
   }



   /*******************************************************************************
    ** Define the h2 rdbms backend
    **
    *******************************************************************************/
   public static RDBMSBackendMetaData defineDefaultH2Backend()
   {
      RDBMSBackendMetaData rdbmsBackendMetaData = new RDBMSBackendMetaData()
         .withVendor("h2")
         .withHostName("mem")
         .withDatabaseName("test_database")
         .withUsername("sa")
         .withPassword("");
      rdbmsBackendMetaData.setName("default");
      return (rdbmsBackendMetaData);
   }



   /*******************************************************************************
    ** Define the memory-only backend
    **
    *******************************************************************************/
   public static QBackendMetaData defineMemoryBackend()
   {
      return new QBackendMetaData()
         .withBackendType(MemoryBackendModule.class)
         .withName(BACKEND_NAME_MEMORY);
   }



   /*******************************************************************************
    ** Define the person table
    **
    *******************************************************************************/
   public static QTableMetaData defineTablePerson()
   {
      QTableMetaData qTableMetaData = new QTableMetaData()
         .withName(TABLE_NAME_PERSON)
         .withLabel("Person")
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("firstName", "lastName")
         .withBackendName(defineDefaultH2Backend().getName())
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date"))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date"))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING).withBackendName("first_name"))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING).withBackendName("last_name"))
         .withField(new QFieldMetaData("birthDate", QFieldType.DATE).withBackendName("birth_date"))
         .withField(new QFieldMetaData("partnerPersonId", QFieldType.INTEGER).withBackendName("partner_person_id").withPossibleValueSourceName(TABLE_NAME_PERSON))
         .withField(new QFieldMetaData("email", QFieldType.STRING))
         .withField(new QFieldMetaData("testScriptId", QFieldType.INTEGER).withBackendName("test_script_id"))
         .withField(new QFieldMetaData("photo", QFieldType.BLOB).withBackendName("photo"))
         .withField(new QFieldMetaData("photoFileName", QFieldType.STRING).withBackendName("photo_file_name"))
         .withField(new QFieldMetaData("licenseScanPdfUrl", QFieldType.STRING).withBackendName("license_scan_pdf_url"))

         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_PET).withLabel("Pets").withJoinPath(List.of(PERSON_JOIN_PET)))

         .withAssociation(new Association().withName("pets").withJoinName(PERSON_JOIN_PET).withAssociatedTableName(TABLE_NAME_PET))
         .withAssociatedScript(new AssociatedScript()
            .withFieldName("testScriptId")
            .withScriptTypeId(1)
            .withScriptTester(new QCodeReference(TestScriptAction.class)));

      qTableMetaData.withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(PersonTableCustomizer.class));
      qTableMetaData.withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(PersonTableCustomizer.class));

      qTableMetaData.getField("photo")
         .withIsHeavy(true)
         .withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD)
            .withValue(AdornmentType.FileDownloadValues.DEFAULT_MIME_TYPE, "image")
            .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FIELD, "photoFileName"));

      qTableMetaData.getField("licenseScanPdfUrl")
         .withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD)
            .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT, "License-%s.pdf")
            .withValue(AdornmentType.FileDownloadValues.FILE_NAME_FORMAT_FIELDS, new ArrayList<>(List.of("id"))));

      return (qTableMetaData);
   }



   /*******************************************************************************
    ** Define the pet table
    **
    *******************************************************************************/
   public static QTableMetaData defineTablePet()
   {
      QTableMetaData qTableMetaData = new QTableMetaData()
         .withName(TABLE_NAME_PET)
         .withLabel("Pet")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("name")
         .withBackendName(defineDefaultH2Backend().getName())
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date"))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date"))
         .withField(new QFieldMetaData("ownerPersonId", QFieldType.INTEGER).withBackendName("owner_person_id")
            .withPossibleValueSourceName(TABLE_NAME_PERSON)
            .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, "${input.email}")))
         )
         .withField(new QFieldMetaData("name", QFieldType.STRING).withBackendName("name"))
         .withField(new QFieldMetaData("species", QFieldType.STRING).withBackendName("species"));

      return (qTableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonTableCustomizer implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
      {
         return warnPostInsertOrUpdate(records);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
      {
         return warnPostInsertOrUpdate(records);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private List<QRecord> warnPostInsertOrUpdate(List<QRecord> records)
      {
         for(QRecord record : records)
         {
            if(Objects.requireNonNullElse(record.getValueString("firstName"), "").toLowerCase().contains("warn"))
            {
               record.addWarning(new QWarningMessage("Warning in firstName."));
            }
         }

         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QJoinMetaData definePersonJoinPartnerPerson()
   {
      return (new QJoinMetaData()
         .withLeftTable(TABLE_NAME_PERSON)
         .withRightTable(TABLE_NAME_PERSON)
         .withType(JoinType.MANY_TO_ONE)
         .withName(PERSON_JOIN_PARTNER_PERSON)
         .withJoinOn(new JoinOn("partnerPersonId", "id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QJoinMetaData definePersonJoinPet()
   {
      return (new QJoinMetaData()
         .withLeftTable(TABLE_NAME_PERSON)
         .withRightTable(TABLE_NAME_PET)
         .withType(JoinType.ONE_TO_MANY)
         .withName(PERSON_JOIN_PET)
         .withJoinOn(new JoinOn("id", "ownerPersonId")));
   }



   /*******************************************************************************
    ** Define the 'greet people' process
    *******************************************************************************/
   private static QProcessMetaData defineProcessGreetPeople()
   {
      return new QProcessMetaData()
         .withName("greet")
         .withTableName(TABLE_NAME_PERSON)
         .withStep(new QBackendStepMetaData()
            .withName("prepare")
            .withCode(new QCodeReference()
               .withName(MockBackendStep.class.getName())
               .withCodeType(QCodeType.JAVA))
            .withInputData(new QFunctionInputMetaData()
               .withRecordListMetaData(new QRecordListMetaData().withTableName(TABLE_NAME_PERSON))
               .withFieldList(List.of(
                  new QFieldMetaData("greetingPrefix", QFieldType.STRING),
                  new QFieldMetaData("greetingSuffix", QFieldType.STRING),
                  new QFieldMetaData("partnerPersonId", QFieldType.INTEGER).withBackendName("partner_person_id").withPossibleValueSourceName(TABLE_NAME_PERSON)
               )))
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withRecordListMetaData(new QRecordListMetaData()
                  .withTableName(TABLE_NAME_PERSON)
                  .withField(new QFieldMetaData("fullGreeting", QFieldType.STRING))
               )
               .withFieldList(List.of(new QFieldMetaData("outputMessage", QFieldType.STRING))))
         );
   }



   /*******************************************************************************
    ** Define an interactive version of the 'greet people' process
    *******************************************************************************/
   private static QProcessMetaData defineProcessGreetPeopleInteractive()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_GREET_PEOPLE_INTERACTIVE)
         .withTableName(TABLE_NAME_PERSON)

         .withStep(new QFrontendStepMetaData()
            .withName("setup")
            .withFormField(new QFieldMetaData("greetingPrefix", QFieldType.STRING))
            .withFormField(new QFieldMetaData("greetingSuffix", QFieldType.STRING))
         )

         .withStep(new QBackendStepMetaData()
            .withName("doWork")
            .withCode(new QCodeReference()
               .withName(MockBackendStep.class.getName())
               .withCodeType(QCodeType.JAVA))
            .withInputData(new QFunctionInputMetaData()
               .withRecordListMetaData(new QRecordListMetaData().withTableName(TABLE_NAME_PERSON))
               .withFieldList(List.of(
                  new QFieldMetaData("greetingPrefix", QFieldType.STRING),
                  new QFieldMetaData("greetingSuffix", QFieldType.STRING)
               )))
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withRecordListMetaData(new QRecordListMetaData()
                  .withTableName(TABLE_NAME_PERSON)
                  .withField(new QFieldMetaData("fullGreeting", QFieldType.STRING))
               )
               .withFieldList(List.of(new QFieldMetaData("outputMessage", QFieldType.STRING))))
         )

         .withStep(new QFrontendStepMetaData()
            .withName("results")
            .withFormField(new QFieldMetaData("outputMessage", QFieldType.STRING))
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QPossibleValueSource definePossibleValueSourcePerson()
   {
      return (new QPossibleValueSource()
         .withName(TABLE_NAME_PERSON)
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName(TABLE_NAME_PERSON)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_PARENS_ID)
         .withOrderByField("id"));
   }



   /*******************************************************************************
    ** Define a process with just one step that sleeps
    *******************************************************************************/
   private static QProcessMetaData defineProcessSimpleSleep()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_SIMPLE_SLEEP)
         .withIsHidden(true)
         .withStep(SleeperStep.getMetaData());
   }



   /*******************************************************************************
    ** Define a process with a screen, then a sleep step
    *******************************************************************************/
   private static QProcessMetaData defineProcessScreenThenSleep()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_SLEEP_INTERACTIVE)
         .withStep(new QFrontendStepMetaData()
            .withName(SCREEN_0)
            .withFormField(new QFieldMetaData("outputMessage", QFieldType.STRING)))
         .withStep(SleeperStep.getMetaData())
         .withStep(new QFrontendStepMetaData()
            .withName(SCREEN_1)
            .withFormField(new QFieldMetaData("outputMessage", QFieldType.STRING)));
   }



   /*******************************************************************************
    ** Define an interactive version of the 'greet people' process
    *******************************************************************************/
   private static QProcessMetaData defineProcessPutsNullKeyInMap()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_PUTS_NULL_KEY_IN_MAP)
         .withTableName(TABLE_NAME_PERSON)
         .withStep(new QBackendStepMetaData().withName("step")
            .withCode(new QCodeReferenceLambda<BackendStep>((runBackendStepInput, runBackendStepOutput) ->
            {
               HashMap<String, String> mapWithNullKey = new HashMap<>();
               mapWithNullKey.put(null, "hadNullKey");
               mapWithNullKey.put("one", "1");
               runBackendStepOutput.addValue("mapWithNullKey", mapWithNullKey);
            })));
   }



   /*******************************************************************************
    ** Define a process with just one step that sleeps and then throws
    *******************************************************************************/
   private static QProcessMetaData defineProcessSimpleThrow()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_SIMPLE_THROW)
         .withStep(ThrowerStep.getMetaData());
   }



   /*******************************************************************************
    ** Testing backend step - just sleeps however long you ask it to (or, throws if
    ** you don't provide a number of seconds to sleep).
    *******************************************************************************/
   public static class SleeperStep implements BackendStep
   {
      public static final String FIELD_SLEEP_MILLIS = "sleepMillis";



      /*******************************************************************************
       ** Execute the backend step - using the request as input, and the result as output.
       **
       ******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         try
         {
            Thread.sleep(runBackendStepInput.getValueInteger(FIELD_SLEEP_MILLIS));
            runBackendStepOutput.addValue("didSleep", true);
         }
         catch(InterruptedException e)
         {
            throw (new QException("Interrupted while sleeping..."));
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static QBackendStepMetaData getMetaData()
      {
         return (new QBackendStepMetaData()
            .withName(STEP_NAME_SLEEPER)
            .withCode(new QCodeReference()
               .withName(SleeperStep.class.getName())
               .withCodeType(QCodeType.JAVA))
            .withInputData(new QFunctionInputMetaData()
               .withField(new QFieldMetaData(SleeperStep.FIELD_SLEEP_MILLIS, QFieldType.INTEGER))));
      }
   }



   /*******************************************************************************
    ** Testing backend step - just throws an exception after however long you ask it to sleep.
    *******************************************************************************/
   public static class ThrowerStep implements BackendStep
   {
      public static final String FIELD_SLEEP_MILLIS = "sleepMillis";



      /*******************************************************************************
       ** Execute the backend step - using the request as input, and the result as output.
       **
       ******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         int sleepMillis;
         try
         {
            sleepMillis = runBackendStepInput.getValueInteger(FIELD_SLEEP_MILLIS);
         }
         catch(QValueException qve)
         {
            sleepMillis = 50;
         }

         try
         {
            Thread.sleep(sleepMillis);
         }
         catch(InterruptedException e)
         {
            throw (new QException("Interrupted while sleeping..."));
         }

         throw (new QException("I always throw."));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static QBackendStepMetaData getMetaData()
      {
         return (new QBackendStepMetaData()
            .withName(STEP_NAME_THROWER)
            .withCode(new QCodeReference()
               .withName(ThrowerStep.class.getName())
               .withCodeType(QCodeType.JAVA))
            .withInputData(new QFunctionInputMetaData()
               .withField(new QFieldMetaData(ThrowerStep.FIELD_SLEEP_MILLIS, QFieldType.INTEGER))));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QReportMetaData definePersonsReport()
   {
      return new QReportMetaData()
         .withName("personsReport")
         .withInputField(new QFieldMetaData("firstNamePrefix", QFieldType.STRING))
         .withDataSource(new QReportDataSource()
            .withSourceTable(TABLE_NAME_PERSON)
            .withQueryFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.STARTS_WITH, "${input.firstNamePrefix}")))
         )
         .withView(new QReportView()
            .withType(ReportType.TABLE)
            .withColumns(List.of(
               new QReportField("id"),
               new QReportField("firstName"),
               new QReportField("lastName")
            ))
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void insertRecords(QInstance qInstance, QTableMetaData table, List<QRecord> records) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(table.getName());
      insertInput.setRecords(records);
      new InsertAction().execute(insertInput);
   }



   /***************************************************************************
    * simple test widget, that echos back html (really just text) equal to the
    * "input" param that it receives
    ***************************************************************************/
   public static class EchoWidgetRenderer extends AbstractWidgetRenderer
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         return (new RenderWidgetOutput(new RawHTML("title",
            input.getQueryParams().get("input")
         )));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class TimezoneWidgetRenderer extends AbstractWidgetRenderer
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         return (new RenderWidgetOutput(new RawHTML("title",
            QContext.getQSession().getValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES)
            + "|" + QContext.getQSession().getValue(QSession.VALUE_KEY_USER_TIMEZONE)
         )));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void defineMemoryBackendVariantUseCases(QInstance qInstance)
   {
      qInstance.addBackend(new QBackendMetaData()
         .withName(MEMORY_BACKEND_WITH_VARIANTS_NAME)
         .withBackendType(MemoryBackendModule.class)
         .withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig()
            .withVariantTypeKey(TABLE_NAME_MEMORY_VARIANT_OPTIONS)
            .withOptionsTableName(TABLE_NAME_MEMORY_VARIANT_OPTIONS)
            .withBackendSettingSourceFieldNameMap(Map.of(MemoryModuleBackendVariantSetting.PRIMARY_KEY, "id"))
         ));

      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME_MEMORY_VARIANT_DATA)
         .withBackendName(MEMORY_BACKEND_WITH_VARIANTS_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
      );

      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME_MEMORY_VARIANT_OPTIONS)
         .withBackendName(BACKEND_NAME_MEMORY) // note, the version without variants!
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
      );
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static class TablePersonalizer implements TableMetaDataPersonalizerInterface
   {

      /***************************************************************************
       *
       ***************************************************************************/
      public static void register(QInstance qInstance)
      {
         qInstance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(TablePersonalizer.class));
      }



      /***************************************************************************
       *
       ***************************************************************************/
      public static void unregister(QInstance qInstance)
      {
         qInstance.getSupplementalCustomizers().remove(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE);
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput tableMetaDataPersonalizerInput) throws QException
      {
         if(QInputSource.USER.equals(tableMetaDataPersonalizerInput.getInputSource()))
         {
            QTableMetaData clone = tableMetaDataPersonalizerInput.getTable().clone();
            clone.getFields().remove("createDate");
            return clone;
         }

         //////////////////////////////////////////////////
         // by default, return the input table unchanged //
         //////////////////////////////////////////////////
         return (tableMetaDataPersonalizerInput.getTable());
      }
   }
}
