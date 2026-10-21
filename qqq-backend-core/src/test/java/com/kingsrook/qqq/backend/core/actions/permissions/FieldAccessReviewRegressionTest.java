/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiExposure;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEventType;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.actions.reporting.customizers.DataSourceQueryInputCustomizer;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QSubQuery;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.CurrentUser;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValuePicker;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.processes.implementations.audits.GetAuditsForRecordProcess;
import com.kingsrook.qqq.backend.core.processes.implementations.columnstats.ColumnStatsStep;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Regression coverage for the secured-data review's shared boundaries.
 *******************************************************************************/
class FieldAccessReviewRegressionTest extends FieldAccessTestSupport
{
   /*******************************************************************************
    ** Grouping audit values through a process must reach the same criteria gate.
    *******************************************************************************/
   @Test
   void columnStatsCannotGroupAuditPayload() throws Exception
   {
      new AuditsMetaDataProvider().defineAll(QContext.getQInstance(), table.getBackendName(), null);
      QContext.getQInstance().addProcess(ColumnStatsStep.getProcessMetaData());
      RunProcessInput input = new RunProcessInput().withProcessName("columnStats").withInputSource(QInputSource.USER);
      input.addValue("tableName", "auditDetail");
      input.addValue("fieldName", "oldValue");
      Exception exception = assertThrows(QException.class, () -> new RunProcessAction().execute(input));
      assertTrue(ExceptionUtils.findClassInRootChain(exception, QPermissionDeniedException.class) != null);
   }



   /*******************************************************************************
    ** A tableName-based process still accepts a permitted user filter.
    *******************************************************************************/
   @Test
   void columnStatsAcceptsFullAccessFilter() throws Exception
   {
      QContext.getQInstance().addProcess(ColumnStatsStep.getProcessMetaData());
      RunProcessInput input = new RunProcessInput().withProcessName("columnStats").withInputSource(QInputSource.USER);
      input.addValue("tableName", TABLE);
      input.addValue("fieldName", "firstName");
      input.addValue("filterJSON", JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))));
      assertFalse(((List<?>) new RunProcessAction().execute(input).getValue("valueCounts")).isEmpty());
   }



   /*******************************************************************************
    ** A trusted process can return audit joins without their QueryInput metadata.
    *******************************************************************************/
   @Test
   void auditEgressRecognizesTablePrefixedDetails() throws Exception
   {
      new AuditsMetaDataProvider().defineAll(QContext.getQInstance(), table.getBackendName(), null);
      AuditAction.execute(TABLE, 1, Map.of(), "Changed " + SSN, List.of(new QRecord().withValue("fieldName", "ssn").withValue("oldValue", SSN).withValue("newValue", SSN)));
      List<QRecord> records;
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         records = new QueryAction().execute(new QueryInput("audit")
            .withQueryJoin(new QueryJoin("auditDetail").withSelect(true))).getRecords();
      }
      assertEquals(SSN, records.get(0).getValue("auditDetail.oldValue"));
      restrict(FieldAccessTier.MASKED);
      FieldAccessHelper.applyToRecords("audit", null, records, "process");
      assertNotEquals(SSN, records.get(0).getValue("auditDetail.oldValue"));
      FieldAccessSentinelScanner.assertSafe(records, "918273645");
   }



   /*******************************************************************************
    ** Caller filters and bulk values must be checked before trusted steps run.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = { "queryFilterJson", "filterJSON", "defaultQueryFilter" })
   void processRejectsCallerFilter(String name) throws Exception
   {
      registerNoopProcess();
      restrict(FieldAccessTier.MASKED);
      RunProcessInput input = new RunProcessInput().withProcessName("reviewProcess").withInputSource(QInputSource.USER);
      input.addValue(name, JsonUtils.toJson(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, EMAIL))));
      assertThrows(QPermissionDeniedException.class, () -> new RunProcessAction().execute(input));
   }



   /*******************************************************************************
    ** Enabled bulk-edit fields need FULL even though the following step is trusted.
    *******************************************************************************/
   @Test
   void processRejectsBulkWrites() throws Exception
   {
      registerNoopProcess();
      restrict(FieldAccessTier.MASKED);
      RunProcessInput input = new RunProcessInput().withProcessName("reviewProcess").withInputSource(QInputSource.USER);
      input.addValue("bulkEditEnabledFields", "email");
      input.addValue("email", EMAIL);
      assertThrows(QPermissionDeniedException.class, () -> new RunProcessAction().execute(input));
      restrict(FieldAccessTier.FULL);
      new RunProcessAction().execute(input);
   }



   /*******************************************************************************
    ** Minimal process for testing the real boundary before any backend work.
    *******************************************************************************/
   private void registerNoopProcess()
   {
      QContext.getQInstance().addProcess(new QProcessMetaData().withName("reviewProcess").withTableName(TABLE)
         .withStep(new QBackendStepMetaData().withName("noop").withCode(new QCodeReference(NoopStep.class))));
   }



   /*******************************************************************************
    ** A process that succeeds if the boundary lets an unsafe input reach its steps.
    *******************************************************************************/
   public static class NoopStep implements BackendStep
   {
      /*******************************************************************************
       ** No trusted work is necessary for the input-boundary regression.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output)
      {
      }
   }



   /*******************************************************************************
    ** Reports must retain the customizer's alias to apply the joined field policy.
    *******************************************************************************/
   @Test
   void reportUsesFinalCustomizerJoins() throws Exception
   {
      restrict(FieldAccessTier.MASKED);
      QReportView view = new QReportView()
         .withName("view").withType(ReportType.TABLE).withDataSourceName("people")
         .withColumns(List.of(new QReportField().withName("child.notes").withLabel("Notes").withType(QFieldType.STRING).withIsVirtual(true)));
      QContext.getQInstance().addReport(new QReportMetaData().withName("reviewReport")
         .withDataSources(List.of(new QReportDataSource().withName("people").withSourceTable(TABLE)
            .withQueryInputCustomizer(new QCodeReference(JoinedReport.class)))).withViews(List.of(view)));
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ReportInput input = new ReportInput();
      input.setReportName("reviewReport");
      input.setReportDestination(new ReportDestination()
         .withReportFormat(ReportFormat.CSV).withReportOutputStream(bytes));
      new GenerateReportAction().execute(input);
      assertTrue(bytes.toString().contains("***"));
      FieldAccessSentinelScanner.assertSafe(bytes.toString(), "ABCDEFG");
   }



   /*******************************************************************************
    ** The alias exists only after query customization.
    *******************************************************************************/
   public static class JoinedReport implements DataSourceQueryInputCustomizer
   {
      /*******************************************************************************
       ** Select the protected child's notes under a custom alias.
       *******************************************************************************/
      @Override
      public QueryInput run(ReportInput input, QueryInput query)
      {
         return (query.withQueryJoin(new QueryJoin(CHILD).withAlias("child").withSelect(true).withType(QueryJoin.Type.LEFT)));
      }
   }



   /*******************************************************************************
    ** Audit payloads cannot be used as an oracle, including joins and ordering.
    *******************************************************************************/
   @Test
   void auditPayloadCriteriaAreRefused() throws Exception
   {
      new AuditsMetaDataProvider().defineAll(QContext.getQInstance(), table.getBackendName(), null);
      for(String name : List.of("oldValue", "newValue", "message"))
      {
         for(String base : List.of("auditDetail", "audit"))
         {
            List<QueryJoin> joins = List.of(new QueryJoin("auditDetail").withAlias("detail"));
            String field = base.equals("auditDetail") ? name : "detail." + name;
            QQueryFilter filter = new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.STARTS_WITH, "12"));
            assertThrows(QPermissionDeniedException.class, () -> FieldAccessHelper.assertCriteriaAllowed(base, joins, filter, null));
            assertThrows(QPermissionDeniedException.class, () -> FieldAccessHelper.assertCriteriaAllowed(base, joins, new QQueryFilter().withOrderBy(new QFilterOrderBy(field)), null));
            assertThrows(QPermissionDeniedException.class, () -> FieldAccessHelper.assertCriteriaAllowed(base, joins, null, List.of(field)));
         }
      }
   }



   /*******************************************************************************
    ** Fingerprinting must never receive a query or expression object.
    *******************************************************************************/
   @Test
   void revealableOnlyAcceptsLiterals()
   {
      restrict(FieldAccessTier.REVEALABLE);
      for(Serializable value : List.of(new QSubQuery(TABLE, "email", new QQueryFilter()), new CurrentUser(), "${currentUser}"))
      {
         assertThrows(QPermissionDeniedException.class, () -> FieldAccessHelper.assertCriteriaAllowed(TABLE, null,
            new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, value)), null));
         assertTrue(resolver.fingerprintValues.isEmpty());
      }
   }



   /*******************************************************************************
    ** Splitting IN across nested criteria or aliases cannot enlarge the budget.
    *******************************************************************************/
   @Test
   void revealableValueBudgetCoversWholeTree()
   {
      restrict(FieldAccessTier.REVEALABLE);
      QQueryFilter filter = new QQueryFilter();
      for(int i = 0; i < 26; i++)
      {
         filter.addSubFilter(new QQueryFilter(new QFilterCriteria(i % 2 == 0 ? "email" : TABLE + ".email", QCriteriaOperator.EQUALS, "candidate" + i)));
      }
      assertThrows(QPermissionDeniedException.class, () -> FieldAccessHelper.assertCriteriaAllowed(TABLE, null, filter, null));
   }



   /*******************************************************************************
    ** Trusted translation still has to be sanitized at the outer egress boundary.
    *******************************************************************************/
   @Test
   void trustedPossibleValueLabelIsReplacedById() throws Exception
   {
      QPossibleValueSource source = new QPossibleValueSource().withName("reviewPeople").withType(QPossibleValueSourceType.TABLE)
         .withTableName(TABLE).withValueFields(List.of("label")).withValueFormat("%s");
      QContext.getQInstance().addPossibleValueSource(source);
      QFieldMetaData reference = new QFieldMetaData("person", QFieldType.INTEGER).withPossibleValueSourceName(source.getName());
      table.addField(reference);
      restrict(FieldAccessTier.MASKED);
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         String label = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession()).withFieldAccessEnforcement(false).translatePossibleValue(reference, 1);
         assertEquals(EMAIL, label);
         QRecord record = new QRecord().withValue("id", 2).withValue("person", 1).withDisplayValue("person", label);
         FieldAccessHelper.applyToRecords(TABLE, null, List.of(record), "process");
         assertEquals("1", record.getDisplayValue("person"));
      }
   }



   /*******************************************************************************
    ** Unknown aliases must not leave either value representation untouched.
    *******************************************************************************/
   @Test
   void unknownJoinAliasIsWithheld()
   {
      QRecord record = new QRecord().withValue("id", 1).withValue("unresolved.notes", NOTES).withDisplayValue("unresolved.notes", NOTES);
      FieldAccessHelper.applyToRecords(TABLE, null, List.of(record), "report");
      assertFalse(record.getValues().containsKey("unresolved.notes"));
      assertFalse(record.getDisplayValues().containsKey("unresolved.notes"));
   }



   /*******************************************************************************
    ** Metadata-only uses a complete mask even when a resolver requests a partial one.
    *******************************************************************************/
   @Test
   void assistantMetadataOnlyAndPrimaryKey()
   {
      QContext.getQSession().setClientChannel("assistant");
      table.getField("email").withSupplementalMetaData(new AiFieldMetaData().withExposure(AiExposure.METADATA_ONLY));
      table.getField("id").withSupplementalMetaData(new AiFieldMetaData().withExposure(AiExposure.METADATA_ONLY));
      QContext.getQInstance().getSupplementalMetaData().remove(FieldAccessResolverMetaData.NAME);
      FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance()).withResolverInstance((t, f) -> new FieldAccessDecision(FieldAccessTier.MASKED, FieldMaskStyle.LAST_4, "partial"));
      QRecord record = new QRecord().withValue("id", 1).withValue("email", EMAIL);
      FieldAccessHelper.applyToRecords(TABLE, null, List.of(record), "query");
      assertEquals("***", record.getValue("email"));
      assertEquals(1, record.getValue("id"));
   }



   /*******************************************************************************
    ** A partial resolver style cannot weaken a FULL lock mask.
    *******************************************************************************/
   @Test
   void fullLockMaskWinsOverPartialResolver()
   {
      table.getField("email").withFieldSecurityLock(new FieldSecurityLock().withDefaultBehavior(FieldSecurityLock.Behavior.MASK).withMaskStyle(FieldMaskStyle.FULL));
      QContext.getQInstance().getSupplementalMetaData().remove(FieldAccessResolverMetaData.NAME);
      FieldAccessResolverMetaData.ofOrWithNew(QContext.getQInstance()).withResolverInstance((t, f) -> new FieldAccessDecision(FieldAccessTier.MASKED, FieldMaskStyle.EMAIL, "partial"));
      assertEquals(FieldMaskStyle.FULL, FieldAccessHelper.getDecision(table, table.getField("email")).maskStyle());
   }



   /*******************************************************************************
    ** Preliminary possible-value checks must not reserve extra exact searches.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(booleans = { false, true })
   void possibleValueGuardrailRunsOnce(boolean picker) throws Exception
   {
      restrict(FieldAccessTier.REVEALABLE);
      QContext.getQInstance().addPossibleValueSource(new QPossibleValueSource().withName("reviewSearch").withType(QPossibleValueSourceType.TABLE)
         .withTableName(TABLE).withValueFields(List.of("label")).withValueFormat("%s"));
      SearchPossibleValueSourceInput input = new SearchPossibleValueSourceInput().withPossibleValueSourceName("reviewSearch")
         .withDefaultQueryFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, EMAIL))).withLimit(10);
      if(picker)
      {
         QContext.getQInstance().getPossibleValueSource("reviewSearch").setPicker(new QPossibleValuePicker().withFilterFields(List.of("email")));
         input.withPickerRequest(true).withPickerFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, EMAIL)));
      }
      new SearchPossibleValueSourceAction().execute(input);
      assertEquals(1, resolver.exactMatchChecks);
      assertEquals(1, resolver.events.stream().filter(e -> e.getType() == FieldAccessEventType.EXACT_MATCH_FILTER).count());
   }



   /*******************************************************************************
    ** The real core process boundary must protect history without an HTTP wrapper.
    *******************************************************************************/
   @Test
   void userAuditProcessMasksHistory() throws Exception
   {
      new AuditsMetaDataProvider().defineAll(QContext.getQInstance(), table.getBackendName(), null);
      AuditAction.execute(TABLE, 1, Map.of(), "Changed " + SSN, List.of(new QRecord().withValue("fieldName", "ssn").withValue("oldValue", SSN).withValue("newValue", SSN)));
      restrict(FieldAccessTier.MASKED);
      RunProcessInput input = new RunProcessInput().withProcessName(GetAuditsForRecordProcess.NAME).withInputSource(QInputSource.USER);
      input.addValue("tableName", TABLE);
      input.addValue("recordId", 1);
      List<QRecord> audits = (List<QRecord>) new RunProcessAction().execute(input).getValue("audits");
      assertFalse(audits.isEmpty());
      assertNotEquals(SSN, audits.get(0).getValue("auditDetail.oldValue"));
      FieldAccessSentinelScanner.assertSafe(audits, "918273645");
   }
}
