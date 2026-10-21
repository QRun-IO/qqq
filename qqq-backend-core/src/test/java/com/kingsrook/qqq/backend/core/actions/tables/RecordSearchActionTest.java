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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchHighlight;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchHighlightMatch;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchProviderState;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchSort;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchTableSummary;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for {@link RecordSearchAction}
 *******************************************************************************/
class RecordSearchActionTest extends BaseTest
{

   /*******************************************************************************
    ** personMemory is searchable by name, email and id; order by order number
    ** (and is locked by store).
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withLabel("Person")
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("firstName", "lastName")
         .withSearchFields("firstName", "lastName", "email", "id");
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER)
         .withLabel("Order")
         .withRecordLabelFormat("Order %s")
         .withRecordLabelFields("orderNo")
         .withSearchFields("orderNo");

      QContext.withTemporaryContext(new CapturedContext(qInstance, new QSystemUserSession()), () ->
      {
         new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff").withValue("email", "darin@example.invalid"),
            new QRecord().withValue("id", 2).withValue("firstName", "Linda").withValue("lastName", "KELKHOFF").withValue("email", "linda@example.invalid"),
            new QRecord().withValue("id", 3).withValue("firstName", "Tim").withValue("lastName", "Chamberlain").withValue("email", "tim@example.invalid"))));
         new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("orderNo", "KEL-1").withValue("storeId", 1),
            new QRecord().withValue("id", 2).withValue("orderNo", "KEL-2").withValue("storeId", 2))));
      });

      TestRecordSearchProvider.reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void useTestProvider()
   {
      QContext.getQInstance().withRecordSearchProvider(new QCodeReference(TestRecordSearchProvider.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<RecordSearchResult> search(String term) throws QException
   {
      return (search(new RecordSearchInput().withSearchTerm(term)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<RecordSearchResult> search(RecordSearchInput input) throws QException
   {
      return (new RecordSearchAction().execute(input.withInputSource(QInputSource.USER)).getResults());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> labels(List<RecordSearchResult> results)
   {
      return (results.stream().map(r -> r.getTableName() + ":" + r.getRecordId() + ":" + r.getRecordLabel()).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMatchesStringFieldsCaseInsensitivelyWithTableLabelAndRecordLabel() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);

      List<RecordSearchResult> results = search("  KeL ");
      assertThat(labels(results)).containsExactly(
         "personMemory:1:Darin Kelkhoff",
         "personMemory:2:Linda KELKHOFF",
         "order:1:Order KEL-1");
      assertThat(results.get(0).getTableLabel()).isEqualTo("Person");
      assertThat(results.get(2).getTableLabel()).isEqualTo("Order");

      assertThat(labels(search("EXAMPLE.INVALID"))).containsExactly("personMemory:1:Darin Kelkhoff", "personMemory:2:Linda KELKHOFF", "personMemory:3:Tim Chamberlain");
      assertThat(search("nobody")).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIntegerFieldsMatchNumericTermsOnly() throws QException
   {
      assertThat(labels(search("3"))).containsExactly("personMemory:3:Tim Chamberlain");
      assertThat(search("99999999999999999")).isEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordSecurityLocksApply() throws QException
   {
      assertThat(labels(search(new RecordSearchInput().withSearchTerm("KEL-").withTableNames(List.of(TestUtils.TABLE_NAME_ORDER))))).isEmpty();

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 2);
      assertThat(labels(search(new RecordSearchInput().withSearchTerm("KEL-").withTableNames(List.of(TestUtils.TABLE_NAME_ORDER))))).containsExactly("order:2:Order KEL-2");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTablesWithoutReadPermissionAreSkipped() throws QException
   {
      for(String tableName : List.of(TestUtils.TABLE_NAME_PERSON_MEMORY, TestUtils.TABLE_NAME_ORDER))
      {
         QContext.getQInstance().getTable(tableName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);

      QContext.getQSession().withPermission(TestUtils.TABLE_NAME_ORDER + ".read");
      assertThat(labels(search("kel"))).containsExactly("order:1:Order KEL-1");

      /////////////////////////////////////////////////////////////////////////////
      // naming a table the session may not read returns nothing - and no error //
      /////////////////////////////////////////////////////////////////////////////
      assertThat(search(new RecordSearchInput().withSearchTerm("kel").withTableNames(List.of(TestUtils.TABLE_NAME_PERSON_MEMORY)))).isEmpty();

      QContext.getQSession().withPermission(TestUtils.TABLE_NAME_PERSON_MEMORY + ".read");
      assertThat(labels(search("kel"))).containsExactly("personMemory:1:Darin Kelkhoff", "personMemory:2:Linda KELKHOFF", "order:1:Order KEL-1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableNamesScopeAndUnsearchableTables() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);

      assertThat(labels(search(new RecordSearchInput().withSearchTerm("kel").withTableNames(List.of(TestUtils.TABLE_NAME_ORDER))))).containsExactly("order:1:Order KEL-1");

      ////////////////////////////////////////////////////////////////////////////
      // unknown tables, and tables without search fields, are silently skipped //
      ////////////////////////////////////////////////////////////////////////////
      assertThat(search(new RecordSearchInput().withSearchTerm("kel").withTableNames(List.of("notATable", TestUtils.TABLE_NAME_PERSON)))).isEmpty();

      ///////////////////////////////////////////////////////
      // a table whose query capability is disabled is too //
      ///////////////////////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER).withoutCapability(Capability.TABLE_QUERY);
      assertThat(labels(search("kel"))).containsExactly("personMemory:1:Darin Kelkhoff", "personMemory:2:Linda KELKHOFF");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLimitPerTable() throws QException
   {
      List<QRecord> more = new ArrayList<>();
      for(int i = 10; i < 50; i++)
      {
         more.add(new QRecord().withValue("id", i).withValue("firstName", "Kelly").withValue("lastName", "Number " + i));
      }
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(more));

      assertThat(search("kel")).hasSize(RecordSearchAction.DEFAULT_LIMIT_PER_TABLE);
      assertThat(labels(search(new RecordSearchInput().withSearchTerm("kel").withLimitPerTable(1)))).containsExactly("personMemory:1:Darin Kelkhoff");
      assertThat(search(new RecordSearchInput().withSearchTerm("kel").withLimitPerTable(1000))).hasSize(RecordSearchAction.MAX_LIMIT_PER_TABLE);
      assertThat(search(new RecordSearchInput().withSearchTerm("kel").withLimitPerTable(0))).hasSize(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldsRemovedByPersonalizationAreNotSearched() throws QException
   {
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "email", DEFAULT_USER_ID);

      assertThat(search("example.invalid")).isEmpty();
      assertThat(labels(search("tim"))).containsExactly("personMemory:3:Tim Chamberlain");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidSearchTerms()
   {
      assertThatThrownBy(() -> search((String) null)).isInstanceOf(QBadRequestException.class).hasMessageContaining("A search term is required");
      assertThatThrownBy(() -> search("   ")).isInstanceOf(QBadRequestException.class).hasMessageContaining("A search term is required");
      assertThatThrownBy(() -> search("x".repeat(RecordSearchAction.MAX_SEARCH_TERM_LENGTH + 1))).isInstanceOf(QBadRequestException.class).hasMessageContaining("may not be longer than");
   }



   /*******************************************************************************
    ** A provider searches the tables it claims (its order, core's labels); other
    ** tables keep the default search; the output keeps instance table order.
    *******************************************************************************/
   @Test
   void testProviderSearchesClaimedTablesAndDefaultSearchesTheRest() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      useTestProvider();
      TestRecordSearchProvider.claim(TestUtils.TABLE_NAME_PERSON_MEMORY);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_PERSON_MEMORY, 3, 1);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_ORDER, 2);

      List<RecordSearchResult> results = search("  kel ");
      assertThat(labels(results)).containsExactly(
         "personMemory:3:Tim Chamberlain",
         "personMemory:1:Darin Kelkhoff",
         "order:1:Order KEL-1");
      assertThat(results.get(0).getTableLabel()).isEqualTo("Person");

      assertThat(TestRecordSearchProvider.getInputs()).hasSize(1);
      RecordSearchInput providerInput = TestRecordSearchProvider.getInputs().get(0);
      assertThat(providerInput.getSearchTerm()).isEqualTo("kel");
      assertThat(providerInput.getTableNames()).containsExactly(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertThat(providerInput.getLimitPerTable()).isEqualTo(RecordSearchAction.DEFAULT_LIMIT_PER_TABLE);
      assertThat(providerInput.getInputSource()).isEqualTo(QInputSource.USER);
   }



   /*******************************************************************************
    ** The provider is only asked about tables that are being searched - not ones
    ** outside the requested table names, nor ones without search fields.
    *******************************************************************************/
   @Test
   void testProviderNotCalledWhenItClaimsNoSearchedTable() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      useTestProvider();
      TestRecordSearchProvider.claim(TestUtils.TABLE_NAME_PERSON_MEMORY, TestUtils.TABLE_NAME_PERSON);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_PERSON, 1);

      assertThat(labels(search(new RecordSearchInput().withSearchTerm("kel").withTableNames(List.of(TestUtils.TABLE_NAME_ORDER, TestUtils.TABLE_NAME_PERSON)))))
         .containsExactly("order:1:Order KEL-1");
      assertThat(TestRecordSearchProvider.getInputs()).isEmpty();
   }



   /*******************************************************************************
    ** Provider hits are re-read through QueryAction: record security locks
    ** apply, ids that no longer exist (or do not convert) drop out, duplicates
    ** are removed, and the per-table limit applies after that.
    *******************************************************************************/
   @Test
   void testProviderResultsAreReadThroughQueryAction() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 2);
      useTestProvider();
      TestRecordSearchProvider.claim(TestUtils.TABLE_NAME_PERSON_MEMORY, TestUtils.TABLE_NAME_ORDER);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_PERSON_MEMORY, "2", 2, 99, "notAnId", null, 1);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_ORDER, 1, 2);

      assertThat(labels(search("anything"))).containsExactly(
         "personMemory:2:Linda KELKHOFF",
         "personMemory:1:Darin Kelkhoff",
         "order:2:Order KEL-2");

      assertThat(labels(search(new RecordSearchInput().withSearchTerm("anything").withLimitPerTable(1)))).containsExactly(
         "personMemory:2:Linda KELKHOFF",
         "order:2:Order KEL-2");
      assertThat(TestRecordSearchProvider.getInputs().get(1).getLimitPerTable()).isEqualTo(1);
   }



   /*******************************************************************************
    ** The provider is only offered tables the session may read, and results it
    ** returns for any other table are ignored.
    *******************************************************************************/
   @Test
   void testProviderRespectsTablePermissions() throws QException
   {
      for(String tableName : List.of(TestUtils.TABLE_NAME_PERSON_MEMORY, TestUtils.TABLE_NAME_ORDER))
      {
         QContext.getQInstance().getTable(tableName).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      QContext.getQSession().withPermission(TestUtils.TABLE_NAME_ORDER + ".read");

      useTestProvider();
      TestRecordSearchProvider.claim(TestUtils.TABLE_NAME_PERSON_MEMORY, TestUtils.TABLE_NAME_ORDER);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_PERSON_MEMORY, 1, 2);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_ORDER, 1);

      assertThat(labels(search("kel"))).containsExactly("order:1:Order KEL-1");
      assertThat(TestRecordSearchProvider.getInputs().get(0).getTableNames()).containsExactly(TestUtils.TABLE_NAME_ORDER);
   }



   /*******************************************************************************
    ** If the provider fails (in search or claimsTable), or cannot be loaded, its
    ** tables are searched with the default search - search does not fail.
    *******************************************************************************/
   @Test
   void testProviderFailureFallsBackToDefaultSearch() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      List<String> defaultResults = labels(search("kel"));
      assertThat(defaultResults).containsExactly("personMemory:1:Darin Kelkhoff", "personMemory:2:Linda KELKHOFF", "order:1:Order KEL-1");

      useTestProvider();
      TestRecordSearchProvider.claim(TestUtils.TABLE_NAME_PERSON_MEMORY);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_PERSON_MEMORY, 3);

      TestRecordSearchProvider.fail(true, false);
      assertThat(labels(search("kel"))).isEqualTo(defaultResults);
      assertThat(TestRecordSearchProvider.getInputs()).hasSize(1);

      TestRecordSearchProvider.fail(false, true);
      assertThat(labels(search("kel"))).isEqualTo(defaultResults);

      QContext.getQInstance().withRecordSearchProvider(new QCodeReference("com.kingsrook.qqq.NoSuchProvider", QCodeType.JAVA));
      assertThat(labels(search("kel"))).isEqualTo(defaultResults);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static RecordSearchOutput searchOutput(RecordSearchInput input) throws QException
   {
      return (new RecordSearchAction().execute(input.withInputSource(QInputSource.USER)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String highlighted(RecordSearchHighlight highlight)
   {
      StringBuilder rs   = new StringBuilder();
      int           last = 0;
      for(RecordSearchHighlightMatch match : highlight.getMatches())
      {
         rs.append(highlight.getText(), last, match.getStart()).append('[').append(highlight.getText(), match.getStart(), match.getEnd()).append(']');
         last = match.getEnd();
      }
      return (rs.append(highlight.getText().substring(last)).toString());
   }



   /*******************************************************************************
    ** Search fields outside the record label are highlighted where they match,
    ** and detailed (except the primary key) where they don't.
    *******************************************************************************/
   @Test
   void testHighlightsAndDetails() throws QException
   {
      List<RecordSearchResult> results = search("EXAMPLE");
      RecordSearchResult       darin   = results.get(0);
      assertThat(darin.getRecordLabel()).isEqualTo("Darin Kelkhoff");
      assertThat(darin.getHighlights()).hasSize(1);
      assertThat(darin.getHighlights().get(0).getFieldName()).isEqualTo("email");
      assertThat(darin.getHighlights().get(0).getFieldLabel()).isEqualTo("Email");
      assertThat(highlighted(darin.getHighlights().get(0))).isEqualTo("darin@[example].invalid");
      assertThat(darin.getDetails()).isNull();
      assertThat(darin.getScore()).isNull();
      assertThat(darin.getUpdatedAt()).isNotNull();

      ///////////////////////////////////////////////////////////////////////
      // a match in the label only: no highlight; the email is a detail    //
      ///////////////////////////////////////////////////////////////////////
      darin = search("kelkhoff").get(0);
      assertThat(darin.getHighlights()).isNull();
      assertThat(darin.getDetails()).hasSize(1);
      assertThat(darin.getDetails().get(0).getFieldName()).isEqualTo("email");
      assertThat(darin.getDetails().get(0).getFieldLabel()).isEqualTo("Email");
      assertThat(darin.getDetails().get(0).getDisplayValue()).isEqualTo("darin@example.invalid");

      ///////////////////////////////////////////////////////////
      // every word of the term is highlighted, ignoring case //
      ///////////////////////////////////////////////////////////
      RecordSearchHighlight highlight = RecordSearchAction.highlight(new QFieldMetaData("email", QFieldType.STRING), "darin@example.invalid", RecordSearchAction.termTokens("darin@ INVALID"));
      assertThat(highlighted(highlight)).isEqualTo("[darin@]example.[invalid]");
   }



   /*******************************************************************************
    ** Hidden fields, and fields personalization removes, are never highlighted
    ** or detailed.
    *******************************************************************************/
   @Test
   void testHighlightsAndDetailsOnlyUseFieldsTheSessionCanSee() throws QException
   {
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "email", DEFAULT_USER_ID);

      RecordSearchResult darin = search("darin").get(0);
      assertThat(darin.getHighlights()).isNull();
      assertThat(darin.getDetails()).isNull();
      assertThat(search("example")).isEmpty();

      ExamplePersonalizer.reset();
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("email").setIsHidden(true);
      darin = search("darin").get(0);
      assertThat(darin.getHighlights()).isNull();
      assertThat(darin.getDetails()).isNull();
   }



   /*******************************************************************************
    ** Long values are trimmed to a snippet around the first match, with
    ** ellipses, and match offsets into the snippet.
    *******************************************************************************/
   @Test
   void testHighlightSnippets()
   {
      QFieldMetaData field = new QFieldMetaData("notes", QFieldType.TEXT).withLabel("Notes");
      String         text  = "a".repeat(300) + " needle " + "b".repeat(300) + " Needle";

      RecordSearchHighlight highlight = RecordSearchAction.highlight(field, text, RecordSearchAction.termTokens(" NEEDLE  needle "));
      assertThat(highlight.getText()).startsWith(RecordSearchAction.ELLIPSIS).endsWith(RecordSearchAction.ELLIPSIS);
      assertThat(highlight.getText().length()).isEqualTo(RecordSearchAction.HIGHLIGHT_SNIPPET_LENGTH + 2);
      assertThat(highlight.getMatches()).hasSize(1);
      assertThat(highlight.getText().substring(highlight.getMatches().get(0).getStart(), highlight.getMatches().get(0).getEnd())).isEqualTo("needle");
      assertThat(highlight.getFieldName()).isEqualTo("notes");
      assertThat(highlight.getFieldLabel()).isEqualTo("Notes");

      ///////////////////////////////////////////////////////////
      // overlapping matches of different words merge into one //
      ///////////////////////////////////////////////////////////
      highlight = RecordSearchAction.highlight(field, "Kelkhoff", RecordSearchAction.termTokens("kel elk"));
      assertThat(highlighted(highlight)).isEqualTo("[Kelk]hoff");

      assertThat(RecordSearchAction.highlight(field, "nothing here", RecordSearchAction.termTokens("needle"))).isNull();
      assertThat(RecordSearchAction.highlight(field, null, RecordSearchAction.termTokens("needle"))).isNull();
   }



   /*******************************************************************************
    ** updatedAt comes from the MODIFY_DATE field (if visible); sort RECENT orders
    ** by it, most recent first.
    *******************************************************************************/
   @Test
   void testUpdatedAtAndSortRecent() throws QException
   {
      ///////////////////////////////////////////////////////////////////
      // modifyDate is the table's MODIFY_DATE field (by enrichment);  //
      // set distinct values without the behavior overwriting them     //
      ///////////////////////////////////////////////////////////////////
      assertThat(RecordSearchAction.modifyDateField(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).getName()).isEqualTo("modifyDate");
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
         new QRecord().withValue("id", 20).withValue("firstName", "Kelsey").withValue("lastName", "Old"),
         new QRecord().withValue("id", 21).withValue("firstName", "Kelsey").withValue("lastName", "New"))));
      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withOmitModifyDateUpdate(true).withRecords(List.of(
         new QRecord().withValue("id", 20).withValue("modifyDate", Instant.parse("2020-01-01T00:00:00Z")),
         new QRecord().withValue("id", 21).withValue("modifyDate", Instant.parse("2026-01-01T00:00:00Z")))));

      RecordSearchInput input = new RecordSearchInput().withSearchTerm("kelsey").withTableNames(List.of(TestUtils.TABLE_NAME_PERSON_MEMORY));
      assertThat(labels(search(input))).containsExactly("personMemory:20:Kelsey Old", "personMemory:21:Kelsey New");

      List<RecordSearchResult> recent = search(input.withSort(RecordSearchSort.RECENT));
      assertThat(labels(recent)).containsExactly("personMemory:21:Kelsey New", "personMemory:20:Kelsey Old");
      assertThat(recent.get(0).getUpdatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("modifyDate").setIsHidden(true);
      assertThat(search(input).get(0).getUpdatedAt()).isNull();
   }



   /*******************************************************************************
    ** offset pages through a table's matches; the summary says when there are more.
    *******************************************************************************/
   @Test
   void testOffsetHasMoreAndTableSummaries() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);

      List<QRecord> more = new ArrayList<>();
      for(int i = 10; i < 22; i++)
      {
         more.add(new QRecord().withValue("id", i).withValue("firstName", "Kelly").withValue("lastName", "Number " + i));
      }
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(more));

      //////////////////////////////////////////////////////////
      // 14 people match: Darin, Linda and 12 Kellys (10-21) //
      //////////////////////////////////////////////////////////
      RecordSearchOutput output = searchOutput(new RecordSearchInput().withSearchTerm("kel").withLimitPerTable(5));
      assertThat(output.getProviderStatus().getStatus()).isEqualTo(RecordSearchProviderState.NONE);
      assertThat(output.getTables()).hasSize(2);
      RecordSearchTableSummary people = output.getTables().get(0);
      assertThat(people.getTableName()).isEqualTo(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertThat(people.getTableLabel()).isEqualTo("Person");
      assertThat(people.getResultCount()).isEqualTo(5);
      assertThat(people.getHasMore()).isTrue();
      assertThat(people.getTotalCount()).isNull();
      assertThat(people.getSource()).isEqualTo(RecordSearchSource.DEFAULT);
      RecordSearchTableSummary orders = output.getTables().get(1);
      assertThat(orders.getResultCount()).isEqualTo(1);
      assertThat(orders.getHasMore()).isFalse();

      RecordSearchInput page3 = new RecordSearchInput().withSearchTerm("kel").withTableNames(List.of(TestUtils.TABLE_NAME_PERSON_MEMORY)).withLimitPerTable(5).withOffset(10);
      output = searchOutput(page3);
      assertThat(output.getResults()).extracting(RecordSearchResult::getRecordId).containsExactly(18, 19, 20, 21);
      assertThat(output.getTables().get(0).getHasMore()).isFalse();

      assertThat(searchOutput(page3.withOffset(-5)).getResults()).hasSize(5);
      assertThat(searchOutput(page3.withOffset(RecordSearchAction.MAX_OFFSET + 1)).getResults()).isEmpty();
   }



   /*******************************************************************************
    ** A provider's scores, totals and well-formed highlights (for fields the
    ** session can see) are kept; it gets the offset and sort.
    *******************************************************************************/
   @Test
   void testProviderScoresTotalsAndHighlights() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withSearchFields("firstName", "lastName", "email", "id", "ssn", "superSecret");
      useTestProvider();
      TestRecordSearchProvider.claim(TestUtils.TABLE_NAME_PERSON_MEMORY);
      TestRecordSearchProvider.total(TestUtils.TABLE_NAME_PERSON_MEMORY, 40);
      TestRecordSearchProvider.respondWith(
         new RecordSearchResult().withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecordId(3).withScore(9.5).withHighlights(List.of(
            new RecordSearchHighlight().withFieldName("email").withFieldLabel("ignored").withText("tim at example").withMatches(List.of(
               new RecordSearchHighlightMatch().withStart(0).withEnd(3))),
            new RecordSearchHighlight().withFieldName("superSecret").withText("secret").withMatches(List.of(new RecordSearchHighlightMatch().withStart(0).withEnd(6))),
            new RecordSearchHighlight().withFieldName("ssn").withText("123").withMatches(List.of(new RecordSearchHighlightMatch().withStart(0).withEnd(3))),
            new RecordSearchHighlight().withFieldName("notAField").withText("x").withMatches(List.of(new RecordSearchHighlightMatch().withStart(0).withEnd(1))))),
         new RecordSearchResult().withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecordId(1).withScore(2.0).withHighlights(List.of(
            new RecordSearchHighlight().withFieldName("email").withText("darin").withMatches(List.of(
               new RecordSearchHighlightMatch().withStart(2).withEnd(4), new RecordSearchHighlightMatch().withStart(1).withEnd(3))))));

      //////////////////////////////////////////////////////////////////////////
      // "chamberlain" is only in the label, so core highlights nothing itself //
      //////////////////////////////////////////////////////////////////////////
      RecordSearchOutput output = searchOutput(new RecordSearchInput().withSearchTerm("chamberlain").withLimitPerTable(5).withOffset(5).withSort(RecordSearchSort.RECENT));
      assertThat(output.getProviderStatus().getStatus()).isEqualTo(RecordSearchProviderState.OK);
      assertThat(output.getProviderStatus().getFailureClass()).isNull();

      RecordSearchTableSummary people = output.getTables().get(0);
      assertThat(people.getSource()).isEqualTo(RecordSearchSource.PROVIDER);
      assertThat(people.getTotalCount()).isEqualTo(40);
      assertThat(people.getHasMore()).isTrue();
      assertThat(output.getTables().get(1).getSource()).isEqualTo(RecordSearchSource.DEFAULT);

      RecordSearchResult tim = output.getResults().get(0);
      assertThat(tim.getRecordLabel()).isEqualTo("Tim Chamberlain");
      assertThat(tim.getScore()).isEqualTo(9.5);
      assertThat(tim.getHighlights()).hasSize(1);
      assertThat(tim.getHighlights().get(0).getFieldLabel()).isEqualTo("Email");
      assertThat(highlighted(tim.getHighlights().get(0))).isEqualTo("[tim] at example");

      ///////////////////////////////////////////////////////////////////////////
      // overlapping provider ranges are invalid - dropped; email is a detail //
      ///////////////////////////////////////////////////////////////////////////
      RecordSearchResult darin = output.getResults().get(1);
      assertThat(darin.getScore()).isEqualTo(2.0);
      assertThat(darin.getHighlights()).isNull();
      assertThat(darin.getDetails()).extracting(d -> d.getFieldName()).containsExactly("email");

      RecordSearchInput providerInput = TestRecordSearchProvider.getInputs().get(0);
      assertThat(providerInput.getOffset()).isEqualTo(5);
      assertThat(providerInput.getSort()).isEqualTo(RecordSearchSort.RECENT);
      assertThat(TestRecordSearchProvider.getInputs().get(0).getLimitPerTable()).isEqualTo(5);

      ////////////////////////////////////////////////////////////////////////////
      // a total within the page, and no more readable ids: no more; defaults //
      ////////////////////////////////////////////////////////////////////////////
      TestRecordSearchProvider.total(TestUtils.TABLE_NAME_PERSON_MEMORY, 2);
      output = searchOutput(new RecordSearchInput().withSearchTerm("tim"));
      assertThat(output.getTables().get(0).getHasMore()).isFalse();
      assertThat(TestRecordSearchProvider.getInputs().get(1).getOffset()).isEqualTo(0);
      assertThat(TestRecordSearchProvider.getInputs().get(1).getSort()).isEqualTo(RecordSearchSort.RELEVANCE);

      //////////////////////////////////////////////////////////////////////
      // more readable ids than the limit: more, even without a total     //
      //////////////////////////////////////////////////////////////////////
      TestRecordSearchProvider.total(TestUtils.TABLE_NAME_PERSON_MEMORY, null);
      output = searchOutput(new RecordSearchInput().withSearchTerm("tim").withLimitPerTable(1));
      assertThat(output.getTables().get(0).getResultCount()).isEqualTo(1);
      assertThat(output.getTables().get(0).getHasMore()).isTrue();
   }



   /*******************************************************************************
    ** Well-formed provider highlights are copied; malformed ones are dropped.
    *******************************************************************************/
   @Test
   void testValidProviderHighlight()
   {
      QFieldMetaData field = new QFieldMetaData("email", QFieldType.STRING).withLabel("Email");
      RecordSearchHighlight valid = RecordSearchAction.validProviderHighlight(field, new RecordSearchHighlight().withFieldName("email").withFieldLabel("x").withText("abcdef")
         .withMatches(List.of(new RecordSearchHighlightMatch().withStart(0).withEnd(2), new RecordSearchHighlightMatch().withStart(2).withEnd(6))));
      assertThat(valid.getFieldLabel()).isEqualTo("Email");
      assertThat(highlighted(valid)).isEqualTo("[ab][cdef]");

      for(List<RecordSearchHighlightMatch> invalid : List.of(
         List.<RecordSearchHighlightMatch>of(),
         List.of(new RecordSearchHighlightMatch().withStart(0).withEnd(7)),
         List.of(new RecordSearchHighlightMatch().withStart(-1).withEnd(2)),
         List.of(new RecordSearchHighlightMatch().withStart(3).withEnd(3)),
         List.of(new RecordSearchHighlightMatch().withStart(null).withEnd(3)),
         List.of(new RecordSearchHighlightMatch().withStart(3).withEnd(4), new RecordSearchHighlightMatch().withStart(0).withEnd(1))))
      {
         assertThat(RecordSearchAction.validProviderHighlight(field, new RecordSearchHighlight().withText("abcdef").withMatches(invalid))).isNull();
      }
      assertThat(RecordSearchAction.validProviderHighlight(field, new RecordSearchHighlight().withText(" ").withMatches(List.of(new RecordSearchHighlightMatch().withStart(0).withEnd(1))))).isNull();
      assertThat(RecordSearchAction.validProviderHighlight(field, new RecordSearchHighlight().withText("x".repeat(1001)).withMatches(List.of(new RecordSearchHighlightMatch().withStart(0).withEnd(1))))).isNull();
   }



   /*******************************************************************************
    ** When the provider fails, the output says so (the root cause's class, and
    ** a message only for user-facing errors), and its tables are FALLBACK.
    *******************************************************************************/
   @Test
   void testProviderFailureIsReported() throws QException
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 1);
      useTestProvider();
      TestRecordSearchProvider.claim(TestUtils.TABLE_NAME_PERSON_MEMORY);

      TestRecordSearchProvider.failWith(new QException("Quick Search record search failed: Connect to search.internal:9200", new java.net.ConnectException("Connection refused\nmore detail")));
      RecordSearchOutput output = searchOutput(new RecordSearchInput().withSearchTerm("kel"));
      assertThat(output.getProviderStatus().getStatus()).isEqualTo(RecordSearchProviderState.FAILED);
      assertThat(output.getProviderStatus().getFailureClass()).isEqualTo("ConnectException");
      assertThat(output.getProviderStatus().getMessage()).isNull();
      assertThat(output.getTables()).extracting(RecordSearchTableSummary::getSource).containsExactly(RecordSearchSource.FALLBACK, RecordSearchSource.DEFAULT);
      assertThat(labels(output.getResults())).containsExactly("personMemory:1:Darin Kelkhoff", "personMemory:2:Linda KELKHOFF", "order:1:Order KEL-1");

      TestRecordSearchProvider.failWith(new QUserFacingException("The search index is rebuilding.\nTry again soon."));
      output = searchOutput(new RecordSearchInput().withSearchTerm("kel"));
      assertThat(output.getProviderStatus().getFailureClass()).isEqualTo("QUserFacingException");
      assertThat(output.getProviderStatus().getMessage()).isEqualTo("The search index is rebuilding.");

      TestRecordSearchProvider.failWith(null);
      TestRecordSearchProvider.fail(false, true);
      output = searchOutput(new RecordSearchInput().withSearchTerm("kel"));
      assertThat(output.getProviderStatus().getStatus()).isEqualTo(RecordSearchProviderState.FAILED);
      assertThat(output.getProviderStatus().getFailureClass()).isEqualTo("IllegalStateException");

      //////////////////////////////////////////////////////////////////
      // a provider that claims no searched table: NONE, all DEFAULT //
      //////////////////////////////////////////////////////////////////
      TestRecordSearchProvider.fail(false, false);
      output = searchOutput(new RecordSearchInput().withSearchTerm("kel").withTableNames(List.of(TestUtils.TABLE_NAME_ORDER)));
      assertThat(output.getProviderStatus().getStatus()).isEqualTo(RecordSearchProviderState.NONE);
      assertThat(output.getTables()).extracting(RecordSearchTableSummary::getSource).containsExactly(RecordSearchSource.DEFAULT);
   }



   /*******************************************************************************
    ** A record whose label fields are empty is labeled by its primary key - by
    ** the default search and for provider results - never with a blank label
    ** (which the JSON response would leave out).
    *******************************************************************************/
   @Test
   void testBlankRecordLabelFallsBackToPrimaryKey() throws QException
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withSearchFields("firstName", "lastName", "email", "id");
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(List.of(
         new QRecord().withValue("id", 4).withValue("email", "unnamed@example.invalid"))));

      assertThat(labels(search("unnamed"))).containsExactly("personMemory:4:4");

      useTestProvider();
      TestRecordSearchProvider.claim(TestUtils.TABLE_NAME_PERSON_MEMORY);
      TestRecordSearchProvider.respondWith(TestUtils.TABLE_NAME_PERSON_MEMORY, 4);
      assertThat(labels(search("unnamed"))).containsExactly("personMemory:4:4");
   }

}
