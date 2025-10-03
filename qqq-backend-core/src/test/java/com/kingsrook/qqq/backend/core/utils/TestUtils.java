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

package com.kingsrook.qqq.backend.core.utils;


import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.dashboard.PersonsByCreateDateBarChart;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.person.addtopeoplesage.AddAge;
import com.kingsrook.qqq.backend.core.actions.processes.person.addtopeoplesage.GetAgeStatistics;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.values.QCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.automation.PollingAutomationProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.automation.QAutomationProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.QMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.email.EmailMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.ses.SESMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PVSValueFormatAndFields;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.QQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.simple.SimpleSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTracking;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTrackingType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheOf;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryModuleBackendVariantSetting;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.mock.MockBackendModule;
import com.kingsrook.qqq.backend.core.processes.implementations.basepull.BasepullConfiguration;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.basic.BasicETLProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamed.StreamedETLProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.core.processes.implementations.reports.RunReportForRecordProcess;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Utility class for backend-core test classes
 ** TODO - move to testutils package.
 *******************************************************************************/
public class TestUtils
{
   private static final QLogger LOG = QLogger.getLogger(TestUtils.class);

   public static final String DEFAULT_BACKEND_NAME = "default";
   public static final String MEMORY_BACKEND_NAME  = "memory";

   public static final String APP_NAME_GREETINGS     = "greetingsApp";
   public static final String APP_NAME_PEOPLE        = "peopleApp";
   public static final String APP_NAME_MISCELLANEOUS = "miscellaneous";

   public static final String TABLE_NAME_TWO_KEYS            = "twoKeys";
   public static final String TABLE_NAME_MEMORY_STORAGE      = "memoryStorage";
   public static final String TABLE_NAME_PERSON              = "person";
   public static final String TABLE_NAME_SHAPE               = "shape";
   public static final String TABLE_NAME_SHAPE_CACHE         = "shapeCache";
   public static final String TABLE_NAME_ORDER               = "order";
   public static final String TABLE_NAME_LINE_ITEM           = "orderLine";
   public static final String TABLE_NAME_LINE_ITEM_EXTRINSIC = "orderLineExtrinsic";
   public static final String TABLE_NAME_ORDER_EXTRINSIC     = "orderExtrinsic";

   public static final String MEMORY_BACKEND_WITH_VARIANTS_NAME = "memoryWithVariants";
   public static final String TABLE_NAME_MEMORY_VARIANT_OPTIONS = "memoryVariantOptions";
   public static final String TABLE_NAME_MEMORY_VARIANT_DATA    = "memoryVariantData";

   public static final String PROCESS_NAME_GREET_PEOPLE             = "greet";
   public static final String PROCESS_NAME_GREET_PEOPLE_INTERACTIVE = "greetInteractive";
   public static final String PROCESS_NAME_INCREASE_BIRTHDATE       = "increaseBirthdate";
   public static final String PROCESS_NAME_ADD_TO_PEOPLES_AGE       = "addToPeoplesAge";
   public static final String PROCESS_NAME_BASEPULL                 = "basepullTestProcess";
   public static final String PROCESS_NAME_RUN_SHAPES_PERSON_REPORT = "runShapesPersonReport";
   public static final String TABLE_NAME_PERSON_FILE                = "personFile";
   public static final String TABLE_NAME_PERSON_MEMORY              = "personMemory";
   public static final String TABLE_NAME_PERSON_MEMORY_CACHE        = "personMemoryCache";
   public static final String TABLE_NAME_ID_AND_NAME_ONLY           = "idAndNameOnly";
   public static final String TABLE_NAME_BASEPULL                   = "basepullTest";
   public static final String REPORT_NAME_SHAPES_PERSON             = "shapesPersonReport";
   public static final String REPORT_NAME_PERSON_SIMPLE             = "simplePersonReport";
   public static final String REPORT_NAME_PERSON_JOIN_SHAPE         = "personJoinShapeReport";

   public static final String POSSIBLE_VALUE_SOURCE_STATE                = "state"; // enum-type
   public static final String POSSIBLE_VALUE_SOURCE_SHAPE                = "shape"; // table-type
   public static final String POSSIBLE_VALUE_SOURCE_CUSTOM               = "custom"; // custom-type
   public static final String POSSIBLE_VALUE_SOURCE_AUTOMATION_STATUS    = "automationStatus";
   public static final String POSSIBLE_VALUE_SOURCE_STORE                = "store";
   public static final String POSSIBLE_VALUE_SOURCE_INTERNAL_OR_EXTERNAL = "internalOrExternal";

   public static final String POLLING_AUTOMATION     = "polling";
   public static final String DEFAULT_QUEUE_PROVIDER = "defaultQueueProvider";

   public static final String BASEPULL_KEY_FIELD_NAME           = "processName";
   public static final String BASEPULL_LAST_RUN_TIME_FIELD_NAME = "lastRunTime";

   public static final String SECURITY_KEY_TYPE_STORE                = "store";
   public static final String SECURITY_KEY_TYPE_STORE_ALL_ACCESS     = "storeAllAccess";
   public static final String SECURITY_KEY_TYPE_STORE_NULL_BEHAVIOR  = "storeNullBehavior";
   public static final String SECURITY_KEY_TYPE_INTERNAL_OR_EXTERNAL = "internalOrExternal";

   public static final String EMAIL_MESSAGING_PROVIDER_NAME = "email";
   public static final String SES_MESSAGING_PROVIDER_NAME   = "ses";

   public static final String SIMPLE_SCHEDULER_NAME = "simpleScheduler";
   public static final String TEST_SQS_QUEUE        = "testSQSQueue";



   /*******************************************************************************
    ** Define the instance used in standard tests.
    **
    *******************************************************************************/
   public static QInstance defineInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.setAuthentication(defineAuthentication());
      qInstance.addBackend(defineBackend());
      qInstance.addBackend(defineMemoryBackend());

      qInstance.addTable(defineTablePerson());
      qInstance.addTable(defineTableTwoKeys());
      qInstance.addTable(defineTableMemoryStorage());
      qInstance.addTable(definePersonFileTable());
      qInstance.addTable(definePersonMemoryTable());
      qInstance.addTable(definePersonMemoryCacheTable());
      qInstance.addTable(defineTableIdAndNameOnly());
      qInstance.addTable(defineTableShape());
      qInstance.addTable(defineShapeCacheTable());
      qInstance.addTable(defineTableBasepull());
      qInstance.addTable(defineTableOrder());
      qInstance.addTable(defineTableLineItem());
      qInstance.addTable(defineTableLineItemExtrinsic());
      qInstance.addTable(defineTableOrderExtrinsic());

      qInstance.addJoin(defineJoinOrderLineItem());
      qInstance.addJoin(defineJoinLineItemLineItemExtrinsic());
      qInstance.addJoin(defineJoinOrderOrderExtrinsic());

      qInstance.addPossibleValueSource(defineAutomationStatusPossibleValueSource());
      qInstance.addPossibleValueSource(defineStatesPossibleValueSource());
      qInstance.addPossibleValueSource(defineShapePossibleValueSource());
      qInstance.addPossibleValueSource(defineCustomPossibleValueSource());
      qInstance.addPossibleValueSource(defineStorePossibleValueSource());
      qInstance.addPossibleValueSource(defineStorePossibleValueInternalOrExternal());

      qInstance.addSecurityKeyType(defineStoreSecurityKeyType());
      qInstance.addSecurityKeyType(defineInternalOrExternalSecurityKeyType());

      qInstance.addProcess(defineProcessGreetPeople());
      qInstance.addProcess(defineProcessGreetPeopleInteractive());
      qInstance.addProcess(defineProcessAddToPeoplesAge());
      qInstance.addProcess(new BasicETLProcess().defineProcessMetaData());
      qInstance.addProcess(new StreamedETLProcess().defineProcessMetaData());
      qInstance.addProcess(defineProcessIncreasePersonBirthdate());
      qInstance.addProcess(defineProcessBasepull());

      qInstance.addReport(defineShapesPersonsReport());
      qInstance.addProcess(defineShapesPersonReportProcess());
      qInstance.addReport(definePersonJoinShapeReport());
      qInstance.addReport(definePersonSimpleReport());

      qInstance.addAutomationProvider(definePollingAutomationProvider());

      qInstance.addQueueProvider(defineSqsProvider());
      qInstance.addQueue(defineTestSqsQueue());

      qInstance.addMessagingProvider(defineEmailMessagingProvider());
      qInstance.addMessagingProvider(defineSESMessagingProvider());

      defineMemoryBackendVariantUseCases(qInstance);

      defineWidgets(qInstance);
      defineApps(qInstance);

      qInstance.addScheduler(defineSimpleScheduler());

      return (qInstance);
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
         .withBackendName(MEMORY_BACKEND_NAME) // note, the version without variants!
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
      );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QMessagingProviderMetaData defineSESMessagingProvider()
   {
      String accessKey = "MOCK"; // interpreter.interpret("${env.SES_ACCESS_KEY}");
      String secretKey = "MOCK"; // interpreter.interpret("${env.SES_SECRET_KEY}");
      String region    = "MOCK"; // interpreter.interpret("${env.SES_REGION}");

      return (new SESMessagingProviderMetaData()
         .withAccessKey(accessKey)
         .withSecretKey(secretKey)
         .withRegion(region)
         .withName(SES_MESSAGING_PROVIDER_NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QMessagingProviderMetaData defineEmailMessagingProvider()
   {
      return new EmailMessagingProviderMetaData()
         .withSmtpServer("localhost")
         .withSmtpPort("2500")
         .withName(EMAIL_MESSAGING_PROVIDER_NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QSchedulerMetaData defineSimpleScheduler()
   {
      return new SimpleSchedulerMetaData().withName(SIMPLE_SCHEDULER_NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineWidgets(QInstance qInstance)
   {
      qInstance.addWidget(new QWidgetMetaData()
         .withName(PersonsByCreateDateBarChart.class.getSimpleName())
         .withCodeReference(new QCodeReference(PersonsByCreateDateBarChart.class)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QProcessMetaData defineProcessIncreasePersonBirthdate()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_INCREASE_BIRTHDATE)
         .withTableName(TABLE_NAME_PERSON_MEMORY)

         .withStep(new QFrontendStepMetaData()
            .withName("preview")
         )

         .withStep(new QBackendStepMetaData()
            .withName("doWork")
            .withCode(new QCodeReference(IncreaseBirthdateStep.class))
            .withInputData(new QFunctionInputMetaData()
               .withRecordListMetaData(new QRecordListMetaData().withTableName(TABLE_NAME_PERSON_MEMORY)))
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withFieldList(List.of(new QFieldMetaData("outputMessage", QFieldType.STRING).withDefaultValue("Success!"))))
         )

         .withStep(new QFrontendStepMetaData()
            .withName("results")
            .withFormField(new QFieldMetaData("outputMessage", QFieldType.STRING))
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void insertRecords(QTableMetaData table, List<QRecord> records) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(table.getName());
      insertInput.setRecords(records);
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class IncreaseBirthdateStep implements BackendStep
   {
      /*******************************************************************************
       ** Execute the backend step - using the request as input, and the result as output.
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         List<QRecord> recordsToUpdate = new ArrayList<>();
         for(QRecord record : runBackendStepInput.getRecords())
         {
            LocalDate birthDate = record.getValueLocalDate("birthDate");

            if(birthDate != null && birthDate.getYear() < 1900)
            {
               recordsToUpdate.add(new QRecord()
                  .withValue("id", record.getValue("id"))
                  .withValue("birthDate", birthDate.withYear(1900))
               );
            }
         }

         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(TABLE_NAME_PERSON_MEMORY);
         updateInput.setRecords(recordsToUpdate);
         new UpdateAction().execute(updateInput);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QAutomationProviderMetaData definePollingAutomationProvider()
   {
      return (new PollingAutomationProviderMetaData()
         .withName(POLLING_AUTOMATION));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void defineApps(QInstance qInstance)
   {
      qInstance.addApp(new QAppMetaData()
         .withName(APP_NAME_GREETINGS)
         .withChild(qInstance.getProcess(PROCESS_NAME_GREET_PEOPLE))
         .withChild(qInstance.getProcess(PROCESS_NAME_GREET_PEOPLE_INTERACTIVE)));

      qInstance.addApp(new QAppMetaData()
         .withName(APP_NAME_PEOPLE)
         .withChild(qInstance.getTable(TABLE_NAME_PERSON))
         .withChild(qInstance.getTable(TABLE_NAME_PERSON_FILE))
         .withChild(qInstance.getApp(APP_NAME_GREETINGS))
         .withWidgets(List.of(PersonsByCreateDateBarChart.class.getSimpleName())));

      qInstance.addApp(new QAppMetaData()
         .withName(APP_NAME_MISCELLANEOUS)
         .withChild(qInstance.getTable(TABLE_NAME_ID_AND_NAME_ONLY))
         .withChild(qInstance.getProcess(BasicETLProcess.PROCESS_NAME)));
   }



   /*******************************************************************************
    ** Define the "automationStatus" possible value source used in standard tests
    **
    *******************************************************************************/
   private static QPossibleValueSource defineAutomationStatusPossibleValueSource()
   {
      return new QPossibleValueSource()
         .withName(POSSIBLE_VALUE_SOURCE_AUTOMATION_STATUS)
         .withType(QPossibleValueSourceType.ENUM)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY)
         .withValuesFromEnum(AutomationStatus.values());
   }



   /*******************************************************************************
    ** Define the "states" possible value source used in standard tests
    **
    *******************************************************************************/
   public static QPossibleValueSource defineStatesPossibleValueSource()
   {
      return new QPossibleValueSource()
         .withName(POSSIBLE_VALUE_SOURCE_STATE)
         .withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(new QPossibleValue<>(1, "IL"), new QPossibleValue<>(2, "MO")));
   }



   /*******************************************************************************
    ** Define the "shape" possible value source used in standard tests
    **
    *******************************************************************************/
   public static QPossibleValueSource defineShapePossibleValueSource()
   {
      return new QPossibleValueSource()
         .withName(POSSIBLE_VALUE_SOURCE_SHAPE)
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName(TABLE_NAME_SHAPE)
         .withSearchFields(List.of("id", "name"))
         .withOrderByField("name");
   }



   /*******************************************************************************
    ** Define the "custom" possible value source used in standard tests
    **
    *******************************************************************************/
   private static QPossibleValueSource defineCustomPossibleValueSource()
   {
      return new QPossibleValueSource()
         .withName(POSSIBLE_VALUE_SOURCE_CUSTOM)
         .withType(QPossibleValueSourceType.CUSTOM)
         .withCustomCodeReference(new QCodeReference(CustomPossibleValueSource.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QPossibleValueSource defineStorePossibleValueSource()
   {
      return new QPossibleValueSource()
         .withName(POSSIBLE_VALUE_SOURCE_STORE)
         .withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(new QPossibleValue<>(1, "Q-Mart"), new QPossibleValue<>(2, "Tar-que")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QPossibleValueSource defineStorePossibleValueInternalOrExternal()
   {
      return new QPossibleValueSource()
         .withName(POSSIBLE_VALUE_SOURCE_INTERNAL_OR_EXTERNAL)
         .withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(new QPossibleValue<>("internal", "Internal"), new QPossibleValue<>("external", "External")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QSecurityKeyType defineStoreSecurityKeyType()
   {
      return new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_STORE)
         .withAllAccessKeyName(SECURITY_KEY_TYPE_STORE_ALL_ACCESS)
         .withNullValueBehaviorKeyName(SECURITY_KEY_TYPE_STORE_NULL_BEHAVIOR)
         .withPossibleValueSourceName(POSSIBLE_VALUE_SOURCE_STORE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QSecurityKeyType defineInternalOrExternalSecurityKeyType()
   {
      return new QSecurityKeyType()
         .withName(SECURITY_KEY_TYPE_INTERNAL_OR_EXTERNAL)
         .withPossibleValueSourceName(POSSIBLE_VALUE_SOURCE_INTERNAL_OR_EXTERNAL);
   }



   /*******************************************************************************
    ** Define the authentication used in standard tests - using 'mock' type.
    **
    *******************************************************************************/
   public static QAuthenticationMetaData defineAuthentication()
   {
      return new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK);
   }



   /*******************************************************************************
    ** Define the backend used in standard tests - using 'mock' type.
    *******************************************************************************/
   public static QBackendMetaData defineBackend()
   {
      return new QBackendMetaData()
         .withName(DEFAULT_BACKEND_NAME)
         .withBackendType(MockBackendModule.class);
   }



   /*******************************************************************************
    ** Define the in-memory backend used in standard tests
    *******************************************************************************/
   public static QBackendMetaData defineMemoryBackend()
   {
      return new QBackendMetaData()
         .withName(MEMORY_BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class);
   }



   /*******************************************************************************
    ** Define the 'two key' table used in standard tests.
    *******************************************************************************/
   public static QTableMetaData defineTableTwoKeys()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_TWO_KEYS)
         .withLabel("Two Keys")
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withUniqueKey(new UniqueKey("key1", "key2"))
         .withField(new QFieldMetaData("key1", QFieldType.INTEGER))
         .withField(new QFieldMetaData("key2", QFieldType.INTEGER));
   }



   /*******************************************************************************
    ** Define a table in the memory store that can be used for the StorageAction
    *******************************************************************************/
   public static QTableMetaData defineTableMemoryStorage()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_MEMORY_STORAGE)
         .withLabel("Memory Storage")
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("reference")
         .withField(new QFieldMetaData("reference", QFieldType.STRING).withIsEditable(false))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB));
   }



   /*******************************************************************************
    ** Define the 'person' table used in standard tests.
    *******************************************************************************/
   public static QTableMetaData defineTablePerson()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_PERSON)
         .withLabel("Person")
         .withBackendName(DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING))
         .withField(new QFieldMetaData("birthDate", QFieldType.DATE))
         .withField(new QFieldMetaData("email", QFieldType.STRING))
         .withField(new QFieldMetaData("homeStateId", QFieldType.INTEGER).withPossibleValueSourceName(POSSIBLE_VALUE_SOURCE_STATE))
         .withField(new QFieldMetaData("favoriteShapeId", QFieldType.INTEGER).withPossibleValueSourceName(POSSIBLE_VALUE_SOURCE_SHAPE))
         .withField(new QFieldMetaData("customValue", QFieldType.INTEGER).withPossibleValueSourceName(POSSIBLE_VALUE_SOURCE_CUSTOM))
         .withField(new QFieldMetaData("noOfShoes", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(new QFieldMetaData("cost", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.CURRENCY))
         .withField(new QFieldMetaData("price", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.CURRENCY))
         .withField(new QFieldMetaData("ssn", QFieldType.STRING).withType(QFieldType.PASSWORD))
         .withField(new QFieldMetaData("superSecret", QFieldType.STRING).withType(QFieldType.PASSWORD).withIsHidden(true))
         .withField(new QFieldMetaData("timestamp", QFieldType.DATE_TIME)) // adding this for GC tests, so we can set a date-time (since CD & MD are owned by system)
         ;
   }



   /*******************************************************************************
    ** Define the order table used in standard tests.
    *******************************************************************************/
   public static QTableMetaData defineTableOrder()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_ORDER)
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(SECURITY_KEY_TYPE_STORE)
            .withFieldName("storeId"))
         .withAssociation(new Association().withName("orderLine").withAssociatedTableName(TABLE_NAME_LINE_ITEM).withJoinName("orderLineItem"))
         .withAssociation(new Association().withName("extrinsics").withAssociatedTableName(TABLE_NAME_ORDER_EXTRINSIC).withJoinName("orderOrderExtrinsic"))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_LINE_ITEM))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("timestamp", QFieldType.DATE_TIME)) // adding this for GC tests, so we can set a date-time (since CD & MD are owned by system)
         .withField(new QFieldMetaData("orderNo", QFieldType.STRING))
         .withField(new QFieldMetaData("shipToName", QFieldType.STRING).withMaxLength(200).withBehavior(ValueTooLongBehavior.ERROR))
         .withField(new QFieldMetaData("orderDate", QFieldType.DATE))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("total", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.CURRENCY).withFieldSecurityLock(new FieldSecurityLock()
            .withSecurityKeyType(SECURITY_KEY_TYPE_INTERNAL_OR_EXTERNAL)
            .withDefaultBehavior(FieldSecurityLock.Behavior.DENY)
            .withOverrideValues(List.of("internal"))
         ));
   }



   /*******************************************************************************
    ** Define the lineItem table used in standard tests.
    *******************************************************************************/
   public static QTableMetaData defineTableLineItem()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_LINE_ITEM)
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(SECURITY_KEY_TYPE_STORE)
            .withFieldName("order.storeId")
            .withJoinNameChain(List.of("orderLineItem")))
         .withAssociation(new Association().withName("extrinsics").withAssociatedTableName(TABLE_NAME_LINE_ITEM_EXTRINSIC).withJoinName("lineItemLineItemExtrinsic"))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_ORDER))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("timestamp", QFieldType.DATE_TIME)) // adding this for GC tests, so we can set a date-time (since CD & MD are owned by system)
         .withField(new QFieldMetaData("orderId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("lineNumber", QFieldType.STRING))
         .withField(new QFieldMetaData("sku", QFieldType.STRING).withLabel("SKU"))
         .withField(new QFieldMetaData("quantity", QFieldType.INTEGER));
   }



   /*******************************************************************************
    ** Define the lineItemExtrinsic table used in standard tests.
    *******************************************************************************/
   public static QTableMetaData defineTableLineItemExtrinsic()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_LINE_ITEM_EXTRINSIC)
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(SECURITY_KEY_TYPE_STORE)
            .withFieldName("order.storeId")
            .withJoinNameChain(List.of("orderLineItem", "lineItemLineItemExtrinsic")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("lineItemId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("key", QFieldType.STRING))
         .withField(new QFieldMetaData("value", QFieldType.STRING))
         .withField(new QFieldMetaData("source", QFieldType.STRING)); // doesn't really make sense, but useful to have an extra field here in some bulk-load tests
   }



   /*******************************************************************************
    ** Define the orderExtrinsic table used in standard tests.
    *******************************************************************************/
   public static QTableMetaData defineTableOrderExtrinsic()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_ORDER_EXTRINSIC)
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(SECURITY_KEY_TYPE_STORE)
            .withFieldName("order.storeId")
            .withJoinNameChain(List.of("orderOrderExtrinsic")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("orderId", QFieldType.INTEGER))
         .withField(new QFieldMetaData("key", QFieldType.STRING))
         .withField(new QFieldMetaData("value", QFieldType.STRING).withMaxLength(100).withBehavior(ValueTooLongBehavior.ERROR));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QJoinMetaData defineJoinOrderLineItem()
   {
      return new QJoinMetaData()
         .withName("orderLineItem")
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_LINE_ITEM)
         .withJoinOn(new JoinOn("id", "orderId"))
         .withOrderBy(new QFilterOrderBy("lineNumber"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QJoinMetaData defineJoinLineItemLineItemExtrinsic()
   {
      return new QJoinMetaData()
         .withName("lineItemLineItemExtrinsic")
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(TABLE_NAME_LINE_ITEM)
         .withRightTable(TABLE_NAME_LINE_ITEM_EXTRINSIC)
         .withJoinOn(new JoinOn("id", "lineItemId"))
         .withOrderBy(new QFilterOrderBy("key"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QJoinMetaData defineJoinOrderOrderExtrinsic()
   {
      return new QJoinMetaData()
         .withName("orderOrderExtrinsic")
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_ORDER_EXTRINSIC)
         .withJoinOn(new JoinOn("id", "orderId"))
         .withOrderBy(new QFilterOrderBy("key"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QFieldMetaData standardQqqAutomationStatusField()
   {
      return (new QFieldMetaData("qqqAutomationStatus", QFieldType.INTEGER).withPossibleValueSourceName(POSSIBLE_VALUE_SOURCE_AUTOMATION_STATUS));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QTableAutomationDetails defineStandardAutomationDetails()
   {
      return (new QTableAutomationDetails()
         .withProviderName(POLLING_AUTOMATION)
         .withSchedule(new QScheduleMetaData()
            .withSchedulerName(SIMPLE_SCHEDULER_NAME)
            .withRepeatSeconds(60))
         .withStatusTracking(new AutomationStatusTracking()
            .withType(AutomationStatusTrackingType.FIELD_IN_TABLE)
            .withFieldName("qqqAutomationStatus")));
   }



   /*******************************************************************************
    ** Define the 'shape' table used in standard tests.
    *******************************************************************************/
   public static QTableMetaData defineTableShape()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_SHAPE)
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withRecordLabelFields("name")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("type", QFieldType.STRING)) // todo PVS
         .withField(new QFieldMetaData("noOfSides", QFieldType.INTEGER))
         .withField(new QFieldMetaData("isPolygon", QFieldType.BOOLEAN)) // mmm, should be derived from type, no?
         ;
   }



   /*******************************************************************************
    ** Define a 2nd version of the 'person' table for this test (pretend it's backed by a file)
    *******************************************************************************/
   public static QTableMetaData definePersonFileTable()
   {
      return (new QTableMetaData()
         .withName(TABLE_NAME_PERSON_FILE)
         .withLabel("Person File")
         .withBackendName(DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withFields(TestUtils.defineTablePerson().getFields()));
   }



   /*******************************************************************************
    ** Define a table with unique key where one is nullable
    *******************************************************************************/
   public static QTableMetaData defineTwoKeyTable()
   {
      return (new QTableMetaData()
         .withName(TABLE_NAME_BASEPULL)
         .withLabel("Basepull Test")
         .withPrimaryKeyField("id")
         .withBackendName(MEMORY_BACKEND_NAME)
         .withFields(TestUtils.defineTablePerson().getFields()))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date").withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date").withIsEditable(false))
         .withField(new QFieldMetaData(BASEPULL_KEY_FIELD_NAME, QFieldType.STRING).withBackendName("process_name").withIsRequired(true))
         .withField(new QFieldMetaData(BASEPULL_LAST_RUN_TIME_FIELD_NAME, QFieldType.DATE_TIME).withBackendName("last_run_time").withIsRequired(true));
   }



   /*******************************************************************************
    ** Define a basepullTable
    *******************************************************************************/
   public static QTableMetaData defineTableBasepull()
   {
      return (new QTableMetaData()
         .withName(TABLE_NAME_BASEPULL)
         .withLabel("Basepull Test")
         .withPrimaryKeyField("id")
         .withBackendName(MEMORY_BACKEND_NAME)
         .withFields(TestUtils.defineTablePerson().getFields()))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date").withIsEditable(false))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date").withIsEditable(false))
         .withField(new QFieldMetaData(BASEPULL_KEY_FIELD_NAME, QFieldType.STRING).withBackendName("process_name").withIsRequired(true))
         .withField(new QFieldMetaData(BASEPULL_LAST_RUN_TIME_FIELD_NAME, QFieldType.DATE_TIME).withBackendName("last_run_time").withIsRequired(true));
   }



   /*******************************************************************************
    ** Define a 3nd version of the 'person' table, backed by the in-memory backend
    *******************************************************************************/
   public static QTableMetaData definePersonMemoryTable()
   {
      /////////////////////////////////////////////////////////////////////////////
      // the checkAge automation will only run on persons younger than this date //
      /////////////////////////////////////////////////////////////////////////////
      LocalDate youngPersonLimitDate = LocalDate.now().minusYears(18);

      /////////////////////////////////////////////////////////////////////////////////////
      // the increaseBirthdate automation will only run on persons born before this date //
      /////////////////////////////////////////////////////////////////////////////////////
      LocalDate increaseBirthdateLimitDate = LocalDate.of(1900, Month.JANUARY, 1);

      return (new QTableMetaData()
         .withName(TABLE_NAME_PERSON_MEMORY)
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("firstName", "lastName"))
         .withFields(TestUtils.defineTablePerson().getFields()))

         .withField(standardQqqAutomationStatusField())
         .withAutomationDetails(defineStandardAutomationDetails()
            .withAction(new TableAutomationAction()
               .withName("checkAgeOnInsert")
               .withTriggerEvent(TriggerEvent.POST_INSERT)
               .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.GREATER_THAN, List.of(youngPersonLimitDate))))
               .withCodeReference(new QCodeReference(CheckAge.class))
            )
            .withAction(new TableAutomationAction()
               .withName("failAutomationForSith")
               .withTriggerEvent(TriggerEvent.POST_INSERT)
               .withCodeReference(new QCodeReference(FailAutomationForSith.class))
            )
            .withAction(new TableAutomationAction()
               .withName("increaseBirthdate")
               .withTriggerEvent(TriggerEvent.POST_INSERT)
               .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("birthDate", QCriteriaOperator.LESS_THAN, List.of(increaseBirthdateLimitDate))))
               .withProcessName(PROCESS_NAME_INCREASE_BIRTHDATE)
            )
            .withAction(new TableAutomationAction()
               .withName("logOnUpdatePerFilter")
               .withTriggerEvent(TriggerEvent.POST_UPDATE)
               .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("firstName", QCriteriaOperator.CONTAINS, List.of("Darin"))))
               .withCodeReference(new QCodeReference(LogPersonUpdate.class))
            )
         );
   }



   /*******************************************************************************
    ** Define yet another version of the 'person' table, also in-memory, and as a
    ** cache on the other in-memory one...
    *******************************************************************************/
   public static QTableMetaData definePersonMemoryCacheTable()
   {
      UniqueKey uniqueKey = new UniqueKey("firstName", "lastName");
      return (new QTableMetaData()
         .withName(TABLE_NAME_PERSON_MEMORY_CACHE)
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withUniqueKey(uniqueKey)
         .withFields(TestUtils.defineTablePerson().getFields()))
         .withField(new QFieldMetaData("cachedDate", QFieldType.DATE_TIME))
         .withCacheOf(new CacheOf()
            .withSourceTable(TABLE_NAME_PERSON_MEMORY)
            .withCachedDateFieldName("cachedDate")
            .withExpirationSeconds(60)
            .withUseCase(new CacheUseCase()
               .withType(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY)
               .withSourceUniqueKey(uniqueKey)
               .withCacheUniqueKey(uniqueKey)
               .withCacheSourceMisses(false)
               .withExcludeRecordsMatching(List.of(
                     new QQueryFilter(
                        new QFilterCriteria("noOfShoes", QCriteriaOperator.EQUALS, "503"),
                        new QFilterCriteria("noOfShoes", QCriteriaOperator.EQUALS, "999")
                     ).withBooleanOperator(QQueryFilter.BooleanOperator.OR)
                  )
               ))
         );
   }



   /*******************************************************************************
    ** Define another version of the 'shape' table, also in-memory, and as a
    ** cache on the other in-memory one...
    *******************************************************************************/
   public static QTableMetaData defineShapeCacheTable()
   {
      UniqueKey uniqueKey = new UniqueKey("name");
      return (new QTableMetaData()
         .withName(TABLE_NAME_SHAPE_CACHE)
         .withBackendName(MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withUniqueKey(uniqueKey)
         .withFields(TestUtils.defineTableShape().getFields()))
         .withField(new QFieldMetaData("cachedDate", QFieldType.DATE_TIME))
         .withCacheOf(new CacheOf()
            .withSourceTable(TABLE_NAME_SHAPE)
            .withCachedDateFieldName("cachedDate")
            .withExpirationSeconds(60)
            .withUseCase(new CacheUseCase()
               .withType(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY)
               .withSourceUniqueKey(uniqueKey)
               .withCacheUniqueKey(uniqueKey)
               .withCacheSourceMisses(false)
               .withExcludeRecordsMatching(List.of(
                     new QQueryFilter(
                        new QFilterCriteria("noOfSides", QCriteriaOperator.EQUALS, 503),
                        new QFilterCriteria("noOfSides", QCriteriaOperator.EQUALS, 999)
                     ).withBooleanOperator(QQueryFilter.BooleanOperator.OR)
                  )
               ))
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CheckAge implements RecordAutomationHandlerInterface
   {
      public static String SUFFIX_FOR_MINORS = " (a minor)";



      /*******************************************************************************
       **
       *******************************************************************************/
      public void execute(RecordAutomationInput recordAutomationInput) throws QException
      {
         LocalDate     limitDate       = LocalDate.now().minusYears(18);
         List<QRecord> recordsToUpdate = new ArrayList<>();
         for(QRecord record : recordAutomationInput.getRecordList())
         {
            ////////////////////////////////////////////////////////////////////////
            // get the record - its automation status should currently be RUNNING //
            ////////////////////////////////////////////////////////////////////////
            QRecord freshlyFetchedRecord = new GetAction().executeForRecord(new GetInput(TABLE_NAME_PERSON_MEMORY).withPrimaryKey(record.getValue("id")));
            assertEquals(AutomationStatus.RUNNING_INSERT_AUTOMATIONS.getId(), freshlyFetchedRecord.getValueInteger(TestUtils.standardQqqAutomationStatusField().getName()));

            ///////////////////////////////////////////
            // do whatever business logic we do here //
            ///////////////////////////////////////////
            LocalDate birthDate = record.getValueLocalDate("birthDate");
            if(birthDate != null && birthDate.isAfter(limitDate))
            {
               recordsToUpdate.add(new QRecord()
                  .withValue("id", record.getValue("id"))
                  .withValue("firstName", record.getValueString("firstName") + SUFFIX_FOR_MINORS)
               );
            }
         }

         if(!recordsToUpdate.isEmpty())
         {
            UpdateInput updateInput = new UpdateInput();
            updateInput.setTableName(recordAutomationInput.getTableName());
            updateInput.setRecords(recordsToUpdate);
            new UpdateAction().execute(updateInput);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class FailAutomationForSith implements RecordAutomationHandlerInterface
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      public void execute(RecordAutomationInput recordAutomationInput) throws QException
      {
         for(QRecord record : recordAutomationInput.getRecordList())
         {
            if("Darth".equals(record.getValue("firstName")))
            {
               throw new QException("Oops, you look like a Sith!");
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class LogPersonUpdate implements RecordAutomationHandlerInterface
   {
      public static List<Integer> updatedIds = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      public void execute(RecordAutomationInput recordAutomationInput) throws QException
      {
         for(QRecord record : recordAutomationInput.getRecordList())
         {
            updatedIds.add(record.getValueInteger("id"));
            LOG.info("Person [" + record.getValueInteger("id") + ":" + record.getValueString("firstName") + "] has been updated.");
         }
      }
   }



   /*******************************************************************************
    ** Define simple table with just an id and name
    *******************************************************************************/
   public static QTableMetaData defineTableIdAndNameOnly()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_ID_AND_NAME_ONLY)
         .withLabel("Id and Name Only")
         .withBackendName(DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING));
   }



   /*******************************************************************************
    ** Define the 'greet people' process
    *******************************************************************************/
   private static QProcessMetaData defineProcessGreetPeople()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_GREET_PEOPLE)
         .withTableName(TABLE_NAME_PERSON_MEMORY)
         .withStep(new QBackendStepMetaData()
            .withName("prepare")
            .withCode(new QCodeReference()
               .withName(MockBackendStep.class.getName())
               .withCodeType(QCodeType.JAVA))
            .withInputData(new QFunctionInputMetaData()
               .withRecordListMetaData(new QRecordListMetaData().withTableName(TABLE_NAME_PERSON_MEMORY))
               .withFieldList(List.of(
                  new QFieldMetaData("greetingPrefix", QFieldType.STRING),
                  new QFieldMetaData("greetingSuffix", QFieldType.STRING)
               )))
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withRecordListMetaData(new QRecordListMetaData()
                  .withTableName(TABLE_NAME_PERSON_MEMORY)
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
    ** Define the "add to people's age" process
    **
    ** Works on a list of rows from the person table.
    ** - first function reports the current min & max age for all input rows.
    ** - user is then prompted for how much they want to add to everyone.
    ** - then the second function adds that value to their age, and shows the results.
    *******************************************************************************/
   private static QProcessMetaData defineProcessAddToPeoplesAge()
   {
      return new QProcessMetaData()
         .withName(PROCESS_NAME_ADD_TO_PEOPLES_AGE)
         .withTableName(TABLE_NAME_PERSON)
         .withStep(new QBackendStepMetaData()
            .withName("getAgeStatistics")
            .withCode(new QCodeReference()
               .withName(GetAgeStatistics.class.getName())
               .withCodeType(QCodeType.JAVA))
            .withInputData(new QFunctionInputMetaData()
               .withRecordListMetaData(new QRecordListMetaData().withTableName(TABLE_NAME_PERSON)))
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withRecordListMetaData(new QRecordListMetaData()
                  .withTableName(TABLE_NAME_PERSON)
                  .withField(new QFieldMetaData("age", QFieldType.INTEGER)))
               .withFieldList(List.of(
                  new QFieldMetaData("minAge", QFieldType.INTEGER),
                  new QFieldMetaData("maxAge", QFieldType.INTEGER)))))
         .withStep(new QBackendStepMetaData()
            .withName("addAge")
            .withCode(new QCodeReference()
               .withName(AddAge.class.getName())
               .withCodeType(QCodeType.JAVA))
            .withInputData(new QFunctionInputMetaData()
               .withFieldList(List.of(new QFieldMetaData("yearsToAdd", QFieldType.INTEGER))))
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withRecordListMetaData(new QRecordListMetaData()
                  .withTableName(TABLE_NAME_PERSON)
                  .withField(new QFieldMetaData("newAge", QFieldType.INTEGER)))));

   }



   /*******************************************************************************
    ** Define a sample basepull process
    *******************************************************************************/
   private static QProcessMetaData defineProcessBasepull()
   {
      return new QProcessMetaData()
         .withBasepullConfiguration(new BasepullConfiguration()
            .withKeyField(BASEPULL_KEY_FIELD_NAME)
            .withLastRunTimeFieldName(BASEPULL_LAST_RUN_TIME_FIELD_NAME)
            .withHoursBackForInitialTimestamp(24)
            .withKeyValue(PROCESS_NAME_BASEPULL)
            .withTableName(defineTableBasepull().getName()))
         .withName(PROCESS_NAME_BASEPULL)
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
    **
    *******************************************************************************/
   public static QSession getMockSession() throws QAuthenticationException
   {
      MockAuthenticationModule mockAuthenticationModule = new MockAuthenticationModule();
      return (mockAuthenticationModule.createSession(null, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<QRecord> queryTable(String tableName) throws QException
   {
      return (queryTable(TestUtils.defineInstance(), tableName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static List<QRecord> queryTable(QInstance instance, String tableName) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return (queryOutput.getRecords());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void insertDefaultShapes(QInstance qInstance) throws QException
   {
      List<QRecord> shapeRecords = List.of(
         new QRecord().withTableName(TABLE_NAME_SHAPE).withValue("id", 1).withValue("name", "Triangle"),
         new QRecord().withTableName(TABLE_NAME_SHAPE).withValue("id", 2).withValue("name", "Square"),
         new QRecord().withTableName(TABLE_NAME_SHAPE).withValue("id", 3).withValue("name", "Circle"));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TABLE_NAME_SHAPE);
      insertInput.setRecords(shapeRecords);
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void insertExtraShapes(QInstance qInstance) throws QException
   {
      List<QRecord> shapeRecords = List.of(
         new QRecord().withTableName(TABLE_NAME_SHAPE).withValue("id", 4).withValue("name", "Rectangle"),
         new QRecord().withTableName(TABLE_NAME_SHAPE).withValue("id", 5).withValue("name", "Pentagon"),
         new QRecord().withTableName(TABLE_NAME_SHAPE).withValue("id", 6).withValue("name", "Hexagon"));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TABLE_NAME_SHAPE);
      insertInput.setRecords(shapeRecords);
      new InsertAction().execute(insertInput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getPersonCsvHeader()
   {
      return ("""
         "id","createDate","modifyDate","firstName","lastName","birthDate","email"
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CustomPossibleValueSource implements QCustomPossibleValueProvider<String>
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QPossibleValue<String> getPossibleValue(Serializable idValue)
      {
         if(idValue instanceof Integer i && i < 0)
         {
            return (null);
         }

         return (new QPossibleValue<>(ValueUtils.getValueAsString(idValue), "Custom[" + idValue + "]"));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QPossibleValue<String>> search(SearchPossibleValueSourceInput input)
      {
         List<QPossibleValue<String>> rs = new ArrayList<>();
         for(int i = 0; i < 10; i++)
         {
            QPossibleValue<String> possibleValue = getPossibleValue(i);
            List<String>           idsInType     = convertInputIdsToIdType(String.class, input.getIdList());
            if(doesPossibleValueMatchSearchInput(idsInType, possibleValue, input))
            {
               rs.add(possibleValue);
            }
         }
         return (rs);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QQueueProviderMetaData defineSqsProvider()
   {
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();

      String accessKey = "MOCK"; // interpreter.interpret("${env.SQS_ACCESS_KEY}");
      String secretKey = "MOCK"; // interpreter.interpret("${env.SQS_SECRET_KEY}");
      String region    = "MOCK"; // interpreter.interpret("${env.SQS_REGION}");
      String baseURL   = "MOCK"; // interpreter.interpret("${env.SQS_BASE_URL}");

      return (new SQSQueueProviderMetaData()
         .withName(DEFAULT_QUEUE_PROVIDER)
         .withAccessKey(accessKey)
         .withSecretKey(secretKey)
         .withRegion(region)
         .withBaseURL(baseURL));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QQueueMetaData defineTestSqsQueue()
   {
      return (new QQueueMetaData()
         .withName(TEST_SQS_QUEUE)
         .withProviderName(DEFAULT_QUEUE_PROVIDER)
         .withQueueName("test-queue")
         .withProcessName(PROCESS_NAME_INCREASE_BIRTHDATE)
         .withSchedule(new QScheduleMetaData()
            .withRepeatSeconds(60)
            .withSchedulerName(SIMPLE_SCHEDULER_NAME)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QReportMetaData defineShapesPersonsReport()
   {
      return new QReportMetaData()
         .withName(REPORT_NAME_SHAPES_PERSON)
         .withProcessName(PROCESS_NAME_RUN_SHAPES_PERSON_REPORT)
         .withInputFields(List.of(
            new QFieldMetaData(RunReportForRecordProcess.FIELD_RECORD_ID, QFieldType.INTEGER).withIsRequired(true)
         ))
         .withDataSources(List.of(
            new QReportDataSource()
               .withName("persons")
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
               .withQueryFilter(new QQueryFilter()
                  .withCriteria(new QFilterCriteria("favoriteShapeId", QCriteriaOperator.EQUALS, List.of("${input." + RunReportForRecordProcess.FIELD_RECORD_ID + "}")))
               )
         ))
         .withViews(List.of(
            new QReportView()
               .withName("person")
               .withDataSourceName("persons")
               .withType(ReportType.TABLE)
               .withColumns(List.of(
                  new QReportField().withName("id"),
                  new QReportField().withName("firstName"),
                  new QReportField().withName("lastName")
               ))
         ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData defineShapesPersonReportProcess()
   {
      return RunReportForRecordProcess.processMetaDataBuilder()
         .withProcessName(PROCESS_NAME_RUN_SHAPES_PERSON_REPORT)
         .withReportName(REPORT_NAME_SHAPES_PERSON)
         .withTableName(TestUtils.TABLE_NAME_SHAPE)
         .getProcessMetaData();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QReportMetaData definePersonSimpleReport()
   {
      return new QReportMetaData()
         .withName(REPORT_NAME_PERSON_SIMPLE)
         .withDataSource(
            new QReportDataSource()
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         )
         .withView(new QReportView()
            .withType(ReportType.TABLE)
            .withLabel("Simple Report")
            .withColumns(List.of(
               new QReportField("id"),
               new QReportField("firstName"),
               new QReportField("lastName"),
               new QReportField("homeStateId").withLabel("Home State Id"),
               new QReportField("homeStateName").withSourceFieldName("homeStateId").withShowPossibleValueLabel(true).withLabel("Home State Name")
            ))
         );
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QReportMetaData definePersonJoinShapeReport()
   {
      return new QReportMetaData()
         .withName(REPORT_NAME_PERSON_JOIN_SHAPE)
         .withDataSource(
            new QReportDataSource()
               .withSourceTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
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
   public static void updatePersonMemoryTableInContextWithWritableByWriteLockAndInsert3TestRecords() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addSecurityKeyType(new QSecurityKeyType()
         .withName("writableBy"));

      QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withField(new QFieldMetaData("onlyWritableBy", QFieldType.STRING).withLabel("Only Writable By"));
      table.withRecordSecurityLock(new RecordSecurityLock()
         .withSecurityKeyType("writableBy")
         .withFieldName("onlyWritableBy")
         .withNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW)
         .withLockScope(RecordSecurityLock.LockScope.WRITE));

      QContext.getQSession().setSecurityKeyValues(MapBuilder.of("writableBy", ListBuilder.of("jdoe", "kmarsh")));

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin"),
         new QRecord().withValue("id", 2).withValue("firstName", "Tim").withValue("onlyWritableBy", "kmarsh"),
         new QRecord().withValue("id", 3).withValue("firstName", "James").withValue("onlyWritableBy", "jdoe")
      )));

      //////////////////////////////////////////////
      // make sure we can query for all 3 records //
      //////////////////////////////////////////////
      QContext.getQSession().setSecurityKeyValues(MapBuilder.of("writableBy", ListBuilder.of("jdoe")));
      assertEquals(3, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getCount());
   }

}
