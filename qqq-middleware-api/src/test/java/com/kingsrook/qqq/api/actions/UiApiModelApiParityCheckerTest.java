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

package com.kingsrook.qqq.api.actions;


import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.ApiOperation;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.ClientChannelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Fixed exceptions document the fixture's deliberate API boundaries. Mutations
 ** must never generate their own exclusions, or newly introduced drift would pass.
 *******************************************************************************/
class UiApiModelApiParityCheckerTest
{
   /*******************************************************************************
    ** Release test context, including on assertion failures.
    *******************************************************************************/
   @AfterEach
   void clearContext()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Every published API/version must satisfy the committed parity contract.
    *******************************************************************************/
   @Test
   void currentInstancePassesEverySupportedVersion() throws Exception
   {
      QInstance instance = instance();
      for(var api : ApiInstanceMetaDataContainer.of(instance).getApis().values())
      {
         for(var version : api.getSupportedVersions())
         {
            assertThat(new UiApiModelApiParityChecker().check(instance, api.getName(), version.toString(), exclusions(api.getName(), version.toString())))
               .as(api.getName() + " " + version).isEmpty();
         }
      }
   }



   /*******************************************************************************
    ** isExcluded is not an explanation: the test contract must supply a reason.
    *******************************************************************************/
   @Test
   void detectsRemovedFieldExposure() throws Exception
   {
      QInstance instance = instance();
      ApiFieldMetaDataContainer.of(instance.getTable("order").getField("storeId")).getApiFieldMetaData(TestUtils.API_NAME).setIsExcluded(true);
      assertThat(check(instance)).containsExactly("table [order] field [storeId]: not in API test-api 2023.Q1");
      assertThat(new UiApiModelApiParityChecker().check(instance, TestUtils.API_NAME, TestUtils.V2023_Q1,
         exclusions(TestUtils.API_NAME, TestUtils.V2023_Q1).withField("order", "storeId", "Store routing is internal to the application"))).isEmpty();
   }



   /*******************************************************************************
    ** UI virtual fields need explicit publication; hidden fields are not UI-visible.
    *******************************************************************************/
   @Test
   void checksVirtualFieldsAndIgnoresHiddenFields() throws Exception
   {
      QInstance instance = instance();
      QTableMetaData table = instance.getTable("order");
      QVirtualFieldMetaData virtualField = new QVirtualFieldMetaData("displayTotal", QFieldType.STRING);
      table.withVirtualField(virtualField);
      table.withField(new QFieldMetaData("internalNote", QFieldType.STRING).withIsHidden(true));
      new QInstanceValidator().revalidate(instance);
      assertThat(check(instance)).containsExactly("table [order] field [displayTotal]: not in API test-api 2023.Q1");
      virtualField.withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME,
         new ApiFieldMetaData().withInitialVersion(TestUtils.V2023_Q1).withApiFieldName("formattedTotal")));
      assertThat(check(instance)).isEmpty();
   }



   /*******************************************************************************
    ** Two fields sharing a wire name cannot both satisfy their UI contract.
    *******************************************************************************/
   @Test
   void detectsDuplicateFieldAliases() throws Exception
   {
      QInstance instance = instance();
      ApiFieldMetaDataContainer.of(instance.getTable("order").getField("storeId")).getApiFieldMetaData(TestUtils.API_NAME).setApiFieldName("orderNo");
      assertThat(check(instance)).contains("table [order] API field [orderNo]: duplicate API name");
   }



   /*******************************************************************************
    ** New UI tables must fail even when nobody added an API supplement at all.
    *******************************************************************************/
   @Test
   void detectsNewTableWithoutApiMetadata() throws Exception
   {
      QInstance instance = instance();
      instance.addTable(new QTableMetaData().withName("newTable").withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      new QInstanceValidator().revalidate(instance);
      assertThat(check(instance)).containsExactly("table [newTable]: not in API test-api 2023.Q1");
   }



   /*******************************************************************************
    ** Client denial remains effective even for an all-permissions system session.
    *******************************************************************************/
   @Test
   void detectsPermissionRuleMismatch() throws Exception
   {
      QInstance instance = instance();
      instance.getTable("order").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)
         .withDeniedClients(Set.of(ClientChannelsMetaData.CHANNEL_API)));
      assertThat(check(instance)).containsExactly("table [order]: permission rules differ between next-ui and API test-api 2023.Q1");
   }



   /*******************************************************************************
    ** Protected rules and aliases must retain core permission bases, at every level.
    *******************************************************************************/
   @Test
   void sharedPermissionRulesAndApiAliasesPass() throws Exception
   {
      QInstance instance = instance();
      ApiTableMetaDataContainer.of(instance.getTable("order")).getApiTableMetaData(TestUtils.API_NAME).setApiTableName("purchase");
      for(PermissionLevel level : PermissionLevel.values())
      {
         instance.getTable("order").withPermissionRules(new QPermissionRules().withLevel(level).withPermissionBaseName("sales")
            .withDeniedClients(Set.of(ClientChannelsMetaData.CHANNEL_ASSISTANT)));
         assertThat(check(instance)).isEmpty();
      }
   }



   /*******************************************************************************
    ** Query also supplies count; neither capability may silently disappear.
    *******************************************************************************/
   @Test
   void detectsDisabledCapabilitiesAndAllowsSpecificException() throws Exception
   {
      QInstance instance = instance();
      var apiTable = ApiTableMetaDataContainer.of(instance.getTable("order")).getApiTableMetaData(TestUtils.API_NAME);
      apiTable.withDisabledOperation(ApiOperation.QUERY_BY_QUERY_STRING);
      assertThat(check(instance)).containsExactly(
         "table [order] capability [TABLE_COUNT]: not in API test-api 2023.Q1",
         "table [order] capability [TABLE_QUERY]: not in API test-api 2023.Q1");
      apiTable.setDisabledOperations(Set.of(ApiOperation.DELETE));
      assertThat(check(instance)).containsExactly("table [order] capability [TABLE_DELETE]: not in API test-api 2023.Q1");
      assertThat(new UiApiModelApiParityChecker().check(instance, TestUtils.API_NAME, TestUtils.V2023_Q1,
         exclusions(TestUtils.API_NAME, TestUtils.V2023_Q1).withCapability("order", Capability.TABLE_DELETE, "Orders are retained by the external contract"))).isEmpty();
   }



   /*******************************************************************************
    ** Instance defaults apply first; table overrides apply last, as in execution.
    *******************************************************************************/
   @Test
   void honorsInstanceOperationDefaultsAndTableOverrides() throws Exception
   {
      QInstance instance = instance();
      ApiInstanceMetaDataContainer.of(instance).getApiInstanceMetaData(TestUtils.API_NAME).withDisabledOperation(ApiOperation.GET);
      assertThat(check(instance)).contains("table [order] capability [TABLE_GET]: not in API test-api 2023.Q1");
      ApiTableMetaDataContainer.of(instance.getTable("order")).getApiTableMetaData(TestUtils.API_NAME).withEnabledOperation(ApiOperation.GET);
      assertThat(check(instance)).doesNotContain("table [order] capability [TABLE_GET]: not in API test-api 2023.Q1");
   }



   /*******************************************************************************
    ** A table's explicit exclusion flag and version range both constrain publication.
    *******************************************************************************/
   @Test
   void detectsExcludedAndOutOfVersionTables() throws Exception
   {
      QInstance instance = instance();
      var apiTable = ApiTableMetaDataContainer.of(instance.getTable("order")).getApiTableMetaData(TestUtils.API_NAME);
      apiTable.setIsExcluded(true);
      assertThat(check(instance)).containsExactly("table [order]: not in API test-api 2023.Q1");
      apiTable.setIsExcluded(false);
      apiTable.setFinalVersion(TestUtils.V2022_Q4);
      assertThat(check(instance)).containsExactly("table [order]: not in API test-api 2023.Q1");
   }



   /*******************************************************************************
    ** A missing API parent does not make still-visible UI field exclusions stale.
    *******************************************************************************/
   @Test
   void missingTableDoesNotMakeItsFieldExclusionsStale() throws Exception
   {
      QInstance instance = instance();
      ApiTableMetaDataContainer.of(instance.getTable("person")).getApis().remove(TestUtils.API_NAME);
      assertThat(check(instance)).containsExactly("table [person]: not in API test-api 2023.Q1");
   }



   /*******************************************************************************
    ** Removing a process supplement must not remove it from the check's scope.
    *******************************************************************************/
   @Test
   void detectsMissingProcessAndProcessPermissionMismatch() throws Exception
   {
      QInstance instance = instance();
      var process = instance.getProcess("getPersonInfo");
      process.withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED)
         .withDeniedClients(Set.of(ClientChannelsMetaData.CHANNEL_API)));
      assertThat(check(instance)).containsExactly("process [getPersonInfo]: permission rules differ between next-ui and API test-api 2023.Q1");
      ApiProcessMetaDataContainer.of(process).getApis().remove(TestUtils.API_NAME);
      assertThat(check(instance)).containsExactly("process [getPersonInfo]: not in API test-api 2023.Q1");
   }



   /*******************************************************************************
    ** An excluded or historical process needs a reason, just like a table.
    *******************************************************************************/
   @Test
   void detectsExcludedAndOutOfVersionProcesses() throws Exception
   {
      QInstance instance = instance();
      var apiProcess = ApiProcessMetaDataContainer.of(instance.getProcess("getPersonInfo")).getApiProcessMetaData(TestUtils.API_NAME);
      apiProcess.setIsExcluded(true);
      assertThat(check(instance)).containsExactly("process [getPersonInfo]: not in API test-api 2023.Q1");
      apiProcess.setIsExcluded(false);
      apiProcess.setFinalVersion(TestUtils.V2022_Q4);
      assertThat(check(instance)).containsExactly("process [getPersonInfo]: not in API test-api 2023.Q1");
   }



   /*******************************************************************************
    ** Stale entries are sorted drift, and checking never consumes caller exclusions.
    *******************************************************************************/
   @Test
   void detectsStaleExclusions() throws Exception
   {
      QInstance instance = instance();
      ParityExclusions exclusions = exclusions(TestUtils.API_NAME, TestUtils.V2023_Q1)
         .withTable("deletedTable", "Retired table")
         .withProcess("deletedProcess", "Retired process")
         .withField("order", "deletedField", "Retired field");
      for(int i = 0; i < 2; i++)
      {
         assertThat(new UiApiModelApiParityChecker().check(instance, TestUtils.API_NAME, TestUtils.V2023_Q1, exclusions)).containsExactly(
            "process [deletedProcess]: stale exclusion (Retired process)",
            "table [deletedTable]: stale exclusion (Retired table)",
            "table [order] field [deletedField]: stale exclusion (Retired field)");
      }
   }



   /*******************************************************************************
    ** Invalid exclusion reasons fail immediately, rather than silently waiving drift.
    *******************************************************************************/
   @Test
   void requiresReasons()
   {
      assertThatThrownBy(() -> new ParityExclusions().withTable("order", " ")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new ParityExclusions().withProcess("process", null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new ParityExclusions().withField("order", "id", "")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new ParityExclusions().withCapability("order", Capability.TABLE_GET, "\t")).isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    ** A misspelled API or version must not produce a misleading clean report.
    *******************************************************************************/
   @Test
   void rejectsUnknownApiAndUnsupportedVersion() throws Exception
   {
      QInstance instance = instance();
      assertThatThrownBy(() -> new UiApiModelApiParityChecker().check(instance, "missing", TestUtils.V2023_Q1, new ParityExclusions()))
         .isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> new UiApiModelApiParityChecker().check(instance, TestUtils.API_NAME, TestUtils.V2023_Q2, new ParityExclusions()))
         .isInstanceOf(IllegalArgumentException.class);
   }



   /*******************************************************************************
    ** Compare the actual channel-personalized field types, not just canonical fields.
    *******************************************************************************/
   @Test
   void detectsIncompatibleTypeAndRestoresContext() throws Exception
   {
      QInstance instance = instance();
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(ApiTypePersonalizer.class));
      QInstance callerInstance = instance();
      QSession callerSession = new QSession();
      QContext.init(callerInstance, callerSession);
      assertThat(check(instance)).containsExactly("table [order] field [storeId]: incompatible type (UI INTEGER, API STRING)");
      assertThat(QContext.getQInstance()).isSameAs(callerInstance);
      assertThat(QContext.getQSession()).isSameAs(callerSession);
   }



   /*******************************************************************************
    ** Test-only source of a genuine UI/API schema disagreement.
    *******************************************************************************/
   public static class ApiTypePersonalizer implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       ** Only the API channel receives the incompatible field metadata.
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if("order".equals(input.getTableName()) && ClientChannelsMetaData.CHANNEL_API.equals(QContext.getQSession().getClientChannelName()))
         {
            QTableMetaData table = input.getTable().clone();
            table.getField("storeId").setType(QFieldType.STRING);
            return (table);
         }
         return (input.getTable());
      }
   }



   /*******************************************************************************
    ** Validate/enrich each fresh fixture just as an application would at startup.
    *******************************************************************************/
   private QInstance instance() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      new QInstanceValidator().validate(instance);
      return (instance);
   }



   /*******************************************************************************
    ** Standard target used by mutation cases.
    *******************************************************************************/
   private List<String> check(QInstance instance) throws Exception
   {
      return (new UiApiModelApiParityChecker().check(instance, TestUtils.API_NAME, TestUtils.V2023_Q1,
         exclusions(TestUtils.API_NAME, TestUtils.V2023_Q1)));
   }



   /*******************************************************************************
    ** Committed contract exceptions; never infer this list from live API metadata.
    *******************************************************************************/
   private ParityExclusions exclusions(String apiName, String version)
   {
      ParityExclusions exclusions = new ParityExclusions()
         .withTable("memoryVariantData", "Backend variant test fixture, not part of either published API")
         .withTable("memoryVariantOptions", "Backend variant configuration is internal")
         .withTable("savedReport", "Report definition administration is UI-only in this fixture")
         .withTable("sharedSavedReport", "Report sharing administration is UI-only in this fixture")
         .withTable("scheduledReport", "Report scheduling administration is UI-only in this fixture")
         .withTable("renderedReport", "Report history is UI-only; the API publishes the rendering process")
         .withTable("reportStorage", "Report storage is internal; the API returns generated report content")
         .withProcess("runScheduledReport", "Scheduler entry point has no public API contract")
         .withProcess("scheduledReportSyncToScheduledJob", "Internal synchronization of report schedules and scheduler jobs");
      if(TestUtils.ALTERNATIVE_API_NAME.equals(apiName))
      {
         for(String table : List.of("order", "orderLine", "orderLineExtrinsic", "orderExtrinsic"))
         {
            exclusions.withTable(table, "person-api deliberately publishes only the person table");
         }
         for(String process : List.of("getPersonInfo", "transformPeople", "renderSavedReport"))
         {
            exclusions.withProcess(process, "person-api is a table-only contract");
         }
      }
      else
      {
         exclusions.withField("person", "cost", "Cost is introduced in the future 2023.Q2 API")
            .withField("person", "price", "Price is deliberately excluded from test-api");
         if(TestUtils.V2022_Q4.equals(version))
         {
            exclusions.withField("person", "noOfShoes", "2022.Q4 retains the historical shoeCount field")
               .withField("orderLine", "lineNumber", "Line numbers were introduced in 2023.Q1")
               .withProcess("getPersonInfo", "Process was introduced in 2023.Q1")
               .withProcess("transformPeople", "Process was introduced in 2023.Q1");
         }
      }
      return (exclusions);
   }
}
