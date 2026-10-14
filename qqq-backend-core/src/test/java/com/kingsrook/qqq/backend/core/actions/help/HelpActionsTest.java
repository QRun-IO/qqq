/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.help;


import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.MetaDataAction;
import com.kingsrook.qqq.backend.core.actions.metadata.ProcessMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceHelpContentManager;
import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContent;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpRole;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryDeleteAction;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryInsertAction;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryUpdateAction;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Help integration tests with real metadata, permissions and the memory backend.
 *******************************************************************************/
class HelpActionsTest extends BaseTest
{
   /*******************************************************************************
    ** Restore the production poll interval after each test.
    *******************************************************************************/
   @AfterEach
   void restoreThrottle()
   {
      QInstanceHelpContentManager.setReloadIntervalMillis(30_000);
   }



   /*******************************************************************************
    ** New keys accept summary-only rows, including suffix process wildcards.
    *******************************************************************************/
   @Test
   void newTargetsAndSummaryOnlyDeletion() throws Exception
   {
      var instance = QContext.getQInstance();
      new HelpContentMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      instance.addProcess(new QProcessMetaData().withName("one.explain").withHelpContent("help", new QHelpContent("First fallback").withRole(QHelpRole.ALL_SCREENS)));
      instance.addProcess(new QProcessMetaData().withName("two.explain").withHelpContent("help", new QHelpContent("Second fallback").withRole(QHelpRole.ALL_SCREENS)));
      instance.addReport(new QReportMetaData().withName("helpReport"));
      instance.addPossibleValueSource(new QPossibleValueSource().withName("helpSource"));
      List<String> keys = List.of("process:*.explain;slot:help", "report:helpReport;slot:help", "possibleValueSource:helpSource;slot:help");
      for(int i = 0; i < keys.size(); i++)
      {
         new InsertAction().execute(new InsertInput(HelpContent.TABLE_NAME).withRecordEntity(new HelpContent().withId(i + 1).withKey(keys.get(i)).withRole("ALL_SCREENS").withSummary("Summary " + i)));
      }
      assertEquals("Summary 0", instance.getProcess("one.explain").getHelpContent().get("help").getFirst().getSummary());
      assertEquals("Summary 0", instance.getProcess("two.explain").getHelpContent().get("help").getFirst().getSummary());
      assertEquals("Summary 1", instance.getReport("helpReport").getHelpContent().get("help").getFirst().getSummary());
      assertEquals("Summary 2", instance.getPossibleValueSource("helpSource").getHelpContent().get("help").getFirst().getSummary());
      new DeleteAction().execute(new DeleteInput(HelpContent.TABLE_NAME).withPrimaryKeys(List.of(1, 2, 3)));
      assertEquals("First fallback", instance.getProcess("one.explain").getHelpContent().get("help").getFirst().getContent());
      assertEquals("Second fallback", instance.getProcess("two.explain").getHelpContent().get("help").getFirst().getContent());
      assertThat(instance.getReport("helpReport").getHelpContent().get("help")).isEmpty();
      assertThat(instance.getPossibleValueSource("helpSource").getHelpContent().get("help")).isEmpty();
   }



   /*******************************************************************************
    ** Simulate another node writing directly to the backend, with no customizers.
    *******************************************************************************/
   @Test
   void pollingSeesInsertUpdateDeleteAndRestoresCode() throws Exception
   {
      var instance = QContext.getQInstance();
      new HelpContentMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      var field = instance.getTable("person").getField("email");
      field.withHelpContent(new QHelpContent("Code fallback").withRole(QHelpRole.ALL_SCREENS));
      field.withHelpContent(new QHelpContent("Other role").withRole(QHelpRole.VIEW_SCREEN));
      QInstanceHelpContentManager.setReloadIntervalMillis(0);
      QInstanceHelpContentManager.reloadIfChanged(instance);
      var session = QContext.getQSession();
      QRecord record = new QRecord().withValue("id", 99).withValue("key", "table:person;field:email").withValue("role", "ALL_SCREENS").withValue("summary", "Remote insert").withValue("modifyDate", Instant.parse("2026-01-01T00:00:00Z"));
      new MemoryInsertAction().execute(new InsertInput(HelpContent.TABLE_NAME).withRecord(record));
      assertEquals("Code fallback", field.getHelpContents().getFirst().getContent());
      QInstanceHelpContentManager.reloadIfChanged(instance);
      assertEquals("Remote insert", field.getHelpContents().getFirst().getSummary());
      assertEquals(session, QContext.getQSession());
      record.withValue("summary", "Remote update").withValue("modifyDate", Instant.parse("2026-01-02T00:00:00Z"));
      new MemoryUpdateAction().execute(new UpdateInput(HelpContent.TABLE_NAME).withRecord(record));
      QInstanceHelpContentManager.reloadIfChanged(instance);
      assertEquals("Remote update", field.getHelpContents().getFirst().getSummary());
      QInstanceHelpContentManager.reloadHelpContent(instance);
      assertThat(field.getHelpContents()).hasSize(2);
      new MemoryDeleteAction().execute(new DeleteInput(HelpContent.TABLE_NAME).withPrimaryKeys(List.of(99)));
      QInstanceHelpContentManager.reloadIfChanged(instance);
      assertThat(field.getHelpContents()).extracting(QHelpContent::getContent).containsExactly("Code fallback", "Other role");
      QInstanceHelpContentManager.reloadHelpContent(instance);
      assertThat(field.getHelpContents()).hasSize(2);
   }



   /*******************************************************************************
    ** Targeting filters nested frontend copies without modifying shared metadata.
    *******************************************************************************/
   @Test
   void frontendVisibility() throws Exception
   {
      var instance = QContext.getQInstance();
      QHelpContent help = new QHelpContent("Restricted help").withSummary("Restricted summary").withPermissionName("help.special").withClientChannel("web");
      var table = instance.getTable("person");
      table.withHelpContent("help", help);
      table.getField("email").withHelpContent(help);
      table.getSections().getFirst().withHelpContent(help);
      instance.addApp(new QAppMetaData().withName("helpApp").withLabel("Help app").withHelpContent("help", help));
      QFrontendStepMetaData step = new QFrontendStepMetaData().withName("helpStep").withHelpContent(help)
         .withFormField(new QFieldMetaData("question", QFieldType.STRING).withHelpContent(help));
      instance.addProcess(new QProcessMetaData().withName("helpProcess").withLabel("Help process").withHelpContent("help", help).withStep(step));
      TableMetaDataInput tableInput = new TableMetaDataInput();
      tableInput.setTableName("person");
      ProcessMetaDataInput processInput = new ProcessMetaDataInput();
      processInput.setProcessName("helpProcess");
      for(boolean allowed : List.of(false, true, false))
      {
         QContext.getQSession().setPermissions(allowed ? Set.of("help.special") : Set.of());
         QContext.getQSession().setClientChannel("web/browser");
         var metadata = new MetaDataAction().execute(new MetaDataInput());
         int expected = allowed ? 1 : 0;
         assertThat(metadata.getApps().get("helpApp").getHelpContents().get("help")).hasSize(expected);
         var frontend = new TableMetaDataAction().execute(tableInput).getTable();
         assertThat(frontend.getHelpContents().get("help")).hasSize(expected);
         assertThat(frontend.getFields().get("email").getHelpContents()).hasSize(expected);
         assertThat(frontend.getSections().getFirst().getHelpContents()).hasSize(expected);
         var process = new ProcessMetaDataAction().execute(processInput).getProcess();
         assertThat(process.getHelpContents().get("help")).hasSize(expected);
         assertThat(process.getFrontendSteps().getFirst().getHelpContents()).hasSize(expected);
         assertThat(process.getFrontendSteps().getFirst().getFormFields().getFirst().getHelpContents()).hasSize(expected);
         assertThat(JsonUtils.toJson(process)).doesNotContain("permissionName", "clientChannel", "fromData", "codeContent");
      }
      QContext.getQSession().setPermissions(Set.of("help.special"));
      QContext.getQSession().setClientChannel("cli");
      assertThat(new TableMetaDataAction().execute(tableInput).getTable().getHelpContents().get("help")).isEmpty();
      assertThat(table.getHelpContent().get("help")).hasSize(1);
      assertThat(step.getHelpContents()).hasSize(1);
      assertEquals("help.special", help.getPermissionName());
   }



   /*******************************************************************************
    ** Weighted ranking, canonical keys and both object and content permissions.
    *******************************************************************************/
   @Test
   void searchRankingAndPermissions() throws Exception
   {
      var instance = QContext.getQInstance();
      instance.getTable("person").withLabel("Nebula people").withHelpContent("help", new QHelpContent("Documentation"));
      instance.getTable("person").getField("email").withHelpContent(new QHelpContent("Contact address").withSummary("Nebula delivery"));
      instance.getTable("shape").withHelpContent("help", new QHelpContent("Nebula in content"));
      var results = new HelpSearchAction().execute(new HelpSearchInput().withQuery("nebula"));
      assertThat(results).extracting(HelpSearchResult::getKey).containsExactly("table:person;slot:help", "table:person;field:email", "table:shape;slot:help");
      instance.getTable("person").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      results = new HelpSearchAction().execute(new HelpSearchInput().withQuery("nebula"));
      assertThat(results).extracting(HelpSearchResult::getKey).containsExactly("table:shape;slot:help");
      instance.getTable("shape").getHelpContent().get("help").getFirst().withPermissionName("help.secret");
      assertThat(new HelpSearchAction().execute(new HelpSearchInput().withQuery("nebula"))).isEmpty();
   }



   /*******************************************************************************
    ** Coverage is author-only, includes hidden fields and keeps useful totals.
    *******************************************************************************/
   @Test
   void coverageTotalsFiltersAndPermission() throws Exception
   {
      var instance = QContext.getQInstance();
      new HelpContentMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      instance.getTable("person").getField("email").withHelpContent(new QHelpContent("Address help"));
      instance.getTable("person").getField("id").withIsHidden(true);
      instance.getTable("person").withHelpContent("customSlot", new QHelpContent("Other help"));
      var tables = new HelpCoverageAction().execute(new HelpCoverageInput().withKind("table"));
      assertEquals(instance.getTables().size(), tables.getTotals().getFirst().getTotal());
      var all = new HelpCoverageAction().execute(new HelpCoverageInput().withKind("field").withParentKey("table:person"));
      assertEquals(instance.getTables().values().stream().filter(table -> table.getName().startsWith("person")).mapToInt(table -> table.getFields().size()).sum(), all.getTotals().getFirst().getTotal());
      assertEquals(1, all.getTotals().getFirst().getWithHelp());
      assertThat(all.getEntries()).anyMatch(entry -> entry.getKey().equals("table:person;field:id") && entry.getHidden());
      var missing = new HelpCoverageAction().execute(new HelpCoverageInput().withKind("field").withParentKey("table:person").withMissingOnly(true));
      assertThat(missing.getEntries()).noneMatch(entry -> entry.getKey().equals("table:person;field:email"));
      assertEquals(all.getTotals().getFirst().getTotal(), missing.getTotals().getFirst().getTotal());
      instance.getTable(HelpContent.TABLE_NAME).withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      assertThrows(QPermissionDeniedException.class, () -> new HelpCoverageAction().execute(new HelpCoverageInput()));
      QContext.getQSession().withPermission("helpContent.edit");
      assertThat(new HelpCoverageAction().execute(new HelpCoverageInput()).getEntries()).isNotEmpty();
   }



   /*******************************************************************************
    ** Physical-schema compatibility omits all new fields; audit stays optional.
    *******************************************************************************/
   @Test
   void legacySchemaOption() throws Exception
   {
      var instance = QContext.getQInstance();
      new HelpContentMetaDataProvider().withTargetingFields(false).defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      assertThat(instance.getTable(HelpContent.TABLE_NAME).getFields()).doesNotContainKeys("summary", "permissionName", "clientChannel");
      assertThat(instance.getTable(HelpContent.TABLE_NAME).getSections()).extracting(section -> section.getName()).containsExactly("identity", "content", "dates");
      assertThat(instance.getTable(HelpContent.TABLE_NAME).getAuditRules()).isNull();
   }


   /*******************************************************************************
    ** Summary length is enforced and audit activates only with the audit tables.
    *******************************************************************************/
   @Test
   void summaryValidationAndAudit() throws Exception
   {
      var instance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      new HelpContentMetaDataProvider().defineAll(instance, TestUtils.MEMORY_BACKEND_NAME, null);
      assertEquals(AuditLevel.FIELD, instance.getTable("helpContent").getAuditRules().getAuditLevel());
      var result = new InsertAction().execute(new InsertInput("helpContent").withRecordEntity(new HelpContent()
         .withKey("table:person;slot:help").withRole("ALL_SCREENS").withSummary("x".repeat(501))));
      assertThat(result.getRecords().getFirst().getErrorsAsString()).contains("too long");
   }
}
