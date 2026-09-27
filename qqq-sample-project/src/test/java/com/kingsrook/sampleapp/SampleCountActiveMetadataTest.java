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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native Count must use active metadata without changing its row/distinct count
 ** semantics or publishing fields restored only for native joins and READ locks.
 *******************************************************************************/
class SampleCountActiveMetadataTest
{
   private QInstance instance;
   private Map<String, List<List<String>>> before;



   /*******************************************************************************
    ** The existing Person/Pet fan-out supplies distinct INNER and LEFT oracles.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      before = snapshot();
   }



   /*******************************************************************************
    ** Every canonical row and column remains unchanged after reads and refusals.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         assertEquals(before, snapshot());
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** A USER-only child READ lock belongs in the aliased join's ON condition.
    ** LEFT retains all five parents while INNER returns only the permitted child.
    *******************************************************************************/
   @Test
   void testJoinedReadLocksUseActiveMetadataForInnerAndLeftAlias() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("countSpecies"));
      QContext.setQSession(new QSession().withSecurityKeyValue("countSpecies", 2));
      register(JoinedRead.class);
      assertAll(
         () -> assertNative(people(QueryJoin.Type.INNER, QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id AND a.species_id=2"),
         () -> assertNative(people(QueryJoin.Type.LEFT, QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2"),
         () -> assertNative(people(QueryJoin.Type.INNER, QInputSource.SYSTEM),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertNative(people(QueryJoin.Type.LEFT, QInputSource.SYSTEM),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id"));
   }



   /*******************************************************************************
    ** Existing days_worked values are unique nonnull INTEGERs. USER maps the
    ** logical Person primary key there, changing predicates and native join keys.
    *******************************************************************************/
   @Test
   void testActivePrimaryKeyMappingControlsBaseAndJoinedCount() throws Exception
   {
      register(PersonKeyMapping.class);
      CountInput user = input("person", QInputSource.USER);
      user.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, 5)));
      CountInput system = input("person", QInputSource.SYSTEM);
      system.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, 5)));
      assertAll(
         () ->
         {
            assertNative(user, "SELECT COUNT(*),COUNT(DISTINCT days_worked) FROM person WHERE days_worked>5");
            assertEquals("days_worked", user.getTable().getField("id").getBackendName());
         },
         () -> assertNative(system, "SELECT COUNT(*),COUNT(DISTINCT id) FROM person WHERE id>5"),
         () -> assertNative(pets(QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT a.id) FROM pet a INNER JOIN person p ON a.person_id=p.days_worked"),
         () -> assertNative(pets(QInputSource.SYSTEM),
            "SELECT COUNT(*),COUNT(DISTINCT a.id) FROM pet a INNER JOIN person p ON a.person_id=p.id"));
   }



   /*******************************************************************************
    ** Canonical READ and JoinOn fields removed from USER metadata remain private
    ** operational fields. Their use in explicit USER filters still rejects.
    *******************************************************************************/
   @Test
   void testRemovedReadAndJoinFieldsRemainOperationalOnly() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("countWork"));
      instance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withFieldName("daysWorked").withSecurityKeyType("countWork")
         .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      QContext.setQSession(new QSession().withSecurityKeyValue("countWork", 1001));
      register(RemoveStructuralFields.class);
      CountInput root = input("person", QInputSource.USER);
      CountInput removedRead = input("person", QInputSource.USER);
      removedRead.setFilter(new QQueryFilter(new QFilterCriteria("daysWorked", QCriteriaOperator.EQUALS, 1001)));
      CountInput removedJoin = people(QueryJoin.Type.INNER, QInputSource.USER);
      removedJoin.setFilter(new QQueryFilter(new QFilterCriteria("animal.personId", QCriteriaOperator.EQUALS, 1)));
      CountInput system = people(QueryJoin.Type.INNER, QInputSource.SYSTEM);
      system.setFilter(new QQueryFilter(new QFilterCriteria("animal.personId", QCriteriaOperator.EQUALS, 1)));
      assertAll(
         () ->
         {
            assertNative(root, "SELECT COUNT(*),COUNT(DISTINCT id) FROM person WHERE days_worked=1001");
            assertFalse(root.getTable().getFields().containsKey("daysWorked"));
         },
         () -> assertNative(people(QueryJoin.Type.INNER, QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id WHERE p.days_worked=1001"),
         () -> assertRefused(removedRead),
         () -> assertRefused(removedJoin),
         () -> assertNative(system,
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id WHERE p.days_worked=1001 AND a.person_id=1"));
   }



   /*******************************************************************************
    ** Distinct count needs its primary key internally even when USER cannot select
    ** that field. The input table remains trimmed after native execution.
    *******************************************************************************/
   @Test
   void testRemovedPrimaryKeyRemainsPrivateForDistinctCount() throws Exception
   {
      register(RemovePersonKey.class);
      CountInput root = people(QueryJoin.Type.INNER, QInputSource.USER);
      CountInput filter = input("person", QInputSource.USER);
      filter.setFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, 0)));
      assertAll(
         () ->
         {
            assertNative(root, "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id");
            assertFalse(root.getTable().getFields().containsKey("id"));
         },
         () -> assertNative(pets(QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT a.id) FROM pet a INNER JOIN person p ON a.person_id=p.id"),
         () -> assertRefused(filter),
         () -> assertNative(input("person", QInputSource.SYSTEM), "SELECT COUNT(*),COUNT(DISTINCT id) FROM person"));
   }



   /*******************************************************************************
    ** Returning no USER table must refuse deliberately, both at the root and on
    ** an explicit join. The available base and fresh SYSTEM controls still work.
    *******************************************************************************/
   @Test
   void testRemovedRootAndJoinedTablesRefuse() throws Exception
   {
      register(RemovePersonTable.class);
      assertAll(
         () -> assertRefused(input("person", QInputSource.USER)),
         () -> assertRefused(pets(QInputSource.USER)),
         () -> assertNative(input("pet", QInputSource.USER), "SELECT COUNT(*),COUNT(DISTINCT id) FROM pet"),
         () -> assertNative(input("person", QInputSource.SYSTEM), "SELECT COUNT(*),COUNT(DISTINCT id) FROM person"),
         () -> assertNative(pets(QInputSource.SYSTEM),
            "SELECT COUNT(*),COUNT(DISTINCT a.id) FROM pet a INNER JOIN person p ON a.person_id=p.id"));
   }



   /*******************************************************************************
    ** Compare both independent native cardinalities, not only a success status.
    *******************************************************************************/
   private void assertNative(CountInput input, String query) throws Exception
   {
      String metadataBefore = metadata();
      String filterBefore = JsonUtils.toJson(input.getFilter());
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      QInputSource source = (QInputSource) input.getInputSource();
      List<Integer> expected;
      try(Connection connection = connection();
          Statement statement = connection.createStatement();
          ResultSet result = statement.executeQuery(query))
      {
         assertTrue(result.next());
         expected = List.of(result.getInt(1), result.getInt(2));
         assertFalse(result.next());
      }
      CountOutput output = new CountAction().execute(input);
      assertEquals(expected, List.of(output.getCount(), output.getDistinctCount()));
      assertEquals(source, input.getInputSource());
      assertEquals(filterBefore, JsonUtils.toJson(input.getFilter()));
      assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins()));
      assertEquals(metadataBefore, metadata());
   }



   /*******************************************************************************
    ** Missing metadata cannot succeed or hide behind a native SQL/null-pointer error.
    *******************************************************************************/
   private void assertRefused(CountInput input) throws Exception
   {
      String metadataBefore = metadata();
      String filterBefore = JsonUtils.toJson(input.getFilter());
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      QException failure = assertThrows(QException.class, () -> new CountAction().execute(input));
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         assertFalse(cause instanceof SQLException, "Native SQL is not a metadata refusal");
         assertFalse(cause instanceof NullPointerException, "Missing metadata must refuse deliberately");
      }
      assertEquals(filterBefore, JsonUtils.toJson(input.getFilter()));
      assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins()));
      assertEquals(metadataBefore, metadata());
   }



   /*******************************************************************************
    ** Fresh typed inputs avoid inherited fluent return types and cross-call state.
    *******************************************************************************/
   private CountInput input(String tableName, QInputSource source)
   {
      CountInput input = new CountInput(tableName);
      input.setInputSource(source);
      input.setIncludeDistinctCount(true);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CountInput people(QueryJoin.Type type, QInputSource source)
   {
      CountInput input = input("person", source);
      input.setQueryJoins(List.of(new QueryJoin("pet").withAlias("animal").withSelect(false).withType(type)));
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CountInput pets(QInputSource source)
   {
      CountInput input = input("pet", source);
      input.setQueryJoins(List.of(new QueryJoin("person").withAlias("owner").withSelect(false).withType(QueryJoin.Type.INNER)));
      return input;
   }



   /*******************************************************************************
    ** Only the supported metadata personalizer controls USER variants.
    *******************************************************************************/
   private void register(Class<? extends TableMetaDataPersonalizerInterface> customizer)
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(customizer));
   }



   /*******************************************************************************
    ** Every canonical table and registered join remains unchanged by execution.
    *******************************************************************************/
   private String metadata()
   {
      return JsonUtils.toJson(List.of(instance.getTable("person"), instance.getTable("pet"), instance.getTable("petNote"),
         instance.getTable("fieldLab"), instance.getJoins()));
   }



   /*******************************************************************************
    ** All columns are compared; this fixture introduces no writes or new schema.
    *******************************************************************************/
   private Map<String, List<List<String>>> snapshot() throws Exception
   {
      Map<String, List<List<String>>> tables = new LinkedHashMap<>();
      try(Connection connection = connection();
          Statement statement = connection.createStatement())
      {
         for(String table : List.of("person", "pet", "pet_note", "field_lab"))
         {
            List<List<String>> rows = new ArrayList<>();
            try(ResultSet result = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id"))
            {
               while(result.next())
               {
                  List<String> row = new ArrayList<>();
                  for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
                  {
                     row.add(result.getString(column));
                  }
                  rows.add(row);
               }
            }
            tables.put(table, rows);
         }
      }
      return tables;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws SQLException
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class JoinedRead implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !"pet".equals(input.getTableName()))
         {
            return input.getTable();
         }
         return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withFieldName("speciesId").withSecurityKeyType("countSpecies")
            .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonKeyMapping implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !"person".equals(input.getTableName()))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getField("id").setBackendName("days_worked");
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemoveStructuralFields implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER)
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         if("person".equals(table.getName()))
         {
            table.getFields().remove("daysWorked");
         }
         if("pet".equals(table.getName()))
         {
            table.getFields().remove("personId");
         }
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemovePersonKey implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !"person".equals(input.getTableName()))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getFields().remove("id");
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemovePersonTable implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         return input.getInputSource() == QInputSource.USER && "person".equals(input.getTableName()) ? null : input.getTable();
      }
   }
}
