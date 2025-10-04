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

package com.kingsrook.qqq.backend.core.instances;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostQueryCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.PersonsByCreateDateBarChart;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ParentWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.metadata.AllowAllMetaDataFilter;
import com.kingsrook.qqq.backend.core.actions.metadata.DefaultNoopMetaDataActionCustomizer;
import com.kingsrook.qqq.backend.core.actions.processes.CancelProcessActionTest;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.reporting.customizers.ReportCustomRecordSourceInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.AlwaysFailsProcessValidatorPlugin;
import com.kingsrook.qqq.backend.core.instances.validation.plugins.FailsIfEnabledProcessValidatorPlugin;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.ParentWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.AdornmentType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DateTimeDisplayValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldAdornment;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueRangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.sharing.ShareableTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantSetting;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaDeleteStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for QInstanceValidator.
 **
 *******************************************************************************/
public class QInstanceValidatorTest extends BaseTest
{

   /*******************************************************************************
    ** Test a valid instance - should just pass
    **
    *******************************************************************************/
   @Test
   public void test_validatePass() throws QInstanceValidationException
   {
      new QInstanceValidator().validate(QContext.getQInstance());
   }



   /*******************************************************************************
    ** make sure we don't re-validate if already validated
    **
    *******************************************************************************/
   @Test
   public void test_doNotReValidate() throws QInstanceValidationException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.setHasBeenValidated(new QInstanceValidationKey());
      qInstance.setBackends(null);
      new QInstanceValidator().validate(qInstance);
   }



   /*******************************************************************************
    * the point of this method is to test the deprecated member, so, don't need to
    * get a compiler warning about usage of deprecated member.
    *******************************************************************************/
   @SuppressWarnings("deprecation")
   @Test
   void testMetaDataFilter()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.setMetaDataFilter(new QCodeReference(QInstanceValidator.class)),
         "Instance metaDataFilter CodeReference is not of the expected type");

      assertValidationSuccess((qInstance) -> qInstance.setMetaDataFilter(new QCodeReference(AllowAllMetaDataFilter.class)));
      assertValidationSuccess((qInstance) -> qInstance.setMetaDataFilter(null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMetaDataActionCustomizer()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.setMetaDataActionCustomizer(new QCodeReference(QInstanceValidator.class)),
         "Instance metaDataActionCustomizer CodeReference is not of the expected type");

      assertValidationSuccess((qInstance) -> qInstance.setMetaDataActionCustomizer(new QCodeReference(DefaultNoopMetaDataActionCustomizer.class)));
      assertValidationSuccess((qInstance) -> qInstance.setMetaDataActionCustomizer(null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInstanceLevelTableCustomizers()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.withTableCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(QInstanceValidator.class)),
         "Instance tableCustomizer of type preInsertRecord: CodeReference is not of the expected type");

      assertValidationFailureReasons((qInstance) ->
      {
         qInstance.withTableCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(QInstanceValidator.class));
         qInstance.withTableCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(QInstanceValidator.class));
         qInstance.withTableCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(QInstanceValidator.class));
      },
         "Instance tableCustomizer of type postUpdateRecord: CodeReference is not of the expected type",
         "Instance tableCustomizer of type postUpdateRecord: CodeReference is not of the expected type",
         "Instance tableCustomizer of type preDeleteRecord: CodeReference is not of the expected type");

      assertValidationSuccess((qInstance) -> qInstance.withTableCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(CustomizerValid.class)));
   }



   /*******************************************************************************
    ** Test an instance with null backends - should throw.
    **
    *******************************************************************************/
   @Test
   public void test_validateNullBackends()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.setBackends(null),
         "At least 1 backend must be defined");
   }



   /*******************************************************************************
    ** Test an instance with empty map of backends - should throw.
    **
    *******************************************************************************/
   @Test
   public void test_validateEmptyBackends()
   {
      assertValidationFailureReasonsAllowingExtraReasons((qInstance) -> qInstance.setBackends(new HashMap<>()),
         "At least 1 backend must be defined");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBackendVariants()
   {
      BackendVariantSetting setting = new BackendVariantSetting() {};

      assertValidationFailureReasons((qInstance) -> qInstance.addBackend(new QBackendMetaData()
            .withName("variant")
            .withUsesVariants(true)),
         "Missing backendVariantsConfig in backend [variant] which is marked as usesVariants");

      assertValidationFailureReasons((qInstance) -> qInstance.addBackend(new QBackendMetaData()
            .withName("variant")
            .withUsesVariants(false)
            .withBackendVariantsConfig(new BackendVariantsConfig())),
         "Should not have a backendVariantsConfig");

      assertValidationFailureReasons((qInstance) -> qInstance.addBackend(new QBackendMetaData()
            .withName("variant")
            .withUsesVariants(null)
            .withBackendVariantsConfig(new BackendVariantsConfig())),
         "Should not have a backendVariantsConfig");

      assertValidationFailureReasons((qInstance) -> qInstance.addBackend(new QBackendMetaData()
            .withName("variant")
            .withUsesVariants(true)
            .withBackendVariantsConfig(new BackendVariantsConfig())),
         "Missing variantTypeKey in backendVariantsConfig",
         "Missing optionsTableName in backendVariantsConfig",
         "Missing or empty backendSettingSourceFieldNameMap");

      assertValidationFailureReasons((qInstance) -> qInstance.addBackend(new QBackendMetaData()
            .withName("variant")
            .withUsesVariants(true)
            .withBackendVariantsConfig(new BackendVariantsConfig()
               .withVariantTypeKey("myVariant")
               .withOptionsTableName("notATable")
               .withBackendSettingSourceFieldNameMap(Map.of(setting, "field")))),
         "Unrecognized optionsTableName [notATable] in backendVariantsConfig");

      assertValidationFailureReasons((qInstance) -> qInstance.addBackend(new QBackendMetaData()
            .withName("variant")
            .withUsesVariants(true)
            .withBackendVariantsConfig(new BackendVariantsConfig()
               .withVariantTypeKey("myVariant")
               .withOptionsTableName(TestUtils.TABLE_NAME_PERSON)
               .withOptionsFilter(new QQueryFilter(new QFilterCriteria("notAField", QCriteriaOperator.EQUALS, 1)))
               .withBackendSettingSourceFieldNameMap(Map.of(setting, "firstName")))),
         "optionsFilter in backendVariantsConfig in backend [variant]: Criteria fieldName notAField is not a field");

      assertValidationFailureReasons((qInstance) -> qInstance.addBackend(new QBackendMetaData()
            .withName("variant")
            .withUsesVariants(true)
            .withBackendVariantsConfig(new BackendVariantsConfig()
               .withVariantTypeKey("myVariant")
               .withOptionsTableName(TestUtils.TABLE_NAME_PERSON)
               .withBackendSettingSourceFieldNameMap(Map.of(setting, "noSuchField")))),
         "Unrecognized fieldName [noSuchField] in backendSettingSourceFieldNameMap");

      assertValidationFailureReasons((qInstance) -> qInstance.addBackend(new QBackendMetaData()
            .withName("variant")
            .withUsesVariants(true)
            .withBackendVariantsConfig(new BackendVariantsConfig()
               .withVariantTypeKey("myVariant")
               .withOptionsTableName(TestUtils.TABLE_NAME_PERSON)
               .withVariantRecordLookupFunction(new QCodeReference(CustomizerThatIsNotOfTheRightBaseClass.class))
               .withBackendSettingSourceFieldNameMap(Map.of(setting, "no-field-but-okay-custom-supplier"))
            )),
         "VariantRecordSupplier in backendVariantsConfig in backend [variant]: CodeReference is not any of the expected types: com.kingsrook.qqq.backend.core.utils.lambdas.UnsafeFunction, java.util.function.Function");

      assertValidationSuccess((qInstance) -> qInstance.addBackend(new QBackendMetaData()
         .withName("variant")
         .withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig()
            .withVariantTypeKey("myVariant")
            .withOptionsTableName(TestUtils.TABLE_NAME_PERSON)
            .withBackendSettingSourceFieldNameMap(Map.of(setting, "firstName"))
         )));

      assertValidationSuccess((qInstance) -> qInstance.addBackend(new QBackendMetaData()
         .withName("variant")
         .withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig()
            .withVariantTypeKey("myVariant")
            .withOptionsTableName(TestUtils.TABLE_NAME_PERSON)
            .withVariantRecordLookupFunction(new QCodeReference(VariantRecordFunction.class))
            .withBackendSettingSourceFieldNameMap(Map.of(setting, "no-field-but-okay-custom-supplier"))
         )));

      assertValidationSuccess((qInstance) -> qInstance.addBackend(new QBackendMetaData()
         .withName("variant")
         .withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig()
            .withVariantTypeKey("myVariant")
            .withOptionsTableName(TestUtils.TABLE_NAME_PERSON)
            .withVariantRecordLookupFunction(new QCodeReference(VariantRecordUnsafeFunction.class))
            .withBackendSettingSourceFieldNameMap(Map.of(setting, "no-field-but-okay-custom-supplier"))
         )));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class VariantRecordFunction implements Function<Serializable, QRecord>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QRecord apply(Serializable serializable)
      {
         return null;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class VariantRecordUnsafeFunction implements UnsafeFunction<Serializable, QRecord, QException>
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public QRecord apply(Serializable serializable) throws QException
      {
         return null;
      }
   }



   /*******************************************************************************
    ** Test an instance with null tables - should throw.
    **
    *******************************************************************************/
   @Test
   public void test_validateNullTablesAndProcesses()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         qInstance.setTables(null);
         qInstance.setProcesses(null);
      },
         true,
         "At least 1 table must be defined");
   }



   /*******************************************************************************
    ** Test an instance with empty map of tables - should throw.
    **
    *******************************************************************************/
   @Test
   public void test_validateEmptyTablesAndProcesses()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         qInstance.setTables(new HashMap<>());
         qInstance.setProcesses(new HashMap<>());
      },
         true,
         "At least 1 table must be defined");
   }



   /*******************************************************************************
    ** Test an instance where a table and a backend each have a name attribute that
    ** doesn't match the key that those objects have in the instance's maps - should throw.
    **
    *******************************************************************************/
   @Test
   public void test_validateInconsistentNames()
   {
      assertValidationFailureReasonsAllowingExtraReasons((qInstance) ->
         {
            qInstance.getTable("person").setName("notPerson");
            qInstance.getBackend("default").setName("notDefault");
            qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).setName("notGreetPeople");
            qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_STATE).setName("notStates");
            qInstance.getAutomationProvider(TestUtils.POLLING_AUTOMATION).setName("notPolling");
         },
         "Inconsistent naming for table",
         "Inconsistent naming for backend",
         "Inconsistent naming for process",
         "Inconsistent naming for possibleValueSource",
         "Inconsistent naming for automationProvider"
      );
   }



   /*******************************************************************************
    ** Test that if a table has a null backend, that it fails.
    **
    *******************************************************************************/
   @Test
   public void test_validateTableWithoutBackend()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").setBackendName(null),
         "Missing backend name for table");
   }



   /*******************************************************************************
    ** Test that if a table specifies a backend that doesn't exist, that it fails.
    **
    *******************************************************************************/
   @Test
   public void test_validateTableWithMissingBackend()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").setBackendName("notARealBackend"),
         "Unrecognized backend");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_validateTableBadRecordFormatField()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withRecordLabelFields("notAField"),
         "not a field");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableFieldInlinePossibleValueSource()
   {
      ////////////////////////////////////////////////////
      // make sure can't have both named and inline PVS //
      ////////////////////////////////////////////////////
      assertValidationFailureReasonsAllowingExtraReasons((qInstance) -> qInstance.getTable("person").getField("homeStateId")
            .withInlinePossibleValueSource(new QPossibleValueSource().withType(QPossibleValueSourceType.TABLE).withTableName("person")),
         "both a possibleValueSourceName and an inlinePossibleValueSource");

      /////////////////////////////////////////////
      // make require inline PVS to be enum type //
      /////////////////////////////////////////////
      assertValidationFailureReasonsAllowingExtraReasons((qInstance) -> qInstance.getTable("person").getField("homeStateId")
            .withPossibleValueSourceName(null)
            .withInlinePossibleValueSource(new QPossibleValueSource().withType(QPossibleValueSourceType.TABLE)),
         "must have a type of ENUM");

      ////////////////////////////////////////////////////
      // make sure validation on the inline PVS happens //
      ////////////////////////////////////////////////////
      assertValidationFailureReasonsAllowingExtraReasons((qInstance) -> qInstance.getTable("person").getField("homeStateId")
            .withPossibleValueSourceName(null)
            .withInlinePossibleValueSource(new QPossibleValueSource().withType(QPossibleValueSourceType.ENUM)),
         "missing enum values");
   }



   /*******************************************************************************
    ** Test that if a process specifies a table that doesn't exist, that it fails.
    **
    *******************************************************************************/
   @Test
   public void test_validateProcessWithMissingTable()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).setTableName("notATableName"),
         "Unrecognized table");
   }



   /*******************************************************************************
    ** Test rules for process step names (must be set; must not be duplicated)
    **
    *******************************************************************************/
   @Test
   public void test_validateProcessStepNames()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE).getStepList().get(0).setName(null),
         "Missing name for a step at index");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE).getStepList().get(0).setName(""),
         "Missing name for a step at index");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE).getStepList().forEach(s -> s.setName("myStep")),
         "Duplicate step name [myStep]", "Duplicate step name [myStep]");
   }



   /*******************************************************************************
    ** Test that a process with a step that is a private class fails
    **
    *******************************************************************************/
   @Test
   public void test_validateProcessWithPrivateStep()
   {
      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(
         ExtractViaQueryStep.class,
         TestPrivateClass.class,
         LoadViaDeleteStep.class,
         new HashMap<>()
      );
      process.setName("testProcess");
      process.setLabel("Test Process");
      process.setTableName(TestUtils.defineTablePerson().getName());

      assertValidationFailureReasons((qInstance) -> qInstance.addProcess(process),
         "is not public");
   }



   /*******************************************************************************
    ** Test that a process with a step that does not have a no-args constructor fails
    **
    *******************************************************************************/
   @Test
   public void test_validateProcessWithNoArgsConstructorStep()
   {
      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(
         ExtractViaQueryStep.class,
         TestNoArgsConstructorClass.class,
         LoadViaDeleteStep.class,
         new HashMap<>()
      );
      process.setName("testProcess");
      process.setLabel("Test Process");
      process.setTableName(TestUtils.defineTablePerson().getName());

      assertValidationFailureReasons((qInstance) -> qInstance.addProcess(process),
         "parameterless constructor");
   }



   /*******************************************************************************
    ** Test that a process with a step that is an abstract class fails
    **
    *******************************************************************************/
   @Test
   public void test_validateProcessWithAbstractStep()
   {
      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(
         ExtractViaQueryStep.class,
         TestAbstractClass.class,
         LoadViaDeleteStep.class,
         new HashMap<>()
      );
      process.setName("testProcess");
      process.setLabel("Test Process");
      process.setTableName(TestUtils.defineTablePerson().getName());

      assertValidationFailureReasons((qInstance) -> qInstance.addProcess(process),
         "because it is abstract");
   }



   /*******************************************************************************
    ** Test that a process with no steps fails
    **
    *******************************************************************************/
   @Test
   public void test_validateProcessWithNoSteps()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).setStepList(Collections.emptyList()),
         "At least 1 step");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).setStepList(null),
         "At least 1 step");
   }



   /*******************************************************************************
    ** Test that a process step with an empty string name fails
    **
    *******************************************************************************/
   @Test
   public void test_validateProcessStepWithEmptyName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).getStepList().get(0).setName(""),
         "Missing name for a step");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE).getStepList().get(1).setName(null),
         "Missing name for a step");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_validateProcessCancelSteps()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).withCancelStep(new QBackendStepMetaData()),
         "Cancel step is missing a code reference");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).withCancelStep(new QBackendStepMetaData().withCode(new QCodeReference())),
         "missing a code reference name", "missing a code type");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).withCancelStep(new QBackendStepMetaData().withCode(new QCodeReference(ValidAuthCustomizer.class))),
         "CodeReference is not of the expected type");

      assertValidationSuccess((qInstance) -> qInstance.getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE).withCancelStep(new QBackendStepMetaData().withCode(new QCodeReference(CancelProcessActionTest.CancelStep.class))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_validateSchedules()
   {
      String processName = TestUtils.PROCESS_NAME_GREET_PEOPLE;
      Supplier<QScheduleMetaData> baseScheduleMetaData = () -> new QScheduleMetaData()
         .withSchedulerName(TestUtils.SIMPLE_SCHEDULER_NAME);

      ////////////////////////////////////////////////////
      // do our basic schedule validations on a process //
      ////////////////////////////////////////////////////
      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()),
         "either repeatMillis or repeatSeconds or cronExpression must be set");

      String validCronString = "* * * * * ?";
      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withRepeatMillis(1)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "both a repeat time and cronExpression may not be set");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withRepeatSeconds(1)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "both a repeat time and cronExpression may not be set");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withRepeatSeconds(1)
            .withRepeatMillis(1)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "both a repeat time and cronExpression may not be set");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withInitialDelaySeconds(1)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "cron schedule may not have an initial delay");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withInitialDelayMillis(1)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "cron schedule may not have an initial delay");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withInitialDelaySeconds(1)
            .withInitialDelayMillis(1)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "cron schedule may not have an initial delay");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withCronExpression(validCronString)),
         "must specify a cronTimeZoneId");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withCronExpression(validCronString)
            .withCronTimeZoneId("foobar")),
         "unrecognized cronTimeZoneId: foobar");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withCronExpression("* * * * * *")
            .withCronTimeZoneId("UTC")),
         "invalid cron expression: Support for specifying both");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withCronExpression("x")
            .withCronTimeZoneId("UTC")),
         "invalid cron expression: Illegal cron expression format");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withRepeatSeconds(10)
            .withCronTimeZoneId("UTC")),
         "non-cron schedule must not specify a cronTimeZoneId");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withSchedulerName(null)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "is missing a scheduler name");

      assertValidationFailureReasons((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get()
            .withSchedulerName("not-a-scheduler")
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "referencing an unknown scheduler name: not-a-scheduler");

      /////////////////////////////////
      // validate some success cases //
      /////////////////////////////////
      assertValidationSuccess((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get().withRepeatSeconds(1)));
      assertValidationSuccess((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get().withRepeatMillis(1)));
      assertValidationSuccess((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get().withCronExpression(validCronString).withCronTimeZoneId("UTC")));
      assertValidationSuccess((qInstance) -> qInstance.getProcess(processName).withSchedule(baseScheduleMetaData.get().withCronExpression(validCronString).withCronTimeZoneId("America/New_York")));

      ///////////////////////////////////////////////////////////////
      // make sure table automations get their schedules validated //
      ///////////////////////////////////////////////////////////////
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().withSchedule(baseScheduleMetaData.get()
            .withSchedulerName(null)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "is missing a scheduler name");

      ////////////////////////////////////////////////////
      // make sure queues get their schedules validated //
      ////////////////////////////////////////////////////
      assertValidationFailureReasons((qInstance) -> (qInstance.getQueue(TestUtils.TEST_SQS_QUEUE)).withSchedule(baseScheduleMetaData.get()
            .withSchedulerName(null)
            .withCronExpression(validCronString)
            .withCronTimeZoneId("UTC")),
         "is missing a scheduler name");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_validatorPlugins()
   {
      try
      {
         QInstanceValidator.addValidatorPlugin(new AlwaysFailsProcessValidatorPlugin());

         ////////////////////////////////////////
         // make sure our always failer fails. //
         ////////////////////////////////////////
         assertValidationFailureReasonsAllowingExtraReasons((qInstance) ->
         {
         }, "always fail");
      }
      finally
      {
         QInstanceValidator.removeAllValidatorPlugins();

         ////////////////////////////////////////////////////
         // make sure if remove all plugins, we don't fail //
         ////////////////////////////////////////////////////
         assertValidationSuccess((qInstance) ->
         {
         });
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_validatorPluginIsEnabled()
   {
      try
      {
         ////////////////////////////////////////////////////////////////////
         // enable this plugin, then make sure it runs (causing a failure) //
         ////////////////////////////////////////////////////////////////////
         QInstanceValidator.addValidatorPlugin(new FailsIfEnabledProcessValidatorPlugin(true));

         ///////////////////////////////////////////////
         // make sure it fails, because it is enabled //
         ///////////////////////////////////////////////
         assertValidationFailureReasonsAllowingExtraReasons((qInstance) ->
         {
         }, "I fail, because i am enabled.");

         ///////////////////////////////////////////////////
         // try again, this time with the plugin disabled //
         ///////////////////////////////////////////////////
         QInstanceValidator.removeAllValidatorPlugins();
         QInstanceValidator.addValidatorPlugin(new FailsIfEnabledProcessValidatorPlugin(false));

         ///////////////////////////////////////////////////////////////////////////////
         // make sure it doesn't fail - because it didn't run, because it is disabled //
         ///////////////////////////////////////////////////////////////////////////////
         assertValidationSuccess((qInstance) ->
         {
         });
      }
      finally
      {
         QInstanceValidator.removeAllValidatorPlugins();
      }
   }



   /*******************************************************************************
    ** Test that a table with no fields fails.
    **
    *******************************************************************************/
   @Test
   public void test_validateTableWithNoFields()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").setFields(null),
         "At least 1 field", "Primary key for table person is not a recognized field");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").setFields(new HashMap<>()),
         "At least 1 field", "Primary key for table person is not a recognized field");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableCustomizers()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference()),
         "missing a code reference name", "missing a code type");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(null, QCodeType.JAVA)),
         "missing a code reference name");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference("", QCodeType.JAVA)),
         "missing a code reference name");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference("Test", null)),
         "missing a code type");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference("Test", QCodeType.JAVA)),
         "Class for Test could not be found");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(CustomizerWithNoVoidConstructor.class)),
         "Instance of " + CustomizerWithNoVoidConstructor.class.getSimpleName() + " could not be created");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(CustomizerThatIsNotOfTheRightBaseClass.class)),
         "CodeReference is not of the expected type");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(CustomizerWithOnlyPrivateConstructor.class)),
         "it does not have a public parameterless constructor");

      /////////////////////////////////////////////
      // this class actually works, so, :shrug:? //
      /////////////////////////////////////////////
      // assertValidationFailureReasons((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(CustomizerWithPrivateVisibility.class, QCodeUsage.CUSTOMIZER)),
      //    "it does not have a public parameterless constructor");

      assertValidationSuccess((qInstance) -> qInstance.getTable("person").withCustomizer(TableCustomizers.POST_QUERY_RECORD.getRole(), new QCodeReference(CustomizerValid.class)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CustomizerWithNoVoidConstructor
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public CustomizerWithNoVoidConstructor(boolean b)
      {

      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static class CustomizerWithPrivateVisibility extends AbstractPostQueryCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public CustomizerWithPrivateVisibility()
      {
         System.out.println("eh?");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         return (records);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CustomizerWithOnlyPrivateConstructor
   {
      /***************************************************************************
       **
       ***************************************************************************/
      private CustomizerWithOnlyPrivateConstructor()
      {

      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CustomizerThatIsNotOfTheRightBaseClass
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CustomizerValid extends AbstractPostQueryCustomizer
   {
      @Override
      public List<QRecord> apply(List<QRecord> records)
      {
         return (records);
      }
   }



   /*******************************************************************************
    ** Test that if a field specifies a backend that doesn't exist, that it fails.
    **
    *******************************************************************************/
   @Test
   public void test_validateFieldWithMissingPossibleValueSource()
   {
      assertValidationFailureReasonsAllowingExtraReasons((qInstance) -> qInstance.getTable("person").getField("homeStateId").setPossibleValueSourceName("not a real possible value source"),
         "unrecognized possibleValueSourceName");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testChildrenWithBadParentAppName()
   {
      String[] reasons = new String[] { "Unrecognized parent app", "does not have its parent app properly set" };
      assertValidationFailureReasons((qInstance) -> qInstance.getApp(TestUtils.APP_NAME_GREETINGS).setParentAppName("notAnApp"), reasons);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppCircularReferences()
   {
      assertValidationFailureReasonsAllowingExtraReasons((qInstance) ->
      {
         QAppMetaData miscApp      = qInstance.getApp(TestUtils.APP_NAME_MISCELLANEOUS);
         QAppMetaData greetingsApp = qInstance.getApp(TestUtils.APP_NAME_GREETINGS);

         miscApp.withChild(greetingsApp);
         greetingsApp.withChild(miscApp);
      }, "Circular app reference");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSectionsMissingName()
   {
      QTableMetaData table = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection(null, "Section 1", new QIcon("person"), Tier.T1, List.of("id")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table), "Missing a name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSectionsMissingLabel()
   {
      QTableMetaData table = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", null, new QIcon("person"), Tier.T1, List.of("id")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      assertValidationSuccess((qInstance) -> qInstance.addTable(table));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSectionDuplicateName()
   {
      QTableMetaData table = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of("id")))
         .withSection(new QFieldSection("section1", "Section 2", new QIcon("person"), Tier.T2, List.of("name")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.INTEGER));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table), "more than 1 section named");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSectionDuplicateLabel()
   {
      QTableMetaData table = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of("id")))
         .withSection(new QFieldSection("section2", "Section 1", new QIcon("person"), Tier.T2, List.of("name")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.INTEGER));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table), "more than 1 section labeled");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSectionsNoFields()
   {
      QTableMetaData table1 = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of()))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table1), "section1 does not have any fields", "field id is not listed in any field sections");

      QTableMetaData table2 = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, null))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table2), "section1 does not have any fields", "field id is not listed in any field sections");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSectionsUnrecognizedFieldName()
   {
      QTableMetaData table = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of("id", "od")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table), "not a field on this table");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSectionsDuplicatedFieldName()
   {
      QTableMetaData table1 = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of("id", "id")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table1), "more than once");

      QTableMetaData table2 = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of("id")))
         .withSection(new QFieldSection("section2", "Section 2", new QIcon("person"), Tier.T2, List.of("id")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table2), "more than once");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldNotInAnySections()
   {
      QTableMetaData table = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of("id")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table), "not listed in any field sections");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSectionsMultipleTier1()
   {
      QTableMetaData table = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of("id")))
         .withSection(new QFieldSection("section2", "Section 2", new QIcon("person"), Tier.T1, List.of("name")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table), "more than 1 section listed as Tier 1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppSectionsMissingName()
   {
      QAppMetaData app = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection(null, "Section 1", new QIcon("person"), List.of("test"), null, null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app), "Missing a name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppSectionsMissingLabel()
   {
      ///////////////////////////////////////////////////////////////////////////////////
      // the enricher makes a label from the name, so, we'll just make them both null. //
      ///////////////////////////////////////////////////////////////////////////////////
      QAppMetaData app = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection(null, null, new QIcon("person"), List.of("test"), null, null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app), "Missing a label", "Missing a name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppSectionsNoFields()
   {
      QAppMetaData app1 = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), List.of(), List.of(), null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app1), "section1 does not have any children", "child test is not listed in any app sections");

      QAppMetaData app2 = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), null, null, null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app2), "section1 does not have any children", "child test is not listed in any app sections");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppSectionsUnrecognizedFieldName()
   {
      QAppMetaData app1 = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), List.of("test", "tset"), null, null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app1), "not a child of this app");
      QAppMetaData app2 = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), List.of("test"), List.of("tset"), null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app2), "not a child of this app");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppSectionsDuplicatedFieldName()
   {
      QAppMetaData app1 = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), List.of("test", "test"), null, null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app1), "more than once");

      QAppMetaData app2 = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), List.of("test"), null, null))
         .withSection(new QAppSection("section2", "Section 2", new QIcon("person"), List.of("test"), null, null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app2), "more than once");

      QAppMetaData app3 = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), List.of("test"), List.of("test"), null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app3), "more than once");

      QAppMetaData app4 = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), null, List.of("test", "test"), null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app4), "more than once");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppChildNotInAnySections()
   {
      QTableMetaData table = new QTableMetaData().withName("test")
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withSection(new QFieldSection("section1", "Section 1", new QIcon("person"), Tier.T1, List.of("id")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING));
      assertValidationFailureReasons((qInstance) -> qInstance.addTable(table), "not listed in any field sections");

      QAppMetaData app = new QAppMetaData().withName("test")
         .withChild(new QTableMetaData().withName("tset"))
         .withChild(new QTableMetaData().withName("test"))
         .withSection(new QAppSection("section1", "Section 1", new QIcon("person"), List.of("test"), null, null));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app), "not listed in any app sections");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAppUnrecognizedWidgetName()
   {
      QAppMetaData app = new QAppMetaData().withName("test")
         .withWidgets(List.of("no-such-widget"));
      assertValidationFailureReasons((qInstance) -> qInstance.addApp(app), "not a recognized widget");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSectionsWithJoinFields()
   {
      Consumer<QTableMetaData> putAllFieldsInASection = table -> table.addSection(new QFieldSection()
         .withName("section0")
         .withTier(Tier.T1)
         .withFieldNames(new ArrayList<>(table.getFields().keySet())));

      assertValidationFailureReasons(qInstance ->
      {
         QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_ORDER);
         putAllFieldsInASection.accept(table);
         table.getSections().get(0).getFieldNames().add(TestUtils.TABLE_NAME_LINE_ITEM + ".sku");
      }, "orderLine.sku references an is-many join, which is not supported");

      assertValidationSuccess(qInstance ->
      {
         QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM);
         putAllFieldsInASection.accept(table);
         table.getSections().get(0).getFieldNames().add(TestUtils.TABLE_NAME_ORDER + ".orderNo");
      });

      assertValidationFailureReasons(qInstance ->
      {
         QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM);
         putAllFieldsInASection.accept(table);
         table.getSections().get(0).getFieldNames().add(TestUtils.TABLE_NAME_ORDER + ".asdf");
      }, "order.asdf specifies a fieldName [asdf] which does not exist in that table [order].");

      /////////////////////////////////////////////////////////////////////////////
      // this is aactually allowed, well, just not considered as a join-field... //
      /////////////////////////////////////////////////////////////////////////////
      // assertValidationFailureReasons(qInstance ->
      // {
      //    QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM);
      //    putAllFieldsInASection.accept(table);
      //    table.getSections().get(0).getFieldNames().add("foo.bar");
      // }, "unrecognized table name [foo]");

      assertValidationFailureReasons(qInstance ->
      {
         QTableMetaData table = qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM);
         putAllFieldsInASection.accept(table);
         table.getSections().get(0).getFieldNames().add(TestUtils.TABLE_NAME_SHAPE + ".id");
      }, "[shape] which is not an exposed join on this table");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueSourceMissingType()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_STATE).setType(null),
         "Missing type for possibleValueSource");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueSourceMisConfiguredEnum()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         QPossibleValueSource possibleValueSource = qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_STATE);
         possibleValueSource.setTableName("person");
         possibleValueSource.setSearchFields(List.of("id"));
         possibleValueSource.setOrderByFields(List.of(new QFilterOrderBy("id")));
         possibleValueSource.setCustomCodeReference(new QCodeReference());
         possibleValueSource.setEnumValues(null);
         possibleValueSource.setType(QPossibleValueSourceType.ENUM);
      },
         "should not have a tableName",
         "should not have searchFields",
         "should not have orderByFields",
         "should not have a customCodeReference",
         "is missing enum values",
         "is missing its idType.");

      assertValidationFailureReasons((qInstance) -> qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_STATE).setEnumValues(new ArrayList<>()),
         "is missing enum values",
         "is missing its idType.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueSourceMisConfiguredTable()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         QPossibleValueSource possibleValueSource = qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE);
         possibleValueSource.setTableName(null);
         possibleValueSource.setSearchFields(null);
         possibleValueSource.setOrderByFields(new ArrayList<>());
         possibleValueSource.setCustomCodeReference(new QCodeReference());
         possibleValueSource.setEnumValues(List.of(new QPossibleValue<>("test")));
         possibleValueSource.setType(QPossibleValueSourceType.TABLE);
      },
         "should not have enum values",
         "should not have a customCodeReference",
         "is missing a tableName",
         "is missing searchFields",
         "is missing orderByFields",
         "is missing its idType.");

      assertValidationFailureReasons((qInstance) -> qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE).setTableName("Not a table"),
         "Unrecognized table",
         "is missing its idType.");

      assertValidationFailureReasons((qInstance) -> qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_SHAPE).setSearchFields(List.of("id", "notAField", "name")),
         "unrecognized searchField: notAField");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueSourceMisConfiguredCustom()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         QPossibleValueSource possibleValueSource = qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_CUSTOM);
         possibleValueSource.setTableName("person");
         possibleValueSource.setSearchFields(List.of("id"));
         possibleValueSource.setOrderByFields(List.of(new QFilterOrderBy("id")));
         possibleValueSource.setCustomCodeReference(null);
         possibleValueSource.setEnumValues(List.of(new QPossibleValue<>("test")));
         possibleValueSource.setType(QPossibleValueSourceType.CUSTOM);
      },
         "should not have enum values",
         "should not have a tableName",
         "should not have searchFields",
         "should not have orderByFields",
         "is missing a customCodeReference",
         "is missing its idType.");

      assertValidationFailureReasons((qInstance) -> qInstance.getPossibleValueSource(TestUtils.POSSIBLE_VALUE_SOURCE_CUSTOM).setCustomCodeReference(new QCodeReference()),
         "missing a code reference name",
         "missing a code type",
         "is missing its idType.");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAutomationProviderType()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getAutomationProvider(TestUtils.POLLING_AUTOMATION).setType(null),
         "Missing type for automationProvider");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationProviderName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().setProviderName(null),
         "is missing a providerName");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().setProviderName(""),
         "is missing a providerName");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().setProviderName("notARealProvider"),
         "unrecognized providerName");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationStatusTracking()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().setStatusTracking(null),
         "do not have statusTracking");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationStatusTrackingType()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().getStatusTracking().setType(null),
         "statusTracking is missing a type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationStatusTrackingFieldName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().getStatusTracking().setFieldName(null),
         "missing its fieldName");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().getStatusTracking().setFieldName(""),
         "missing its fieldName");

      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().getStatusTracking().setFieldName("notARealField"),
         "not a defined field");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationActionsNames()
   {
      assertValidationFailureReasons((qInstance) -> getAction0(qInstance).setName(null),
         "action missing a name");

      assertValidationFailureReasons((qInstance) -> getAction0(qInstance).setName(""),
         "action missing a name");

      assertValidationFailureReasons((qInstance) ->
      {
         List<TableAutomationAction> actions = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().getActions();
         actions.add(actions.get(0));
      },
         "more than one action named");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationActionTriggerEvent()
   {
      assertValidationFailureReasons((qInstance) -> getAction0(qInstance).setTriggerEvent(null),
         "missing a triggerEvent");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationActionCodeReference()
   {
      assertValidationFailureReasons((qInstance) -> getAction0(qInstance).setCodeReference(new QCodeReference()),
         "missing a code reference name", "missing a code type");

      assertValidationFailureReasons((qInstance) -> getAction0(qInstance).setCodeReference(new QCodeReference(TestUtils.CustomPossibleValueSource.class)),
         "is not of the expected type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationActionProcessName()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         TableAutomationAction action = getAction0(qInstance);
         action.setCodeReference(null);
         action.setProcessName("notAProcess");
      },
         "unrecognized processName");

      assertValidationFailureReasons((qInstance) ->
      {
         TableAutomationAction action = getAction0(qInstance);
         action.setCodeReference(null);
         action.setProcessName(TestUtils.PROCESS_NAME_BASEPULL);
      },
         "different table");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationActionCodeReferenceAndProcessName()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         TableAutomationAction action = getAction0(qInstance);
         action.setCodeReference(null);
         action.setProcessName(null);
      },
         "missing both");

      assertValidationFailureReasons((qInstance) ->
      {
         TableAutomationAction action = getAction0(qInstance);
         action.setCodeReference(new QCodeReference(TestUtils.CheckAge.class));
         action.setProcessName(TestUtils.PROCESS_NAME_INCREASE_BIRTHDATE);
      },
         "has both");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableAutomationActionFilter()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         TableAutomationAction action = getAction0(qInstance);
         action.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria())
         );
      },
         "without a field name", "without an operator");

      assertValidationFailureReasons((qInstance) ->
      {
         TableAutomationAction action = getAction0(qInstance);
         action.setFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("notAField", QCriteriaOperator.EQUALS, Collections.emptyList()))
         );
      },
         "unrecognized field");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyNoFields()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withUniqueKey(new UniqueKey()),
         "uniqueKey with no fields");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyDuplicatedField()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withUniqueKey(new UniqueKey().withFieldName("id").withFieldName("id")),
         "the same field multiple times");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyInvalidField()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withUniqueKey(new UniqueKey().withFieldName("notAField")),
         "unrecognized field name: notAField");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueKeyDuplicatedUniqueKeys()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
            .withUniqueKey(new UniqueKey().withFieldName("id"))
            .withUniqueKey(new UniqueKey().withFieldName("id")),
         "more than one uniqueKey with the same set of fields");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValidUniqueKeys()
   {
      assertValidationSuccess((qInstance) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withUniqueKey(new UniqueKey().withFieldName("id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueueProviderName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getQueueProvider(TestUtils.DEFAULT_QUEUE_PROVIDER).withName(null),
         "Inconsistent naming for queueProvider");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueueProvider(TestUtils.DEFAULT_QUEUE_PROVIDER).withName(""),
         "Inconsistent naming for queueProvider");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueueProvider(TestUtils.DEFAULT_QUEUE_PROVIDER).withName("wrongName"),
         "Inconsistent naming for queueProvider");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueueProviderType()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getQueueProvider(TestUtils.DEFAULT_QUEUE_PROVIDER).withType(null),
         "Missing type for queueProvider");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueueProviderSQSAttributes()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         SQSQueueProviderMetaData queueProvider = (SQSQueueProviderMetaData) qInstance.getQueueProvider(TestUtils.DEFAULT_QUEUE_PROVIDER);
         queueProvider.setAccessKey(null);
         queueProvider.setSecretKey("");
         queueProvider.setRegion(null);
         queueProvider.setBaseURL("");
      },
         "Missing accessKey", "Missing secretKey", "Missing region", "Missing baseURL");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueueName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withName(null),
         "Inconsistent naming for queue");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withName(""),
         "Inconsistent naming for queue");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withName("wrongName"),
         "Inconsistent naming for queue");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueueQueueProviderName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withProviderName(null),
         "Unrecognized queue providerName");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withProviderName(""),
         "Unrecognized queue providerName");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withProviderName("wrongName"),
         "Unrecognized queue providerName");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueueQueueName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withQueueName(null),
         "Missing queueName for queue");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withQueueName(""),
         "Missing queueName for queue");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQueueProcessName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withProcessName(null),
         "Missing processName for queue");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withProcessName(""),
         "Missing processName for queue");

      assertValidationFailureReasons((qInstance) -> qInstance.getQueue("testSQSQueue").withProcessName("notAProcess"),
         "Unrecognized processName for queue:");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportName()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).withName(null),
         "Inconsistent naming for report");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).withName(""),
         "Inconsistent naming for report");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).withName("wrongName"),
         "Inconsistent naming for report");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportNoDataSources()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).withDataSources(null),
         "At least 1 data source",
         "unrecognized dataSourceName");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).withDataSources(new ArrayList<>()),
         "At least 1 data source",
         "unrecognized dataSourceName");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportDataSourceNames()
   {
      assertValidationFailureReasons((qInstance) ->
      {
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // enricher will give us a default name if only 1 data source, so, set 1st one to null name, then add a second //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QReportMetaData report = qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON);
         report.setDataSources(new ArrayList<>(report.getDataSources()));
         report.getDataSources().get(0).setName(null);
         report.getDataSources().add(new QReportDataSource()
            .withName("2nd")
            .withSourceTable(TestUtils.TABLE_NAME_PERSON)
         );
      },
         "Missing name for a dataSource",
         "unrecognized dataSourceName");

      assertValidationFailureReasons((qInstance) ->
      {
         ///////////////////////////////////
         // same as above, but "" vs null //
         ///////////////////////////////////
         QReportMetaData report = qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON);
         report.setDataSources(new ArrayList<>(report.getDataSources()));
         report.getDataSources().get(0).setName("");
         report.getDataSources().add(new QReportDataSource()
            .withName("2nd")
            .withSourceTable(TestUtils.TABLE_NAME_PERSON)
         );
      },
         "Missing name for a dataSource",
         "unrecognized dataSourceName");

      assertValidationFailureReasons((qInstance) ->
      {
         List<QReportDataSource> dataSources = new ArrayList<>(qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources());
         dataSources.add(dataSources.get(0));
         qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).setDataSources(dataSources);
      },
         "More than one dataSource with name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportDataSourceTables()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).setSourceTable("notATable"),
         "is not a table in this instance");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).setSourceTable(null),
         "does not have a sourceTable");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).setSourceTable(""),
         "does not have a sourceTable");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportDataSourceTablesFilter()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).getQueryFilter().getCriteria().get(0).setFieldName(null),
         "Missing fieldName for a criteria");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).getQueryFilter().getCriteria().get(0).setFieldName("notAField"),
         "is not a field in this table");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).getQueryFilter().getCriteria().get(0).setOperator(null),
         "Missing operator for a criteria");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).getQueryFilter().withOrderBy(new QFilterOrderBy(null)),
         "Missing fieldName for an orderBy");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).getQueryFilter().withOrderBy(new QFilterOrderBy("notAField")),
         "is not a field in this table");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportDataSourceStaticDataSupplier()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0).withStaticDataSupplier(new QCodeReference(TestReportStaticDataSupplier.class)),
         "has both a sourceTable and a staticDataSupplier");

      assertValidationFailureReasons((qInstance) ->
      {
         QReportDataSource dataSource = qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0);
         dataSource.setSourceTable(null);
         dataSource.setStaticDataSupplier(new QCodeReference(null, QCodeType.JAVA));
      }, "missing a code reference name");

      assertValidationFailureReasons((qInstance) ->
      {
         QReportDataSource dataSource = qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0);
         dataSource.setSourceTable(null);
         dataSource.setStaticDataSupplier(new QCodeReference(ArrayList.class));
      }, "is not of the expected type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportDataSourceCustomRecordSource()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0)
            .withSourceTable(null)
            .withStaticDataSupplier(new QCodeReference(TestReportStaticDataSupplier.class))
            .withCustomRecordSource(new QCodeReference(TestReportCustomRecordSource.class)),
         "has both a staticDataSupplier and a customRecordSource");

      assertValidationFailureReasons((qInstance) ->
      {
         QReportDataSource dataSource = qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0);
         dataSource.setSourceTable(null);
         dataSource.setCustomRecordSource(new QCodeReference(null, QCodeType.JAVA));
      }, "missing a code reference name");

      assertValidationFailureReasons((qInstance) ->
      {
         QReportDataSource dataSource = qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getDataSources().get(0);
         dataSource.setSourceTable(null);
         dataSource.setCustomRecordSource(new QCodeReference(ArrayList.class));
      }, "is not of the expected type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportViewBasics()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).setViews(null),
         "At least 1 view must be defined in report");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).setViews(new ArrayList<>()),
         "At least 1 view must be defined in report");

      /////////////////////////////////////////////////////////////////////////
      // meh, enricher sets a default name, so, can't easily catch this one. //
      /////////////////////////////////////////////////////////////////////////
      // assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).setName(null),
      //    "Missing name for a view");
      // assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).setName(""),
      //    "Missing name for a view");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).setType(null),
         "missing its type");

      /////////////////////////////////////////////////////////////////////////
      // meh, enricher sets a default name, so, can't easily catch this one. //
      /////////////////////////////////////////////////////////////////////////
      // assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).setDataSourceName(null),
      //    "missing a dataSourceName");
      // assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).setDataSourceName(""),
      //    "missing a dataSourceName");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).setDataSourceName("notADataSource"),
         "has an unrecognized dataSourceName");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testReportViewColumns()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).setColumns(null),
         "does not have any columns or a view customizer");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).getColumns().get(0).setName(null),
         "has a column with no name");

      assertValidationFailureReasons((qInstance) -> qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).getColumns().get(0).setName(""),
         "has a column with no name");

      assertValidationFailureReasons((qInstance) ->
      {
         List<QReportField> columns = qInstance.getReport(TestUtils.REPORT_NAME_SHAPES_PERSON).getViews().get(0).getColumns();
         columns.get(0).setName("id");
         columns.get(1).setName("id");
      },
         "has multiple columns named: id");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldBehaviors()
   {
      BiFunction<QInstance, String, QFieldMetaData> fieldExtractor = (QInstance qInstance, String fieldName) -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON).getField(fieldName);
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance, "firstName").withBehaviors(Set.of(ValueTooLongBehavior.ERROR, ValueTooLongBehavior.TRUNCATE)).withMaxLength(1)),
         "more than 1 fieldBehavior of type ValueTooLongBehavior, which is not allowed");

      ///////////////////////////////////////////////////////////////////////////
      // make sure a custom validation method in a field behavior gets applied //
      // more tests for this particular behavior are in its own test class     //
      ///////////////////////////////////////////////////////////////////////////
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance, "firstName").withBehavior(new DateTimeDisplayValueBehavior())),
         "DateTimeDisplayValueBehavior was a applied to a non-DATE_TIME field");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldValueTooLongBehavior()
   {
      Function<QInstance, QFieldMetaData> fieldExtractor = qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON).getField("firstName");

      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withBehavior(ValueTooLongBehavior.ERROR)), "specifies a ValueTooLongBehavior, but not a maxLength");
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withBehavior(ValueTooLongBehavior.TRUNCATE)), "specifies a ValueTooLongBehavior, but not a maxLength");
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS)), "specifies a ValueTooLongBehavior, but not a maxLength");
      assertValidationSuccess((qInstance -> fieldExtractor.apply(qInstance).withBehavior(ValueTooLongBehavior.PASS_THROUGH)));

      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withBehavior(ValueTooLongBehavior.ERROR).withMaxLength(0)), "invalid maxLength");
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withBehavior(ValueTooLongBehavior.ERROR).withMaxLength(-1)), "invalid maxLength");
      assertValidationSuccess((qInstance -> fieldExtractor.apply(qInstance).withBehavior(ValueTooLongBehavior.ERROR).withMaxLength(1)));

      Function<QInstance, QFieldMetaData> idFieldExtractor = qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON).getField("id");
      assertValidationFailureReasons((qInstance -> idFieldExtractor.apply(qInstance).withBehavior(ValueTooLongBehavior.ERROR).withMaxLength(1)), "maxLength, but is not of a supported type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldBehaviorsWithTheirOwnValidateMethods()
   {
      Function<QInstance, QFieldMetaData> fieldExtractor = qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON).getField("id");
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withBehavior(new ValueRangeBehavior())),
         "Field id in table person: ValueRangeBehavior: Either minValue or maxValue (or both) must be set.");

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityKeyTypes()
   {
      assertValidationFailureReasons((qInstance -> qInstance.addSecurityKeyType(new QSecurityKeyType())),
         "Missing name for a securityKeyType");

      assertValidationFailureReasons((qInstance -> qInstance.addSecurityKeyType(new QSecurityKeyType().withName(""))),
         "Missing name for a securityKeyType");

      assertThatThrownBy(() ->
      {
         QInstance qInstance = QContext.getQInstance();
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId"));
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId"));
      }).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Attempted to add a second securityKeyType with name: clientId");

      assertValidationFailureReasons((qInstance ->
      {
         QSecurityKeyType securityKeyType1 = new QSecurityKeyType().withName("clientId");
         QSecurityKeyType securityKeyType2 = new QSecurityKeyType().withName("notClientId");
         qInstance.addSecurityKeyType(securityKeyType1);
         qInstance.addSecurityKeyType(securityKeyType2);
         securityKeyType2.setName("clientId");
      }), "Inconsistent naming for securityKeyType");

      assertValidationFailureReasons((qInstance ->
      {
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId").withAllAccessKeyName("clientId"));
      }), "More than one SecurityKeyType with name (or allAccessKeyName or nullValueBehaviorKeyName) of: clientId");

      assertValidationFailureReasonsAllowingExtraReasons((qInstance ->
      {
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId").withAllAccessKeyName("clientId").withNullValueBehaviorKeyName("clientId"));
      }), "More than one SecurityKeyType with name (or allAccessKeyName or nullValueBehaviorKeyName) of: clientId");

      assertValidationFailureReasons((qInstance ->
      {
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId").withAllAccessKeyName("allAccess"));
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("warehouseId").withAllAccessKeyName("allAccess"));
      }), "More than one SecurityKeyType with name (or allAccessKeyName or nullValueBehaviorKeyName) of: allAccess");

      assertValidationFailureReasons((qInstance ->
      {
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId").withNullValueBehaviorKeyName("nullBehavior"));
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("warehouseId").withNullValueBehaviorKeyName("nullBehavior"));
      }), "More than one SecurityKeyType with name (or allAccessKeyName or nullValueBehaviorKeyName) of: nullBehavior");

      assertValidationFailureReasons((qInstance ->
      {
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId").withAllAccessKeyName("allAccess"));
         qInstance.addSecurityKeyType(new QSecurityKeyType().withName("allAccess"));
      }), "More than one SecurityKeyType with name (or allAccessKeyName or nullValueBehaviorKeyName) of: allAccess");

      assertValidationFailureReasons((qInstance -> qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId").withPossibleValueSourceName("nonPVS"))),
         "Unrecognized possibleValueSourceName in securityKeyType");

      assertValidationSuccess((qInstance -> qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId").withPossibleValueSourceName(TestUtils.POSSIBLE_VALUE_SOURCE_STATE))));
      assertValidationSuccess((qInstance -> qInstance.addSecurityKeyType(new QSecurityKeyType().withName("clientId").withAllAccessKeyName("clientAllAccess"))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityLocks()
   {
      Function<QInstance, RecordSecurityLock> lockExtractor = qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getRecordSecurityLocks().get(0);

      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setSecurityKeyType(null)), "missing a securityKeyType");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setSecurityKeyType(" ")), "missing a securityKeyType");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setSecurityKeyType("notAKeyType")), "unrecognized securityKeyType");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setFieldName(null)), "missing a fieldName");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setFieldName("")), "missing a fieldName");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setFieldName("notAField")), "unrecognized field");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setNullValueBehavior(null)), "missing a nullValueBehavior");

      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setFieldName("join.field")), "Table order recordSecurityLock (of key type store) field name join.field looks like a join (has a dot), but no joinNameChain was given");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityLockJoinChains()
   {
      Function<QInstance, RecordSecurityLock> lockExtractor = qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).getRecordSecurityLocks().get(0);

      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setJoinNameChain(null)), "looks like a join (has a dot), but no joinNameChain was given");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setJoinNameChain(new ArrayList<>())), "looks like a join (has a dot), but no joinNameChain was given");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setFieldName("storeId")), "does not look like a join (does not have a dot), but a joinNameChain was given");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setFieldName("order.wrongId")), "unrecognized fieldName: order.wrongId");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setFieldName("lineItem.id")), "joinNameChain doesn't end in the expected table [lineItem]");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setJoinNameChain(List.of("notAJoin"))), "an unrecognized join");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setJoinNameChain(List.of("orderLineItem"))), "joinNameChain could not be followed through join");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldSecurityLocks()
   {
      Function<QInstance, FieldSecurityLock> lockExtractor = qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getField("total").getFieldSecurityLock();

      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setSecurityKeyType(null)), "missing a securityKeyType");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setSecurityKeyType(" ")), "missing a securityKeyType");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setSecurityKeyType("notAKeyType")), "unrecognized securityKeyType");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setDefaultBehavior(null)), "missing a defaultBehavior");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setOverrideValues(null)), "missing overrideValues");
      assertValidationFailureReasons((qInstance -> lockExtractor.apply(qInstance).setOverrideValues(Collections.emptyList())), "missing overrideValues");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAssociations()
   {
      assertValidationFailureReasons((qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_ORDER).withAssociation(new Association())),
         "missing a name for an Association on table " + TestUtils.TABLE_NAME_ORDER);

      assertValidationFailureReasons((qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_ORDER).withAssociation(new Association().withName("myAssociation"))),
         "missing joinName for Association myAssociation on table " + TestUtils.TABLE_NAME_ORDER,
         "missing associatedTableName for Association myAssociation on table " + TestUtils.TABLE_NAME_ORDER);

      assertValidationFailureReasons((qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_ORDER).withAssociation(new Association().withName("myAssociation").withJoinName("notAJoin").withAssociatedTableName(TestUtils.TABLE_NAME_LINE_ITEM))),
         "unrecognized joinName notAJoin for Association myAssociation on table " + TestUtils.TABLE_NAME_ORDER);

      assertValidationFailureReasons((qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_ORDER).withAssociation(new Association().withName("myAssociation").withJoinName("orderLineItem").withAssociatedTableName("notATable"))),
         "unrecognized associatedTableName notATable for Association myAssociation on table " + TestUtils.TABLE_NAME_ORDER);

      //////////////////////////////////
      // wrong join on an association //
      //////////////////////////////////
      assertValidationFailureReasons((qInstance ->
      {
         Association association = qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getAssociationByName("orderLine").orElseThrow();
         association.setJoinName("orderOrderExtrinsic");
      }),
         "join [orderOrderExtrinsic] does not connect tables [order] and [orderLine]");

      //////////////////////////////////////////
      // wrong table (doesn't match the join) //
      //////////////////////////////////////////
      assertValidationFailureReasons((qInstance ->
      {
         Association association = qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getAssociationByName("orderLine").orElseThrow();
         association.setAssociatedTableName(TestUtils.TABLE_NAME_ORDER_EXTRINSIC);
      }),
         "join [orderLineItem] does not connect tables [order] and [orderExtrinsic]");

      //////////////////////////////
      // invalid type on the join //
      //////////////////////////////
      assertValidationFailureReasons((qInstance -> qInstance.getJoin("orderLineItem").setType(JoinType.MANY_TO_MANY)),
         "Join type does not have 'one' on this table's side side (left)");
      assertValidationFailureReasons((qInstance -> qInstance.getJoin("orderLineItem").setType(JoinType.MANY_TO_ONE)),
         "Join type does not have 'one' on this table's side side (left)");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExposedJoinPaths()
   {
      assertValidationFailureReasons(qInstance -> qInstance.addTable(newTable("A", "id").withExposedJoin(new ExposedJoin())),
         "Table A has an exposedJoin that is missing a joinTable name",
         "Table A exposedJoin [missingJoinTableName] is missing a label");

      assertValidationFailureReasons(qInstance -> qInstance.addTable(newTable("A", "id").withExposedJoin(new ExposedJoin().withJoinTable("B"))),
         "Table A exposedJoin B is referencing an unrecognized table",
         "Table A exposedJoin B is missing a label");

      assertValidationFailureReasons(qInstance ->
      {
         qInstance.addTable(newTable("A", "id").withExposedJoin(new ExposedJoin().withJoinTable("B").withLabel("B").withJoinPath(List.of("notAJoin"))));
         qInstance.addTable(newTable("B", "id", "aId"));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("B").withName("AB").withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "aId")));
      },
         "does not match a valid join connection in the instance");

      assertValidationFailureReasons(qInstance ->
      {
         qInstance.addTable(newTable("A", "id")
            .withExposedJoin(new ExposedJoin().withJoinTable("B").withLabel("foo").withJoinPath(List.of("AB")))
            .withExposedJoin(new ExposedJoin().withJoinTable("C").withLabel("foo").withJoinPath(List.of("AC")))
         );
         qInstance.addTable(newTable("B", "id", "aId"));
         qInstance.addTable(newTable("C", "id", "aId"));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("B").withName("AB").withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "aId")));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("C").withName("AC").withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "aId")));
      },
         "more than one join labeled: foo");

      assertValidationFailureReasons(qInstance ->
      {
         qInstance.addTable(newTable("A", "id")
            .withExposedJoin(new ExposedJoin().withJoinTable("B").withLabel("B1").withJoinPath(List.of("AB")))
            .withExposedJoin(new ExposedJoin().withJoinTable("B").withLabel("B2").withJoinPath(List.of("AB")))
         );
         qInstance.addTable(newTable("B", "id", "aId"));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("B").withName("AB").withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "aId")));
      },
         "than one join with the joinPath: [AB]");

      assertValidationSuccess(qInstance ->
      {
         qInstance.addTable(newTable("A", "id").withExposedJoin(new ExposedJoin().withJoinTable("B").withLabel("B").withJoinPath(List.of("AB"))));
         qInstance.addTable(newTable("B", "id", "aId"));
         qInstance.addJoin(new QJoinMetaData().withLeftTable("A").withRightTable("B").withName("AB").withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "aId")));
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testShareableTableMetaData()
   {
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // just make sure we call this class's validator - the rest of its conditions are covered in its own test //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertValidationFailureReasonsAllowingExtraReasons(qInstance -> qInstance.addTable(newTable("A", "id").withShareableTableMetaData(new ShareableTableMetaData())),
         "missing sharedRecordTableName");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldAdornments()
   {
      Function<QInstance, QFieldMetaData> fieldExtractor = qInstance -> qInstance.getTable(TestUtils.TABLE_NAME_PERSON).getField("firstName");

      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment())), "adornment that is missing a type");
      assertValidationSuccess((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment().withType(AdornmentType.REVEAL))));

      ////////////////////////////////
      // type-specific value checks //
      ////////////////////////////////
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment(AdornmentType.SIZE))), "missing a width value");
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment(AdornmentType.SIZE).withValue("width", "foo"))), "unrecognized width value");
      assertValidationSuccess((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment(AdornmentType.SIZE).withValue("width", AdornmentType.Size.MEDIUM))));
      assertValidationSuccess((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(AdornmentType.Size.SMALL.toAdornment())));

      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD).withValue("fileNameField", "foo"))), "unrecognized fileNameField [foo]");
      assertValidationSuccess((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD).withValue("fileNameField", "lastName"))));

      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD).withValue("fileNameFormatFields", "foo"))), "fileNameFormatFields could not be accessed");
      assertValidationFailureReasons((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD).withValue("fileNameFormatFields", new ArrayList<>(List.of("foo"))))), "unrecognized field name in fileNameFormatFields [foo]");
      assertValidationSuccess((qInstance -> fieldExtractor.apply(qInstance).withFieldAdornment(new FieldAdornment(AdornmentType.FILE_DOWNLOAD).withValue("fileNameFormatFields", new ArrayList<>(List.of("lastName"))))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAuthenticationCustomizer()
   {
      assertValidationSuccess((qInstance -> qInstance.getAuthentication().withCustomizer(null)));
      assertValidationSuccess((qInstance -> qInstance.getAuthentication().withCustomizer(new QCodeReference(ValidAuthCustomizer.class))));
      assertValidationFailureReasons((qInstance -> qInstance.getAuthentication().withCustomizer(new QCodeReference(ArrayList.class))), "not of the expected type");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWidgetNaming()
   {
      String name = PersonsByCreateDateBarChart.class.getSimpleName();

      assertValidationFailureReasons((qInstance) -> qInstance.getWidget(name).withName(null),
         "Inconsistent naming for widget");

      assertValidationFailureReasons((qInstance) -> qInstance.getWidget(name).withName(""),
         "Inconsistent naming for widget");

      assertValidationFailureReasons((qInstance) -> qInstance.getWidget(name).withName("wrongName"),
         "Inconsistent naming for widget");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWidgetCodeReference()
   {
      String name = PersonsByCreateDateBarChart.class.getSimpleName();

      assertValidationFailureReasons((qInstance) -> qInstance.getWidget(name).withCodeReference(null),
         "Missing codeReference for widget");

      assertValidationFailureReasons((qInstance) -> qInstance.getWidget(name).withCodeReference(new QCodeReference(ArrayList.class)),
         "CodeReference is not of the expected type: class " + AbstractWidgetRenderer.class.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testParentWidgets()
   {
      assertValidationFailureReasons((qInstance) -> qInstance.addWidget(new ParentWidgetMetaData()
            .withName("parentWidget")
            .withCodeReference(new QCodeReference(ParentWidgetRenderer.class))
         ),
         "Missing child widgets");

      assertValidationFailureReasons((qInstance) -> qInstance.addWidget(new ParentWidgetMetaData()
            .withChildWidgetNameList(List.of("noSuchWidget"))
            .withName("parentWidget")
            .withCodeReference(new QCodeReference(ParentWidgetRenderer.class))
         ),
         "Unrecognized child widget name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QTableMetaData newTable(String tableName, String... fieldNames)
   {
      QTableMetaData tableMetaData = new QTableMetaData()
         .withName(tableName)
         .withBackendName(TestUtils.DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField(fieldNames[0]);

      for(String fieldName : fieldNames)
      {
         tableMetaData.addField(new QFieldMetaData(fieldName, QFieldType.INTEGER));
      }

      return (tableMetaData);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private TableAutomationAction getAction0(QInstance qInstance)
   {
      return qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getAutomationDetails().getActions().get(0);
   }



   /*******************************************************************************
    ** Run a little setup code on a qInstance; then validate it, and assert that it
    ** failed validation with reasons that match the supplied vararg-reasons (but allow
    ** more reasons - e.g., helpful when one thing we're testing causes other errors).
    *******************************************************************************/
   public static void assertValidationFailureReasonsAllowingExtraReasons(Consumer<QInstance> setup, String... expectedReasons)
   {
      assertValidationFailureReasons(setup, true, expectedReasons);
   }



   /*******************************************************************************
    ** Run a little setup code on a qInstance; then validate it, and assert that it
    ** failed validation with reasons that match the supplied vararg-reasons (and
    ** require that exact # of reasons).
    *******************************************************************************/
   public static void assertValidationFailureReasons(Consumer<QInstance> setup, String... expectedReasons)
   {
      assertValidationFailureReasons(setup, false, expectedReasons);
   }



   /*******************************************************************************
    ** Implementation for the overloads of this name.
    *******************************************************************************/
   public static void assertValidationFailureReasons(Consumer<QInstance> setup, boolean allowExtraReasons, String... expectedReasons)
   {
      try
      {
         QInstance qInstance = TestUtils.defineInstance();
         setup.accept(qInstance);
         new QInstanceValidator().validate(qInstance);
         fail("Should have thrown validationException");
      }
      catch(QInstanceValidationException e)
      {
         assertValidationFailureReasons(allowExtraReasons, e.getReasons(), expectedReasons);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void assertValidationFailureReasons(boolean allowExtraReasons, List<String> actualReasons, String... expectedReasons)
   {
      if(!allowExtraReasons)
      {
         int noOfReasons = actualReasons == null ? 0 : actualReasons.size();
         assertEquals(expectedReasons.length, noOfReasons, "Expected number of validation failure reasons.\nExpected reasons: " + String.join(",", expectedReasons)
            + "\nActual reasons: " + (noOfReasons > 0 ? String.join("\n", actualReasons) : "--"));
      }

      for(String reason : expectedReasons)
      {
         assertReason(reason, actualReasons);
      }
   }



   /*******************************************************************************
    ** Assert that an instance is valid!
    *******************************************************************************/
   public static void assertValidationSuccess(Consumer<QInstance> setup)
   {
      try
      {
         QInstance qInstance = TestUtils.defineInstance();
         setup.accept(qInstance);
         new QInstanceValidator().validate(qInstance);
      }
      catch(QInstanceValidationException e)
      {
         fail("Expected no validation errors, but received: " + e.getMessage());
      }
   }



   /*******************************************************************************
    ** utility method for asserting that a specific reason string is found within
    ** the list of reasons in the QInstanceValidationException.
    **
    *******************************************************************************/
   public static void assertReason(String reason, List<String> actualReasons)
   {
      assertNotNull(actualReasons, "Expected there to be a reason for the failure (but there was not)");
      assertThat(actualReasons)
         .withFailMessage("Expected any of:\n%s\nTo match: [%s]", actualReasons, reason)
         .anyMatch(s -> s.contains(reason));
   }



   /***************************************************************************
    ** test classes for validating process steps
    ***************************************************************************/
   public abstract class TestAbstractClass extends AbstractTransformStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private class TestPrivateClass extends AbstractTransformStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
      {
         return null;
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public class TestNoArgsConstructorClass extends AbstractTransformStep
   {
      /***************************************************************************
       **
       ***************************************************************************/
      public TestNoArgsConstructorClass(int i)
      {

      }



      /***************************************************************************
       **
       ***************************************************************************/
      public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
      {
         return null;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ValidAuthCustomizer implements QAuthenticationModuleCustomizerInterface {}



   /***************************************************************************
    **
    ***************************************************************************/
   public static class TestReportStaticDataSupplier implements Supplier<List<List<Serializable>>>
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<List<Serializable>> get()
      {
         return List.of();
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class TestReportCustomRecordSource implements ReportCustomRecordSourceInterface
   {

      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void execute(ReportInput reportInput, QReportDataSource reportDataSource, RecordPipe recordPipe) throws QException
      {

      }
   }
}

