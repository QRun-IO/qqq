/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.sampleapp.metadata;


import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.amazonaws.regions.Regions;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.QuickSightChartRenderer;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerHelper;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QuickSightChartMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.RedirectStateMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.metadata.UserSessionMetaDataProducer;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaInsertStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.general.LoadInitialRecordsStep;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.dashboard.widgets.PersonsByCreateDateBarChart;
import com.kingsrook.sampleapp.processes.clonepeople.ClonePeopleTransformStep;
import org.apache.commons.io.IOUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class SampleMetaDataProvider extends AbstractQQQApplication
{
   public static boolean USE_MYSQL = false;

   public static final String RDBMS_BACKEND_NAME      = "rdbms";
   public static final String FILESYSTEM_BACKEND_NAME = "filesystem";
   public static final String MEMORY_BACKEND_NAME     = "memory";

   public static final String APP_NAME_GREETINGS     = "greetingsApp";
   public static final String APP_NAME_PEOPLE        = "peopleApp";
   public static final String APP_NAME_MISCELLANEOUS = "miscellaneous";

   public static final String PROCESS_NAME_GREET             = "greet";
   public static final String PROCESS_NAME_GREET_INTERACTIVE = "greetInteractive";
   public static final String PROCESS_NAME_CLONE_PEOPLE      = "clonePeople";
   public static final String PROCESS_NAME_SIMPLE_SLEEP      = "simpleSleep";
   public static final String PROCESS_NAME_SIMPLE_THROW      = "simpleThrow";
   public static final String PROCESS_NAME_SLEEP_INTERACTIVE = "sleepInteractive";

   public static final String TABLE_NAME_PERSON  = "person";
   public static final String TABLE_NAME_PET     = "pet";
   public static final String TABLE_NAME_CARRIER = "carrier";
   public static final String TABLE_NAME_CITY    = "city";

   public static final String STEP_NAME_SLEEPER = "sleeper";
   public static final String STEP_NAME_THROWER = "thrower";

   public static final String SCREEN_0 = "screen0";
   public static final String SCREEN_1 = "screen1";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QInstance defineQInstance() throws QException
   {
      return (Boolean.getBoolean("qqq.sample.mockAuthentication") ? defineTestInstance() : defineInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QInstance defineInstance() throws QException
   {
      QInstance qInstance = new QInstance();

      qInstance.addBackend(defineRdbmsBackend());
      qInstance.addBackend(defineMemoryBackend());
      qInstance.addBackend(defineFilesystemBackend());
      qInstance.addTable(defineTableCarrier());
      qInstance.addTable(defineTablePerson());
      qInstance.addPossibleValueSource(QPossibleValueSource.newForTable(TABLE_NAME_PERSON));
      qInstance.addPossibleValueSource(QPossibleValueSource.newForEnum(PetSpecies.NAME, PetSpecies.values()));
      qInstance.addTable(defineTablePet());
      qInstance.addJoin(defineTablePersonJoinPet());
      qInstance.addTable(defineTableCityFile());
      qInstance.addProcess(defineProcessGreetPeople());
      qInstance.addProcess(defineProcessGreetPeopleInteractive());
      qInstance.addProcess(defineProcessClonePeople());
      qInstance.addProcess(defineProcessSimpleSleep());
      qInstance.addProcess(defineProcessScreenThenSleep());
      qInstance.addProcess(defineProcessSimpleThrow());

      qInstance.addTable(setTableBackendNamesForRdbms(new UserSessionMetaDataProducer(RDBMS_BACKEND_NAME).produce(qInstance)));
      qInstance.addTable(setTableBackendNamesForRdbms(new RedirectStateMetaDataProducer(RDBMS_BACKEND_NAME).produce(qInstance)));

      MetaDataProducerHelper.processAllMetaDataProducersInPackage(qInstance, SampleMetaDataProvider.class.getPackageName());

      defineWidgets(qInstance);
      defineBranding(qInstance);
      defineApps(qInstance);

      return (qInstance);
   }



   /*******************************************************************************
    ** if rdbms backend uses snake_case table & column names, instead of camelCase
    ** style used for qqq meta-data tableNames and fieldNames, then set those via
    ** this method.
    *******************************************************************************/
   private static QTableMetaData setTableBackendNamesForRdbms(QTableMetaData table)
   {
      table.setBackendDetails(new RDBMSTableBackendDetails()
         .withTableName(QInstanceEnricher.inferBackendName(table.getName())));
      QInstanceEnricher.setInferredFieldBackendNames(table);
      return (table);
   }



   /***************************************************************************
    ** for tests, define the same instance as above, but use mock authentication.
    ***************************************************************************/
   public static QInstance defineTestInstance() throws QException
   {
      QInstance qInstance = defineInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), defineAuthentication());
      return qInstance;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QBackendMetaData defineMemoryBackend()
   {
      return new QBackendMetaData()
         .withName(MEMORY_BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void primeTestDatabase(String sqlFileName) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()))
      {
         InputStream  primeTestDatabaseSqlStream = SampleMetaDataProvider.class.getResourceAsStream("/" + sqlFileName);
         List<String> lines                      = IOUtils.readLines(primeTestDatabaseSqlStream, StandardCharsets.UTF_8);
         lines = lines.stream().filter(line -> !line.startsWith("-- ")).toList();
         String joinedSQL = String.join("\n", lines);
         for(String sql : joinedSQL.split(";"))
         {
            QueryManager.executeUpdate(connection, sql);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineBranding(QInstance qInstance)
   {
      qInstance.setBranding(new QBrandingMetaData()
         .withAppName("QQQ Sample")
         .withLogo("/samples-logo.png")
         .withIcon("/kr-icon.png"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineWidgets(QInstance qInstance)
   {
      qInstance.addWidget(new QWidgetMetaData()
         .withName(PersonsByCreateDateBarChart.class.getSimpleName())
         .withCodeReference(new QCodeReference(PersonsByCreateDateBarChart.class)));

      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
      String                       accountId   = interpreter.interpret("${env.QUICKSIGHT_ACCOUNT_ID}");
      String                       accessKey   = interpreter.interpret("${env.QUICKSIGHT_ACCESS_KEY}");
      String                       secretKey   = interpreter.interpret("${env.QUICKSIGHT_SECRET_KEY}");
      String                       userArn     = interpreter.interpret("${env.QUICKSIGHT_USER_ARN}");

      QWidgetMetaDataInterface quickSightChartMetaData = new QuickSightChartMetaData()
         .withAccountId(accountId)
         .withAccessKey(accessKey)
         .withSecretKey(secretKey)
         .withUserArn(userArn)
         .withDashboardId("9e452e78-8509-4c81-bb7f-967abfc356da")
         .withRegion(Regions.US_EAST_2.getName())
         .withName(QuickSightChartRenderer.class.getSimpleName())
         .withLabel("Example Quicksight Chart")
         .withCodeReference(new QCodeReference(QuickSightChartRenderer.class));

      qInstance.addWidget(quickSightChartMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineApps(QInstance qInstance)
   {
      qInstance.addApp(new QAppMetaData()
         .withName(APP_NAME_GREETINGS)
         .withIcon(new QIcon().withName("emoji_people"))
         .withChild(qInstance.getProcess(PROCESS_NAME_GREET).withIcon(new QIcon().withName("emoji_people")))
         .withChild(qInstance.getTable(TABLE_NAME_PERSON).withIcon(new QIcon().withName("person")))
         .withChild(qInstance.getTable(TABLE_NAME_PET).withIcon(new QIcon().withName("pets")))
         .withChild(qInstance.getTable(TABLE_NAME_CITY).withIcon(new QIcon().withName("location_city")))
         .withChild(qInstance.getProcess(PROCESS_NAME_GREET_INTERACTIVE).withIcon(new QIcon().withName("waving_hand")))
         .withWidgets(List.of(PersonsByCreateDateBarChart.class.getSimpleName(), QuickSightChartRenderer.class.getSimpleName()))
      );

      qInstance.addApp(new QAppMetaData()
         .withName(APP_NAME_PEOPLE)
         .withIcon(new QIcon().withName("person"))
         .withChild(qInstance.getApp(APP_NAME_GREETINGS))
         .withChild(qInstance.getProcess(PROCESS_NAME_CLONE_PEOPLE).withIcon(new QIcon().withName("content_copy")))
      );

      qInstance.addApp(new QAppMetaData()
         .withName(APP_NAME_MISCELLANEOUS)
         .withIcon(new QIcon().withName("stars"))
         .withChild(qInstance.getTable(TABLE_NAME_CARRIER).withIcon(new QIcon("local_shipping")))
         .withChild(qInstance.getTable(FieldLabTableMetaDataProducer.NAME).withIcon(new QIcon("science")))
         .withChild(qInstance.getProcess(PROCESS_NAME_SIMPLE_SLEEP))
         .withChild(qInstance.getProcess(PROCESS_NAME_SLEEP_INTERACTIVE))
         .withChild(qInstance.getProcess(PROCESS_NAME_SIMPLE_THROW)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QAuthenticationMetaData defineAuthentication()
   {
      return (new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static RDBMSBackendMetaData defineRdbmsBackend()
   {
      if(USE_MYSQL)
      {
         QMetaDataVariableInterpreter interpreter  = new QMetaDataVariableInterpreter();
         String                       vendor       = interpreter.interpret("${env.RDBMS_VENDOR}");
         String                       hostname     = interpreter.interpret("${env.RDBMS_HOSTNAME}");
         Integer                      port         = Integer.valueOf(interpreter.interpret("${env.RDBMS_PORT}"));
         String                       databaseName = interpreter.interpret("${env.RDBMS_DATABASE_NAME}");
         String                       username     = interpreter.interpret("${env.RDBMS_USERNAME}");
         String                       password     = interpreter.interpret("${env.RDBMS_PASSWORD}");

         return new RDBMSBackendMetaData()
            .withName(RDBMS_BACKEND_NAME)
            .withVendor(vendor)
            .withHostName(hostname)
            .withPort(port)
            .withDatabaseName(databaseName)
            .withUsername(username)
            .withPassword(password);
      }
      else
      {
         return (new RDBMSBackendMetaData()
            .withName(RDBMS_BACKEND_NAME)
            .withVendor("h2")
            .withHostName("mem")
            .withDatabaseName("test_database")
            .withQueriesForNewConnections(List.of("SET TIME ZONE 'UTC'"))
            .withUsername("sa"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static FilesystemBackendMetaData defineFilesystemBackend()
   {
      return new FilesystemBackendMetaData()
         .withBasePath("/tmp/sample-filesystem")
         .withName(FILESYSTEM_BACKEND_NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineTableCarrier()
   {
      QTableMetaData table = new QTableMetaData();
      table.setName(TABLE_NAME_CARRIER);
      table.setBackendName(RDBMS_BACKEND_NAME);
      table.setPrimaryKeyField("id");
      table.setRecordLabelFormat("%s");
      table.setRecordLabelFields(List.of("name"));

      table.addField(new QFieldMetaData("id", QFieldType.INTEGER));

      table.addField(new QFieldMetaData("name", QFieldType.STRING)
         .withIsRequired(true));

      table.addField(new QFieldMetaData("company_code", QFieldType.STRING) // todo PVS
         .withLabel("Company")
         .withIsRequired(true)
         .withBackendName("company_code"));

      table.addField(new QFieldMetaData("service_level", QFieldType.STRING) // todo PVS
         .withLabel("Service Level")
         .withIsRequired(true));

      table.addSection(new QFieldSection("identity", "Identity", new QIcon("badge"), Tier.T1, List.of("id", "name")));
      table.addSection(new QFieldSection("basicInfo", "Basic Info", new QIcon("dataset"), Tier.T2, List.of("company_code", "service_level")));

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineTablePerson()
   {
      QTableMetaData qTableMetaData = new QTableMetaData()
         .withName(TABLE_NAME_PERSON)
         .withLabel("Person")
         .withBackendName(RDBMS_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("firstName", "lastName")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date").withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date").withIsEditable(false))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING).withBackendName("first_name").withIsRequired(true))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING).withBackendName("last_name").withIsRequired(true))
         .withField(new QFieldMetaData("birthDate", QFieldType.DATE).withBackendName("birth_date"))
         .withField(new QFieldMetaData("email", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("isEmployed", QFieldType.BOOLEAN).withBackendName("is_employed"))
         .withField(new QFieldMetaData("annualSalary", QFieldType.DECIMAL).withBackendName("annual_salary").withDisplayFormat(DisplayFormat.CURRENCY))
         .withField(new QFieldMetaData("daysWorked", QFieldType.INTEGER).withBackendName("days_worked").withDisplayFormat(DisplayFormat.COMMAS))

         .withSection(new QFieldSection("identity", "Identity", new QIcon("badge"), Tier.T1, List.of("id", "firstName", "lastName")))
         .withSection(new QFieldSection("basicInfo", "Basic Info", new QIcon("dataset"), Tier.T2, List.of("email", "birthDate")))
         .withSection(new QFieldSection("employmentInfo", "Employment Info", new QIcon("work"), Tier.T2, List.of("isEmployed", "annualSalary", "daysWorked")))
         .withSection(new QFieldSection("dates", "Dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      QInstanceEnricher.setInferredFieldBackendNames(qTableMetaData);

      qTableMetaData.withAssociation(new Association()
         .withAssociatedTableName(TABLE_NAME_PET)
         .withName("pets")
         .withJoinName(QJoinMetaData.makeInferredJoinName(TABLE_NAME_PERSON, TABLE_NAME_PET)));

      return (qTableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineTablePet()
   {
      QTableMetaData qTableMetaData = new QTableMetaData()
         .withName(TABLE_NAME_PET)
         .withLabel("Pet")
         .withBackendName(RDBMS_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("name")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date").withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date").withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withBackendName("name").withIsRequired(true))
         .withField(new QFieldMetaData("personId", QFieldType.INTEGER).withBackendName("person_id").withIsRequired(true).withPossibleValueSourceName(TABLE_NAME_PERSON))
         .withField(new QFieldMetaData("speciesId", QFieldType.INTEGER).withBackendName("species_id").withIsRequired(true).withPossibleValueSourceName(PetSpecies.NAME))
         .withField(new QFieldMetaData("birthDate", QFieldType.DATE).withBackendName("birth_date"))

         .withSection(new QFieldSection("identity", "Identity", new QIcon("badge"), Tier.T1, List.of("id", "name")))
         .withSection(new QFieldSection("basicInfo", "Basic Info", new QIcon("dataset"), Tier.T2, List.of("personId", "speciesId", "birthDate")))
         .withSection(new QFieldSection("dates", "Dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));

      QInstanceEnricher.setInferredFieldBackendNames(qTableMetaData);

      return (qTableMetaData);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QJoinMetaData defineTablePersonJoinPet()
   {
      return new QJoinMetaData()
         .withLeftTable(TABLE_NAME_PERSON)
         .withRightTable(TABLE_NAME_PET)
         .withInferredName()
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "personId"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableMetaData defineTableCityFile()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_CITY)
         .withLabel("Cities")
         .withIsHidden(true)
         .withBackendName(FILESYSTEM_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("state", QFieldType.STRING)) // todo - state PVS.
         .withBackendDetails(new FilesystemTableBackendDetails()
            .withBasePath("cities")
            .withCardinality(Cardinality.MANY)
            .withRecordFormat(RecordFormat.CSV)
         );
   }



   /*******************************************************************************
    ** Define the 'greet people' process
    *******************************************************************************/
   private static QProcessMetaData defineProcessGreetPeople()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_GREET)
         .withLabel("Greet People")
         .withTableName(TABLE_NAME_PERSON)
         .withIsHidden(true)
         .withStep(new QBackendStepMetaData()
            .withName("prepare")
            .withCode(new QCodeReference(MockBackendStep.class))
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
         );
   }



   /*******************************************************************************
    ** Define an interactive version of the 'greet people' process
    *******************************************************************************/
   private static QProcessMetaData defineProcessGreetPeopleInteractive()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_GREET_INTERACTIVE)
         .withTableName(TABLE_NAME_PERSON)

         .withStep(LoadInitialRecordsStep.defineMetaData(TABLE_NAME_PERSON))

         .withStep(new QFrontendStepMetaData()
            .withName("setup")
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.EDIT_FORM))
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
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.VIEW_FORM))
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.RECORD_LIST))
            .withViewField(new QFieldMetaData("noOfPeopleGreeted", QFieldType.INTEGER))
            .withViewField(new QFieldMetaData("outputMessage", QFieldType.STRING))
            .withRecordListField(new QFieldMetaData("id", QFieldType.INTEGER))
            .withRecordListField(new QFieldMetaData("firstName", QFieldType.STRING))
            // .withRecordListField(new QFieldMetaData(MockBackendStep.FIELD_MOCK_VALUE, QFieldType.STRING))
            .withRecordListField(new QFieldMetaData("greetingMessage", QFieldType.STRING))
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QProcessMetaData defineProcessClonePeople()
   {
      Map<String, Serializable> values = new HashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, TABLE_NAME_PERSON);
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, TABLE_NAME_PERSON);
      values.put(StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE, "This is a preview of what the clones will look like.");

      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(
         ExtractViaQueryStep.class,
         ClonePeopleTransformStep.class,
         LoadViaInsertStep.class,
         values
      );
      process.setName(PROCESS_NAME_CLONE_PEOPLE);
      process.setTableName(TABLE_NAME_PERSON);

      process.getFrontendStep(StreamedETLWithFrontendProcess.STEP_NAME_REVIEW)
         .withRecordListField(new QFieldMetaData("firstName", QFieldType.STRING))
         .withRecordListField(new QFieldMetaData("lastName", QFieldType.STRING))
      ;

      return (process);
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
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.VIEW_FORM))
            .withFormField(new QFieldMetaData("outputMessage", QFieldType.STRING)))
         .withStep(SleeperStep.getMetaData())
         .withStep(new QFrontendStepMetaData()
            .withName(SCREEN_1)
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.VIEW_FORM))
            .withFormField(new QFieldMetaData("outputMessage", QFieldType.STRING)));
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



   /***************************************************************************
    **
    ***************************************************************************/
   public enum PetSpecies implements PossibleValueEnum<Integer>
   {
      DOG(1, "Dog"),
      CAT(2, "Cat");

      private final Integer id;
      private final String  label;

      public static final String NAME = "petSpecies";



      /***************************************************************************
       **
       ***************************************************************************/
      PetSpecies(int id, String label)
      {
         this.id = id;
         this.label = label;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Integer getPossibleValueId()
      {
         return (id);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getPossibleValueLabel()
      {
         return (label);
      }
   }

}
