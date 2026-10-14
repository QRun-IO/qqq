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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEvent;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessEventType;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessOutcome;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldValueMasker;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportAction;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableDefinition;
import com.kingsrook.qqq.backend.core.model.actions.reporting.pivottable.PivotTableGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.CurrentUser;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.JsonPathStorage;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportDataSource;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportField;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.QReportView;
import com.kingsrook.qqq.backend.core.model.metadata.reporting.ReportType;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Secured field access contract tests.
 *******************************************************************************/
class FieldAccessEnforcementTest extends FieldAccessTestSupport
{



   /*******************************************************************************
    ** JSON-backed custom fields obey the same value and criteria policy.
    *******************************************************************************/
   @Test
   void maskedCustomField() throws Exception
   {
      table.addField(new QFieldMetaData("customSecret", QFieldType.STRING).withBackendName("ignored")
         .withBackendStorage(new JsonPathStorage().withColumnName("qqq_custom_fields").withKey("customSecret")));
      new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 1).withValue("customSecret", NOTES)));
      assertEquals(NOTES, new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(1)).getValue("customSecret"));
      MapResolver.tiers.put(TABLE + ".customSecret", FieldAccessTier.MASKED);
      QContext.removeObject(FieldAccessHelper.CACHE_KEY);
      QRecord record = new QueryAction().execute(new QueryInput(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1)))
         .withShouldGenerateDisplayValues(true)).getRecords().get(0);
      assertValue(FieldAccessTier.MASKED, record, "customSecret", NOTES);
      assertFalse(record.getValues().containsKey("qqq_custom_fields"));
      assertValue(FieldAccessTier.MASKED, new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(1).withShouldGenerateDisplayValues(true)), "customSecret", NOTES);
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("customSecret", QCriteriaOperator.EQUALS, NOTES));
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withFilter(filter)));
      assertThrows(QPermissionDeniedException.class, () -> new CountAction().execute(new CountInput(TABLE).withFilter(filter)));
   }



   /*******************************************************************************
    ** Expressions are accepted for FULL fields, but never for REVEALABLE searches.
    *******************************************************************************/
   @Test
   void currentUserCriteriaEnforcedBeforeExpansion() throws Exception
   {
      for(Serializable expression : List.of(new CurrentUser(), "${currentUser}"))
      {
         QQueryFilter filter = new QQueryFilter().withSubFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, expression)));
         QContext.setQSession(newSession(EMAIL));
         restrict(FieldAccessTier.MASKED);
         assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withFilter(filter)));
         assertThrows(QPermissionDeniedException.class, () -> new CountAction().execute(new CountInput(TABLE).withFilter(filter)));

         restrict(FieldAccessTier.REVEALABLE);
         resolver.fingerprintValues.clear();
         assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withFilter(filter)));
         assertThrows(QPermissionDeniedException.class, () -> new CountAction().execute(new CountInput(TABLE).withFilter(filter)));
         assertTrue(resolver.fingerprintValues.isEmpty());
         restrict(FieldAccessTier.FULL);
         assertEquals(2, new CountAction().execute(new CountInput(TABLE).withFilter(filter)).getCount());
         assertEquals(2, new QueryAction().execute(new QueryInput(TABLE).withFilter(filter)).getRecords().size());
         assertEquals(expression, filter.getSubFilters().get(0).getCriteria().get(0).getValues().get(0));
      }
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(FieldAccessTier.class)
   void recordsAndMetadata(FieldAccessTier tier) throws Exception
   {
      restrict(tier);
      QueryInput input = new QueryInput(TABLE).withShouldGenerateDisplayValues(true).withIncludeAssociations(true);
      List<QRecord> records = new QueryAction().execute(input).getRecords();
      assertEquals(2, records.size());
      QRecord first = records.get(0);
      assertValue(tier, first, "email", EMAIL);
      assertValue(tier, first, "ssn", SSN);
      assertValue(tier, first.getAssociatedRecords().get("children").get(0), "notes", NOTES);
      QRecord got = new GetAction().executeForRecord(new GetInput(TABLE).withPrimaryKey(1).withShouldGenerateDisplayValues(true));
      assertValue(tier, got, "email", EMAIL);
      if(tier != FieldAccessTier.FULL)
      {
         assertNull(first.getRecordLabel());
         assertNull(got.getRecordLabel());
         FieldAccessSentinelScanner.assertSafe(records, "918273645", "ABCDEFG");
      }
      QTableMetaData personalized = FieldAccessHelper.applyToTable(table);
      QFrontendTableMetaData frontend = new QFrontendTableMetaData(new TableMetaDataInput(), QContext.getQInstance().getBackend(table.getBackendName()), personalized, true, false);
      assertEquals(tier != FieldAccessTier.FULL, frontend.getHasRestrictedFields());
      if(tier == FieldAccessTier.HIDDEN)
      {
         assertFalse(frontend.getFields().containsKey("email"));
      }
      else
      {
         QFrontendFieldMetaData email = frontend.getFields().get("email");
         assertEquals(tier == FieldAccessTier.FULL ? null : tier.name(), email.getAccessTier());
         assertEquals(tier == FieldAccessTier.FULL ? null : false, email.getIsSortable());
         assertEquals(tier == FieldAccessTier.FULL ? null : tier == FieldAccessTier.REVEALABLE ? List.of("EQUALS", "IN") : List.of(), email.getAllowedFilterOperators());
         assertEquals(tier == FieldAccessTier.FULL, email.getIsEditable());
      }
      FieldAccessSentinelScanner.assertSafe(frontend, "918273645", "ABCDEFG");
   }



   /*******************************************************************************
    ** Check both value channels.
    *******************************************************************************/
   private void assertValue(FieldAccessTier tier, QRecord record, String name, String original)
   {
      if(tier == FieldAccessTier.HIDDEN)
      {
         assertFalse(record.getValues().containsKey(name));
         assertFalse(record.getDisplayValues().containsKey(name));
      }
      else if(tier == FieldAccessTier.FULL)
      {
         assertEquals(original, record.getValue(name));
      }
      else
      {
         String mask = FieldValueMasker.mask(original, QFieldType.STRING, FieldMaskStyle.AUTO);
         assertEquals(mask, record.getValue(name));
         assertEquals(mask, record.getDisplayValue(name));
      }
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(QCriteriaOperator.class)
   void criteriaOperators(QCriteriaOperator operator) throws Exception
   {
      restrict(FieldAccessTier.REVEALABLE);
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("email", operator, EMAIL));
      if(operator == QCriteriaOperator.EQUALS || operator == QCriteriaOperator.IN)
      {
         assertEquals(2, new QueryAction().execute(new QueryInput(TABLE).withFilter(filter)).getRecords().size());
         assertTrue(resolver.events.stream().anyMatch(event -> event.getType() == FieldAccessEventType.EXACT_MATCH_FILTER && event.getFieldNames().equals(List.of("email"))));
      }
      else
      {
         QPermissionDeniedException exception = assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withFilter(filter)));
         assertEquals("Permission denied.", exception.getMessage());
         assertTrue(resolver.events.stream().anyMatch(event -> event.getType() == FieldAccessEventType.DENIED));
      }
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(FieldAccessTier.class)
   void criteriaCountsAggregatesAndWrites(FieldAccessTier tier) throws Exception
   {
      restrict(tier);
      QQueryFilter equals = new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS, EMAIL));
      if(tier == FieldAccessTier.FULL || tier == FieldAccessTier.REVEALABLE)
      {
         assertEquals(2, new CountAction().execute(new CountInput(TABLE).withFilter(equals)).getCount());
         table.withUniqueKey(new UniqueKey("email"));
         assertNotNull(new GetAction().executeForRecord(new GetInput(TABLE).withUniqueKey(Map.of("email", EMAIL))));
         table.getUniqueKeys().removeIf(key -> key.getFieldNames().equals(List.of("email")));
      }
      else
      {
         assertThrows(QPermissionDeniedException.class, () -> new CountAction().execute(new CountInput(TABLE).withFilter(equals)));
      }
      assertEquals(2, new CountAction().execute(new CountInput(TABLE)).getCount());
      AggregateInput aggregate = new AggregateInput(TABLE).withAggregate(new Aggregate("email", AggregateOperator.COUNT));
      AggregateInput grouped = new AggregateInput(TABLE).withAggregate(new Aggregate("id", AggregateOperator.COUNT)).withGroupBy(new GroupBy(table.getField("email")));
      QueryInput sorted = new QueryInput(TABLE).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("email")));
      InsertInput insert = new InsertInput(TABLE).withRecord(new QRecord().withValue("firstName", "Gamma").withValue("lastName", "Three").withValue("email", EMAIL));
      UpdateInput update = new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 1).withValue("email", null));
      if(tier == FieldAccessTier.FULL)
      {
         assertDoesNotThrow(() -> new AggregateAction().execute(aggregate));
         assertDoesNotThrow(() -> new AggregateAction().execute(grouped));
         assertDoesNotThrow(() -> new QueryAction().execute(sorted));
         assertDoesNotThrow(() -> new InsertAction().execute(insert));
         assertDoesNotThrow(() -> new UpdateAction().execute(update));
      }
      else
      {
         assertThrows(QPermissionDeniedException.class, () -> new AggregateAction().execute(aggregate));
         assertThrows(QPermissionDeniedException.class, () -> new AggregateAction().execute(grouped));
         assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(sorted));
         assertThrows(QPermissionDeniedException.class, () -> new InsertAction().execute(insert));
         assertThrows(QPermissionDeniedException.class, () -> new UpdateAction().execute(update));
      }
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void boundedInAndFieldComparisons() throws Exception
   {
      restrict(FieldAccessTier.REVEALABLE);
      List<Serializable> values = new ArrayList<>();
      for(int i = 0; i < 25; i++)
      {
         values.add(EMAIL);
      }
      assertDoesNotThrow(() -> FieldAccessHelper.assertCriteriaAllowed(TABLE, null, new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.IN, values)), null));
      values.add(EMAIL);
      assertThrows(QPermissionDeniedException.class, () -> FieldAccessHelper.assertCriteriaAllowed(TABLE, null, new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.IN, values)), null));
      QQueryFilter comparison = new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS).withOtherFieldName("email"));
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withFilter(new QQueryFilter().withSubFilter(comparison))));
      QQueryFilter reverse = new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.EQUALS).withOtherFieldName("firstName"));
      assertThrows(QPermissionDeniedException.class, () -> FieldAccessHelper.assertCriteriaAllowed(TABLE, null, reverse, null));
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(FieldAccessTier.class)
   void joinedAndAssociatedRecords(FieldAccessTier tier) throws Exception
   {
      restrict(tier);
      QueryJoin join = new QueryJoin().withJoinTable(CHILD).withAlias("child").withSelect(true);
      List<QRecord> records = new QueryAction().execute(new QueryInput(TABLE).withQueryJoin(join).withShouldGenerateDisplayValues(true)).getRecords();
      assertEquals(1, records.size());
      assertValue(tier, records.get(0), "child.notes", NOTES);
      if(tier != FieldAccessTier.FULL)
      {
         FieldAccessSentinelScanner.assertSafe(records, "918273645", "ABCDEFG");
         assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withQueryJoin(join).withFilter(new QQueryFilter(new QFilterCriteria("child.notes", QCriteriaOperator.CONTAINS, "SENTINEL")))));
         assertThrows(QPermissionDeniedException.class, () -> new InsertAction().execute(new InsertInput(TABLE).withRecord(new QRecord().withValue("firstName", "Parent").withValue("lastName", "Four").withAssociatedRecords("children", List.of(new QRecord().withValue("notes", NOTES))))));
      }
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void revealAndAudit() throws Exception
   {
      restrict(FieldAccessTier.REVEALABLE);
      resolver.reveals.add(TABLE + ".email.1");
      resolver.reveals.add(CHILD + ".notes.10");
      List<QRecord> records = new QueryAction().execute(new QueryInput(TABLE).withShouldGenerateDisplayValues(true).withIncludeAssociations(true)).getRecords();
      assertEquals(EMAIL, records.get(0).getValue("email"));
      assertEquals(EMAIL, records.get(0).getDisplayValue("email"));
      assertNotEquals(EMAIL, records.get(1).getValue("email"));
      assertNotEquals(SSN, records.get(0).getValue("ssn"));
      assertEquals(NOTES, records.get(0).getAssociatedRecords().get("children").get(0).getValue("notes"));
      FieldAccessEvent event = resolver.events.stream().filter(e -> e.getType() == FieldAccessEventType.VIEW && TABLE.equals(e.getTableName())).findFirst().orElseThrow();
      assertEquals(List.of(1, 2), event.getPrimaryKeys());
      assertEquals(2, event.getRecordCount());
      assertEquals("query", event.getActionName());
      assertEquals(1, resolver.events.stream().filter(e -> e.getType() == FieldAccessEventType.VIEW && CHILD.equals(e.getTableName())).count());
      FieldAccessSentinelScanner.assertSafe(resolver.events, "918273645", "ABCDEFG");
      resolver.throwOnAccess = true;
      assertEquals(2, new QueryAction().execute(new QueryInput(TABLE)).getRecords().size());
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @ParameterizedTest
   @EnumSource(FieldAccessTier.class)
   void exportsAndSearch(FieldAccessTier tier) throws Exception
   {
      restrict(tier);
      resolver.reveals.add(TABLE + ".email.1");
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ExportInput export = new ExportInput();
      export.setTableName(TABLE);
      new ExportAction().execute(export.withReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV).withReportOutputStream(bytes)));
      String csv = bytes.toString(StandardCharsets.UTF_8);
      if(tier == FieldAccessTier.FULL)
      {
         assertTrue(csv.contains(EMAIL));
      }
      else
      {
         FieldAccessSentinelScanner.assertSafe(csv, "918273645", "ABCDEFG");
         if(tier != FieldAccessTier.HIDDEN)
         {
            assertTrue(csv.contains("***"));
         }
      }
      RecordSearchOutput search = new RecordSearchAction().execute(new RecordSearchInput().withTableNames(List.of(TABLE)).withSearchTerm("SENTINEL"));
      if(tier != FieldAccessTier.FULL)
      {
         assertTrue(search.getResults().isEmpty());
         FieldAccessSentinelScanner.assertSafe(new RecordSearchAction().execute(new RecordSearchInput().withTableNames(List.of(TABLE)).withSearchTerm("Alpha")), "918273645", "ABCDEFG");
      }
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void reportsMaskAndRefuseGrouping() throws Exception
   {
      restrict(FieldAccessTier.MASKED);
      QReportView view = new QReportView().withName("view").withLabel("View").withType(ReportType.TABLE).withDataSourceName("people")
         .withColumns(List.of(new QReportField().withName("email").withLabel("Email").withType(QFieldType.STRING)));
      QReportMetaData report = new QReportMetaData().withName("securedReport").withDataSources(List.of(new QReportDataSource().withName("people").withSourceTable(TABLE))).withViews(List.of(view));
      QContext.getQInstance().addReport(report);
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ReportInput input = new ReportInput();
      input.setReportName(report.getName());
      input.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV).withReportOutputStream(bytes));
      new GenerateReportAction().execute(input);
      assertTrue(bytes.toString().contains("***"));
      FieldAccessSentinelScanner.assertSafe(bytes.toString(), "918273645", "ABCDEFG");
      view.withSummaryFields(List.of("email"));
      assertThrows(QPermissionDeniedException.class, () -> new GenerateReportAction().execute(input));
      view.setSummaryFields(null);
      view.withOrderByFields(List.of(new QFilterOrderBy("email")));
      assertThrows(QPermissionDeniedException.class, () -> new GenerateReportAction().execute(input));
      view.setOrderByFields(null);
      view.withPivotTableDefinition(new PivotTableDefinition().withRow(new PivotTableGroupBy().withFieldName("email")));
      assertThrows(QPermissionDeniedException.class, () -> new GenerateReportAction().execute(input));
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void possibleValuesUseIdsAndSkipRestrictedSearch() throws Exception
   {
      restrict(FieldAccessTier.MASKED);
      QPossibleValueSource source = new QPossibleValueSource().withName("securedPeople").withType(QPossibleValueSourceType.TABLE).withTableName(TABLE).withSearchFields(List.of("email", "firstName")).withValueFields(List.of("label")).withValueFormat("%s");
      QContext.getQInstance().addPossibleValueSource(source);
      SearchPossibleValueSourceOutput hiddenMatch = new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput().withPossibleValueSourceName(source.getName()).withSearchTerm("SENTINEL").withLimit(10));
      assertTrue(hiddenMatch.getResults().isEmpty());
      SearchPossibleValueSourceOutput visibleMatch = new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput().withPossibleValueSourceName(source.getName()).withSearchTerm("Alpha").withLimit(10));
      assertEquals(1, visibleMatch.getResults().size());
      assertEquals("1", visibleMatch.getResults().get(0).getLabel());
      FieldAccessSentinelScanner.assertSafe(visibleMatch, "918273645", "ABCDEFG");
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void reusedPossibleValueCacheCannotExposePreviousSessionLabel() throws Exception
   {
      QPossibleValueSource source = new QPossibleValueSource().withName("reusedPeople").withType(QPossibleValueSourceType.TABLE).withTableName(TABLE).withValueFields(List.of("label")).withValueFormat("%s");
      QContext.getQInstance().addPossibleValueSource(source);
      QPossibleValueTranslator translator = new QPossibleValueTranslator(QContext.getQInstance(), QContext.getQSession());
      QFieldMetaData reference = new QFieldMetaData("person", QFieldType.INTEGER).withPossibleValueSourceName(source.getName());
      assertEquals(EMAIL, translator.translatePossibleValue(reference, 1));
      MapResolver.tiers.put(TABLE + ".email", FieldAccessTier.MASKED);
      QContext.setQSession(newSession("restricted"));
      assertEquals("1", translator.translatePossibleValue(reference, 1));
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void joinedRevealUsesJoinedPrimaryKey() throws Exception
   {
      restrict(FieldAccessTier.REVEALABLE);
      resolver.reveals.add(CHILD + ".notes.1");
      QueryJoin join = new QueryJoin().withJoinTable(CHILD).withAlias("child").withSelect(true);
      assertNotEquals(NOTES, new QueryAction().execute(new QueryInput(TABLE).withQueryJoin(join)).getRecords().get(0).getValue("child.notes"));
      resolver.reveals.add(CHILD + ".notes.10");
      assertEquals(NOTES, new QueryAction().execute(new QueryInput(TABLE).withQueryJoin(join)).getRecords().get(0).getValue("child.notes"));
   }



   /*******************************************************************************
    ** Verify the secured-data contract.
    *******************************************************************************/
   @Test
   void reportAggregateFormulaIsRefused() throws Exception
   {
      restrict(FieldAccessTier.MASKED);
      QReportMetaData report = new QReportMetaData().withName("securedSummary").withDataSources(List.of(new QReportDataSource().withName("people").withSourceTable(TABLE)))
         .withViews(List.of(new QReportView().withName("summary").withType(ReportType.SUMMARY).withDataSourceName("people").withSummaryFields(List.of("firstName"))
            .withColumns(List.of(new QReportField().withName("count").withFormula("${pivot.count.email}")))));
      QContext.getQInstance().addReport(report);
      ReportInput input = new ReportInput();
      input.setReportName(report.getName());
      input.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.CSV).withReportOutputStream(new ByteArrayOutputStream()));
      assertThrows(QPermissionDeniedException.class, () -> new GenerateReportAction().execute(input));
   }



   /*******************************************************************************
    ** Resolver opt-in enforces only USER field access, preserving core table opt-in.
    *******************************************************************************/
   @Test
   void resolverGateDoesNotEnableTablePermissions() throws Exception
   {
      QContext.getQInstance().setEnforcePermissionsInCoreActions(false);
      restrict(FieldAccessTier.MASKED);
      table.withPermissionRules(new QPermissionRules().withPermissionBaseName("ungranted"));
      QueryInput userInput = new QueryInput(TABLE).withInputSource(QInputSource.USER);
      assertFalse(CorePermissionEnforcer.shouldEnforce(userInput));
      assertTrue(CorePermissionEnforcer.shouldEnforceFieldAccess(userInput));
      assertNotEquals(EMAIL, new QueryAction().execute(userInput).getRecords().get(0).getValueString("email"));
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withInputSource(QInputSource.USER)
         .withFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.CONTAINS, "SENTINEL")))));
      assertEquals(EMAIL, new QueryAction().execute(new QueryInput(TABLE)).getRecords().get(0).getValueString("email"));
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         assertEquals(EMAIL, new QueryAction().execute(new QueryInput(TABLE).withInputSource(QInputSource.USER)).getRecords().get(0).getValueString("email"));
      }
      QueryInput bypass = new QueryInput(TABLE).withInputSource(QInputSource.USER);
      bypass.withPermissionBypass("trusted test");
      assertEquals(EMAIL, new QueryAction().execute(bypass).getRecords().get(0).getValueString("email"));
      QContext.getQInstance().getSupplementalMetaData().remove(FieldAccessResolverMetaData.NAME);
      table.getField("email").setFieldSecurityLock(new FieldSecurityLock().withDefaultBehavior(FieldSecurityLock.Behavior.MASK));
      assertFalse(CorePermissionEnforcer.shouldEnforceFieldAccess(new QueryInput(TABLE).withInputSource(QInputSource.USER)));
      assertEquals(EMAIL, new QueryAction().execute(new QueryInput(TABLE).withInputSource(QInputSource.USER)).getRecords().get(0).getValueString("email"));
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withInputSource(QInputSource.USER)));
   }
}
