/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
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

}
