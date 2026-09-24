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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native Memory Aggregate joins over typed copies of canonical Person/Pet rows.
 ** H2 SQL is the independent oracle; H2 Aggregate is an additional control only.
 *******************************************************************************/
class SampleAggregateMemoryJoinContractTest
{
   private static final String MEMORY_BACKEND = "aggregateMemoryJoin";
   private QInstance nativeInstance;
   private QInstance memoryInstance;
   private QSession session;
   private Map<String, List<List<String>>> nativeBefore;
   private Map<String, List<Map<String, String>>> memoryBefore;
   private String metadataBefore;
   private Map<QTableMetaData, Map<String, QVirtualFieldMetaData>> virtualFieldsBefore = new IdentityHashMap<>();



   /*******************************************************************************
    ** Keep canonical table and registered join names in both owned instances.
    ** Only Person/Pet's backend changes in the Memory metadata variant.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      MemoryRecordStore.fullReset();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      nativeInstance = SampleMetaDataProvider.defineTestInstance();
      memoryInstance = SampleMetaDataProvider.defineTestInstance();
      memoryInstance.addBackend(new QBackendMetaData().withName(MEMORY_BACKEND).withBackendType(MemoryBackendModule.class));
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(memoryInstance.getTables());
      for(String tableName : List.of("person", "pet"))
      {
         tables.put(tableName, tables.get(tableName).clone().withBackendName(MEMORY_BACKEND));
      }
      memoryInstance.setTables(tables);
      for(QInstance instance : List.of(nativeInstance, memoryInstance))
      {
         instance.addSecurityKeyType(new QSecurityKeyType().withName("memoryJoinSpecies"));
      }
      session = new QSession().withSecurityKeyValue("memoryJoinSpecies", 2);
      // QContext validates and enriches each instance before preservation snapshots.
      QContext.init(nativeInstance, session);
      QContext.init(memoryInstance, session);
      Map<String, List<QRecord>> seeds = new LinkedHashMap<>();
      seeds.put("person", new ArrayList<>());
      seeds.put("pet", new ArrayList<>());
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         try(ResultSet rows = statement.executeQuery("SELECT id,first_name,last_name,days_worked FROM person ORDER BY id"))
         {
            while(rows.next())
            {
               seeds.get("person").add(new QRecord().withValue("id", rows.getInt("id"))
                  .withValue("firstName", rows.getString("first_name")).withValue("lastName", rows.getString("last_name"))
                  .withValue("daysWorked", rows.getInt("days_worked")));
            }
         }
         try(ResultSet rows = statement.executeQuery("SELECT id,name,person_id,species_id FROM pet ORDER BY id"))
         {
            while(rows.next())
            {
               seeds.get("pet").add(new QRecord().withValue("id", rows.getInt("id"))
                  .withValue("name", rows.getString("name")).withValue("personId", rows.getInt("person_id"))
                  .withValue("speciesId", rows.getInt("species_id")));
            }
         }
      }
      assertEquals(5, seeds.get("person").size());
      assertEquals(6, seeds.get("pet").size());
      for(Map.Entry<String, List<QRecord>> seed : seeds.entrySet())
      {
         List<QRecord> inserted = MemoryRecordStore.getInstance().insert(new InsertInput(seed.getKey()).withRecords(seed.getValue()), true);
         assertEquals(seed.getValue().size(), inserted.size());
         assertTrue(inserted.stream().allMatch(record -> record.getErrors().isEmpty()));
      }
      memoryBefore = memoryRows();
      assertSeed("person", List.of("id", "firstName", "lastName", "daysWorked"), "SELECT id,first_name,last_name,days_worked FROM person ORDER BY id");
      assertSeed("pet", List.of("id", "name", "personId", "speciesId"), "SELECT id,name,person_id,species_id FROM pet ORDER BY id");
      nativeBefore = nativeRows();
      metadataBefore = metadata();
   }



   /*******************************************************************************
    ** Check all stored native columns, all seeded Memory values, and both metadata
    ** graphs after success/refusal. No shared build/cache state is needed.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         assertAll(() -> assertEquals(nativeBefore, nativeRows()), () -> assertEquals(memoryBefore, memoryRows()),
            () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"));
      }
      finally
      {
         MemoryRecordStore.fullReset();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Alias-qualified child values and LEFT null extension must survive the source
    ** query. Reverse orientation uses the same registered Person/Pet join.
    *******************************************************************************/
   @Test
   void testUnrestrictedInnerLeftAndReverseAliasesMatchNativeSql() throws Exception
   {
      assertTrue(nativeInstance.getTable("pet").getFields().containsKey("speciesId"));
      assertFalse(nativeInstance.getTable("carrier").getFields().containsKey("speciesId"));
      assertEquals(List.of(List.of("11")), sql("SELECT COUNT(*) FROM carrier"));
      assertAll(
         () -> assertBoth(() -> people(QueryJoin.Type.INNER, QInputSource.SYSTEM),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertBoth(() -> people(QueryJoin.Type.LEFT, QInputSource.SYSTEM),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id"),
         () -> assertBoth(() -> input("pet", QInputSource.SYSTEM, count("id"), count("owner.id"), new Aggregate("id", AggregateOperator.COUNT_DISTINCT))
            .withQueryJoin(new QueryJoin("person").withAlias("owner").withSelect(false).withType(QueryJoin.Type.INNER)),
            "SELECT COUNT(a.id),COUNT(p.id),COUNT(DISTINCT a.id) FROM pet a INNER JOIN person p ON a.person_id=p.id"),
         () -> assertBoth(() -> input("person", QInputSource.SYSTEM, count("id"), count("carrier.id"), count("carrier.speciesId"))
            .withQueryJoin(new QueryJoin("pet").withAlias("carrier").withSelect(false).withType(QueryJoin.Type.INNER)),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(a.species_id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertQueryBoth(() -> new QueryInput("person").withInputSource(QInputSource.SYSTEM)
            .withQueryJoin(new QueryJoin("pet").withAlias("carrier").withSelect(true).withType(QueryJoin.Type.INNER))
            .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withOrderBy(new QFilterOrderBy("carrier.id"))),
            List.of("id", "firstName", "carrier.id", "carrier.name", "carrier.speciesId"),
            "SELECT p.id,p.first_name,a.id,a.name,a.species_id FROM person p INNER JOIN pet a ON p.id=a.person_id ORDER BY p.id,a.id"));
   }



   /*******************************************************************************
    ** A joined table may not borrow the root namespace. Distinct child aliases
    ** retain native INNER/LEFT membership and USER-specific child READ policy.
    *******************************************************************************/
   @Test
   void testRootTableNameCannotBeUsedAsJoinedAlias() throws Exception
   {
      register(ChildRead.class);
      List<Executable> checks = new ArrayList<>();
      for(QInputSource source : List.of(QInputSource.USER, QInputSource.SYSTEM))
      {
         for(QueryJoin.Type type : List.of(QueryJoin.Type.INNER, QueryJoin.Type.LEFT))
         {
            for(QInstance instance : List.of(nativeInstance, memoryInstance))
            {
               checks.add(() ->
               {
                  QContext.init(instance, session);
                  QueryInput input = new QueryInput("person").withInputSource(source)
                     .withQueryJoin(new QueryJoin("pet").withAlias("person").withSelect(true).withType(type))
                     .withFieldNamesToInclude(new LinkedHashSet<>(List.of("id", "firstName")))
                     .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
                  String filterBefore = JsonUtils.toJson(input.getFilter());
                  String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
                  String sessionBefore = JsonUtils.toJson(session);
                  assertAll((instance == memoryInstance ? "Memory" : "RDBMS") + " root alias / " + source + " / " + type,
                     () ->
                     {
                        QException failure = assertThrows(QException.class, () -> new QueryAction().execute(input));
                        boolean aliasReason = false;
                        for(Throwable cause = failure; cause != null; cause = cause.getCause())
                        {
                           assertFalse(cause instanceof SQLException, "Native SQL failure does not prove namespace validation");
                           assertFalse(cause instanceof NullPointerException, "Alias refusal must be deliberate");
                           String message = cause.getMessage();
                           aliasReason |= message != null && message.contains("alias") && message.contains("person");
                        }
                        assertTrue(aliasReason, "The refusal must identify the conflicting root alias");
                     },
                     () -> assertEquals(source, input.getInputSource()),
                     () -> assertEquals(filterBefore, JsonUtils.toJson(input.getFilter())),
                     () -> assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins())),
                     () -> assertEquals(new LinkedHashSet<>(List.of("id", "firstName")), input.getFieldNamesToInclude()),
                     () -> assertEquals(sessionBefore, JsonUtils.toJson(session)),
                     () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"));
               });
            }
            String readScope = source == QInputSource.USER ? " AND a.species_id=2" : "";
            checks.add(() -> assertQueryBoth(() -> publicPeople(type, source),
               List.of("id", "firstName", "animal.id", "animal.name"),
               "SELECT p.id,p.first_name,a.id,a.name FROM person p " + type + " JOIN pet a ON p.id=a.person_id"
                  + readScope + " ORDER BY p.id,a.id"));
         }
      }
      assertAll(checks);
   }



   /*******************************************************************************
    ** The USER-only child lock belongs in ON. INNER has one permitted child; LEFT
    ** keeps all parents and null-extends those whose children are all denied.
    *******************************************************************************/
   @Test
   void testUserChildReadPolicyAndSystemControls()
   {
      register(ChildRead.class);
      assertAll(
         () -> assertBoth(() -> people(QueryJoin.Type.INNER, QInputSource.USER),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id AND a.species_id=2"),
         () -> assertBoth(() -> people(QueryJoin.Type.LEFT, QInputSource.USER),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2"),
         () -> assertBoth(() -> people(QueryJoin.Type.INNER, QInputSource.SYSTEM),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertBoth(() -> people(QueryJoin.Type.LEFT, QInputSource.SYSTEM),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id"));
   }



   /*******************************************************************************
    ** The caller OR remains separate from the child's READ policy. LEFT must keep
    ** the first parent with a null child; INNER must discard its denied children.
    *******************************************************************************/
   @Test
   void testEffectiveOrFilterCombinesWithChildReadPolicy()
   {
      register(ChildRead.class);
      assertAll(
         () -> assertBoth(() -> filteredPeople(QueryJoin.Type.INNER, QInputSource.USER),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id AND a.species_id=2 WHERE p.id=1 OR a.name='Mae'"),
         () -> assertBoth(() -> filteredPeople(QueryJoin.Type.LEFT, QInputSource.USER),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2 WHERE p.id=1 OR a.name='Mae'"),
         () -> assertBoth(() -> filteredPeople(QueryJoin.Type.LEFT, QInputSource.SYSTEM),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id WHERE p.id=1 OR a.name='Mae'"));
   }



   /*******************************************************************************
    ** Removing a child field denies each explicit selector, not the permitted
    ** base count. SYSTEM still sees the canonical child field.
    *******************************************************************************/
   @Test
   void testRemovedChildFieldsStayUnavailableToUser()
   {
      register(RemoveChildName.class);
      Aggregate name = new Aggregate("animal.name", AggregateOperator.MIN);
      assertAll(
         () -> assertBoth(() -> baseCountJoin(QInputSource.USER), "SELECT COUNT(p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertRefusedBoth(() -> input("person", QInputSource.USER, name).withQueryJoin(childJoin(QueryJoin.Type.INNER))),
         () -> assertRefusedBoth(() -> baseCountJoin(QInputSource.USER).withGroupBy(new GroupBy(QFieldType.STRING, "animal.name"))),
         () -> assertRefusedBoth(() -> baseCountJoin(QInputSource.USER).withFilter(new QQueryFilter(new QFilterCriteria("animal.name", QCriteriaOperator.EQUALS, "Mae")))),
         () -> assertRefusedBoth(() -> baseCountJoin(QInputSource.USER).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(name)))),
         () -> assertBoth(() -> input("person", QInputSource.SYSTEM, name).withQueryJoin(childJoin(QueryJoin.Type.INNER)),
            "SELECT MIN(a.name) FROM person p INNER JOIN pet a ON p.id=a.person_id"));
   }



   /*******************************************************************************
    ** The active child retains a READ lock after its owner and join fields are
    ** removed from the public metadata. Private structural fields do not become
    ** legal caller operands; native parent/child membership remains exact.
    *******************************************************************************/
   @Test
   void testPrivateChildReadAndJoinKeysRemainOperational()
   {
      register(RemoveStructuralKeys.class);
      assertAll(
         () -> assertBoth(() -> people(QueryJoin.Type.INNER, QInputSource.USER),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id AND a.species_id=2"),
         () -> assertBoth(() -> people(QueryJoin.Type.LEFT, QInputSource.USER),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2"),
         () -> assertRefusedBoth(() -> baseCountJoin(QInputSource.USER).withFilter(new QQueryFilter(new QFilterCriteria("animal.speciesId", QCriteriaOperator.EQUALS, 2)))),
         () -> assertRefusedBoth(() -> baseCountJoin(QInputSource.USER).withGroupBy(new GroupBy(QFieldType.INTEGER, "animal.personId"))),
         () -> assertBoth(() -> people(QueryJoin.Type.INNER, QInputSource.SYSTEM),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"));
   }



   /*******************************************************************************
    ** A removed child table is a domain refusal even when only a public parent
    ** count is selected. Ordinary base reads and fresh SYSTEM joins still work.
    *******************************************************************************/
   @Test
   void testRemovedChildTableRefusesAndIndependentControlsRecover()
   {
      register(RemoveChildTable.class);
      assertAll(
         () -> assertRefusedBoth(() -> baseCountJoin(QInputSource.USER)),
         () -> assertBoth(() -> input("person", QInputSource.USER, count("id")), "SELECT COUNT(id) FROM person"),
         () -> assertBoth(() -> baseCountJoin(QInputSource.SYSTEM), "SELECT COUNT(p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"));
   }



   /*******************************************************************************
    ** Public Query must preserve selected aliases and explicit LEFT-null cells.
    ** H2 SQL, RDBMS Query and Memory Query are three independent paths.
    *******************************************************************************/
   @Test
   void testPublicQueryAliasesHonorUserReadAndSystem()
   {
      register(ChildRead.class);
      List<String> fields = List.of("id", "firstName", "animal.id", "animal.name");
      assertAll(
         () -> assertQueryBoth(() -> publicPeople(QueryJoin.Type.INNER, QInputSource.USER), fields,
            "SELECT p.id,p.first_name,a.id,a.name FROM person p INNER JOIN pet a ON p.id=a.person_id AND a.species_id=2 ORDER BY p.id,a.id"),
         () -> assertQueryBoth(() -> publicPeople(QueryJoin.Type.LEFT, QInputSource.USER), fields,
            "SELECT p.id,p.first_name,a.id,a.name FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2 ORDER BY p.id,a.id"),
         () -> assertQueryBoth(() -> publicPeople(QueryJoin.Type.INNER, QInputSource.SYSTEM), fields,
            "SELECT p.id,p.first_name,a.id,a.name FROM person p INNER JOIN pet a ON p.id=a.person_id ORDER BY p.id,a.id"),
         () -> assertQueryBoth(() -> publicPeople(QueryJoin.Type.LEFT, QInputSource.SYSTEM), fields,
            "SELECT p.id,p.first_name,a.id,a.name FROM person p LEFT JOIN pet a ON p.id=a.person_id ORDER BY p.id,a.id"),
         () -> assertQueryBoth(() -> blankChildQuery(QInputSource.USER), fields,
            "SELECT p.id,p.first_name,a.id,a.name FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2 WHERE a.id IS NULL ORDER BY p.id,a.id"),
         () -> assertQueryBoth(() -> blankChildQuery(QInputSource.SYSTEM), fields,
            "SELECT p.id,p.first_name,a.id,a.name FROM person p LEFT JOIN pet a ON p.id=a.person_id WHERE a.id IS NULL ORDER BY p.id,a.id"));
   }



   /*******************************************************************************
    ** Raw joined counts and distinct parent counts differ in the canonical fanout.
    ** USER source must reach the actual nested query used by native Memory Count.
    *******************************************************************************/
   @Test
   void testSharedCountReadSourceAndDistinct()
   {
      register(ChildRead.class);
      assertAll(
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.INNER, QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id AND a.species_id=2"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.LEFT, QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.INNER, QInputSource.SYSTEM),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.LEFT, QInputSource.SYSTEM),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.LEFT, QInputSource.USER).withFilter(
            new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
               .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))
               .withCriteria(new QFilterCriteria("animal.name", QCriteriaOperator.EQUALS, "Mae"))),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2 WHERE p.id=1 OR a.name='Mae'"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.LEFT, QInputSource.USER).withFilter(
            new QQueryFilter(new QFilterCriteria("animal.id", QCriteriaOperator.IS_BLANK))),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=2 WHERE a.id IS NULL"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.LEFT, QInputSource.SYSTEM).withFilter(
            new QQueryFilter(new QFilterCriteria("animal.id", QCriteriaOperator.IS_BLANK))),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id WHERE a.id IS NULL"));
   }



   /*******************************************************************************
    ** Removed child tables refuse both public Query and Count. A removed root key
    ** remains private while native distinct count uses its structural identity.
    *******************************************************************************/
   @Test
   void testSharedRemovedMetadataRefusalAndPrivateCountIdentity()
   {
      register(SharedRemovedMetadata.class);
      assertAll(
         () -> assertSharedRefused(nativeInstance, true),
         () -> assertSharedRefused(memoryInstance, true),
         () -> assertSharedRefused(nativeInstance, false),
         () -> assertSharedRefused(memoryInstance, false),
         () -> assertUnavailableQuery(nativeInstance, false),
         () -> assertUnavailableQuery(memoryInstance, false),
         () -> assertUnavailableQuery(nativeInstance, true),
         () -> assertUnavailableQuery(memoryInstance, true),
         () -> assertQueryBoth(() -> new QueryInput("pet").withInputSource(QInputSource.SYSTEM)
            .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))), List.of("id", "name"),
            "SELECT id,name FROM pet ORDER BY id"),
         () -> assertQueryBoth(() -> publicPeople(QueryJoin.Type.INNER, QInputSource.SYSTEM),
            List.of("id", "firstName", "animal.id", "animal.name"),
            "SELECT p.id,p.first_name,a.id,a.name FROM person p INNER JOIN pet a ON p.id=a.person_id ORDER BY p.id,a.id"),
         () -> assertQueryBoth(() -> new QueryInput("person").withInputSource(QInputSource.USER)
            .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("firstName"))), List.of("firstName"),
            "SELECT first_name FROM person ORDER BY first_name"),
         () -> assertCountBoth(() ->
         {
            CountInput input = new CountInput("person");
            input.setInputSource(QInputSource.USER);
            input.setIncludeDistinctCount(true);
            return input;
         }, "SELECT COUNT(*),COUNT(DISTINCT id) FROM person"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.INNER, QInputSource.SYSTEM),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"));
   }



   /*******************************************************************************
    ** Public requested fields are ordinary INTEGER/text values; no masking or PVS
    ** result is compared to native raw values in these shared Query controls.
    *******************************************************************************/
   private void assertQueryBoth(Supplier<QueryInput> request, List<String> fields, String query) throws Exception
   {
      List<List<String>> expected = sql(query);
      assertAll(query,
         () -> assertAll("RDBMS Query", () -> assertQueryResult(nativeInstance, request, fields, expected)),
         () -> assertAll("Memory Query", () -> assertQueryResult(memoryInstance, request, fields, expected)));
   }



   /*******************************************************************************
    ** Exact public projection includes NULL values on unmatched selected joins.
    *******************************************************************************/
   private void assertQueryResult(QInstance instance, Supplier<QueryInput> request, List<String> fields, List<List<String>> expected) throws Exception
   {
      QContext.init(instance, session);
      QueryInput input = request.get();
      input.setFieldNamesToInclude(new LinkedHashSet<>(fields));
      String filterBefore = JsonUtils.toJson(input.getFilter());
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      QInputSource source = (QInputSource) input.getInputSource();
      List<QRecord> records = new QueryAction().execute(input).getRecords();
      assertAll(
         () ->
         {
            List<List<String>> actual = new ArrayList<>();
            for(QRecord record : records)
            {
               assertEquals(new LinkedHashSet<>(fields), record.getValues().keySet());
               assertTrue(record.getDisplayValues().isEmpty());
               assertTrue(record.getAssociatedRecords().isEmpty());
               assertTrue(record.getErrors().isEmpty());
               List<String> row = new ArrayList<>();
               fields.forEach(field ->
               {
                  assertVirtualProjectionType(field, record.getValue(field));
                  row.add(value(record.getValue(field)));
               });
               actual.add(row);
            }
            assertEquals(expected, actual, instance == memoryInstance ? "Memory Query" : "RDBMS Query");
         },
         () -> assertEquals(source, input.getInputSource()),
         () -> assertEquals(filterBefore, JsonUtils.toJson(input.getFilter())),
         () -> assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins())),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"));
   }



   /*******************************************************************************
    ** Both Count adapters are checked against native row and distinct identities.
    *******************************************************************************/
   private void assertCountBoth(Supplier<CountInput> request, String query) throws Exception
   {
      List<List<String>> expected = sql(query);
      assertAll(query,
         () -> assertAll("RDBMS Count", () -> assertCountResult(nativeInstance, request, expected)),
         () -> assertAll("Memory Count", () -> assertCountResult(memoryInstance, request, expected)));
   }



   /*******************************************************************************
    ** The public active root table remains trimmed after private Count execution.
    *******************************************************************************/
   private void assertCountResult(QInstance instance, Supplier<CountInput> request, List<List<String>> expected) throws Exception
   {
      QContext.init(instance, session);
      CountInput input = request.get();
      String filterBefore = JsonUtils.toJson(input.getFilter());
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      QInputSource source = (QInputSource) input.getInputSource();
      CountOutput output = new CountAction().execute(input);
      List<List<String>> actual = List.of(List.of(String.valueOf(output.getCount()), String.valueOf(output.getDistinctCount())));
      assertAll(
         () -> assertEquals(expected, actual, instance == memoryInstance ? "Memory Count" : "RDBMS Count"),
         () -> assertEquals(source, input.getInputSource()),
         () -> assertEquals(filterBefore, JsonUtils.toJson(input.getFilter())),
         () -> assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins())),
         () ->
         {
            if(source == QInputSource.USER && input.getQueryJoins() == null)
            {
               assertFalse(input.getTable().getFields().containsKey("id"), "Private distinct identity must not restore public metadata");
            }
         },
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"));
   }



   /*******************************************************************************
    ** No selected child field is needed to demand a checked removed-table refusal.
    *******************************************************************************/
   private void assertSharedRefused(QInstance instance, boolean query)
   {
      QContext.init(instance, session);
      QueryInput queryInput = new QueryInput("person").withInputSource(QInputSource.USER)
         .withFieldNamesToInclude(new LinkedHashSet<>(List.of("firstName")))
         .withQueryJoin(childJoin(QueryJoin.Type.INNER));
      CountInput countInput = countPeople(QueryJoin.Type.INNER, QInputSource.USER);
      String queryJoinsBefore = JsonUtils.toJson(queryInput.getQueryJoins());
      String countJoinsBefore = JsonUtils.toJson(countInput.getQueryJoins());
      QException failure = assertThrows(QException.class, () ->
      {
         if(query)
         {
            new QueryAction().execute(queryInput);
         }
         else
         {
            new CountAction().execute(countInput);
         }
      });
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         assertFalse(cause instanceof SQLException, "SQL failure is not a metadata refusal");
         assertFalse(cause instanceof NullPointerException, "Metadata refusal must be deliberate");
      }
      assertEquals(queryJoinsBefore, JsonUtils.toJson(queryInput.getQueryJoins()));
      assertEquals(countJoinsBefore, JsonUtils.toJson(countInput.getQueryJoins()));
      assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged");
   }



   /*******************************************************************************
    ** No explicit projection bypasses the unavailable selected-child SELECT path.
    ** The same existing personalizer also removes Pet as a root table for USER.
    *******************************************************************************/
   private void assertUnavailableQuery(QInstance instance, boolean selectedChild)
   {
      QContext.init(instance, session);
      QueryInput input = new QueryInput(selectedChild ? "person" : "pet").withInputSource(QInputSource.USER);
      if(selectedChild)
      {
         input.withQueryJoin(childJoin(QueryJoin.Type.INNER).withSelect(true));
      }
      assertTrue(input.getFieldNamesToInclude() == null, "The native default-selection path is the subject of this refusal");
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      assertDeliberateRefusal(() -> new QueryAction().execute(input));
      assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins()));
      assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged");
   }



   /*******************************************************************************
    ** Public Query joins are selected; Aggregate/Count joins above are not.
    *******************************************************************************/
   private QueryInput publicPeople(QueryJoin.Type type, QInputSource source)
   {
      return new QueryInput("person").withInputSource(source)
         .withQueryJoin(childJoin(type).withSelect(true))
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withOrderBy(new QFilterOrderBy("animal.id")));
   }



   /*******************************************************************************
    ** Filter the explicit NULL extension after child READ membership is resolved.
    *******************************************************************************/
   private QueryInput blankChildQuery(QInputSource source)
   {
      QueryInput input = publicPeople(QueryJoin.Type.LEFT, source);
      input.getFilter().addCriteria(new QFilterCriteria("animal.id", QCriteriaOperator.IS_BLANK));
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CountInput countPeople(QueryJoin.Type type, QInputSource source)
   {
      CountInput input = new CountInput("person");
      input.setInputSource(source);
      input.setQueryJoins(List.of(childJoin(type)));
      input.setIncludeDistinctCount(true);
      return input;
   }



   /*******************************************************************************
    ** A NULL structural value has no equality-join membership, including itself.
    ** Four nonnull same-type keys join; the fifth parent is LEFT-null-extended.
    *******************************************************************************/
   @Test
   void testNullableSelfJoinHasNoNullMembership() throws Exception
   {
      prepareNullableSelfJoin();
      assertAll(
         () -> assertBoth(() -> selfAggregate(QueryJoin.Type.INNER),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN person a ON p.days_worked=a.days_worked"),
         () -> assertBoth(() -> selfAggregate(QueryJoin.Type.LEFT),
            "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN person a ON p.days_worked=a.days_worked"),
         () -> assertQueryBoth(() -> selfQuery(QueryJoin.Type.INNER), List.of("id", "firstName", "peer.id"),
            "SELECT p.id,p.first_name,a.id FROM person p INNER JOIN person a ON p.days_worked=a.days_worked ORDER BY p.id,a.id"),
         () -> assertQueryBoth(() -> selfQuery(QueryJoin.Type.LEFT), List.of("id", "firstName", "peer.id"),
            "SELECT p.id,p.first_name,a.id FROM person p LEFT JOIN person a ON p.days_worked=a.days_worked ORDER BY p.id,a.id"),
         () -> assertCountBoth(() -> selfCount(QueryJoin.Type.INNER),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN person a ON p.days_worked=a.days_worked"),
         () -> assertCountBoth(() -> selfCount(QueryJoin.Type.LEFT),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN person a ON p.days_worked=a.days_worked"));
   }



   /*******************************************************************************
    ** A non-selected join can constrain membership but may not publish its fields.
    ** No explicit field subset is supplied to accidentally conceal this defect.
    *******************************************************************************/
   @Test
   void testUnselectedNamedAndAliasedJoinsDoNotPublishPrivateChildren() throws Exception
   {
      register(UnselectedPrivateChild.class);
      List<List<String>> expected = sql("SELECT p.id,p.first_name,p.last_name,p.days_worked FROM person p INNER JOIN pet a ON p.id=a.person_id ORDER BY p.id");
      assertAll(
         () -> assertUnselectedQuery(nativeInstance, null, expected),
         () -> assertUnselectedQuery(memoryInstance, null, expected),
         () -> assertUnselectedQuery(nativeInstance, "animal", expected),
         () -> assertUnselectedQuery(memoryInstance, "animal", expected));
   }



   /*******************************************************************************
    ** The runtime fixture changes exactly one existing field in each owned store.
    ** Expected complete snapshots are derived from the old snapshot, not replaced
    ** with whatever a mutation happened to produce.
    *******************************************************************************/
   private void prepareNullableSelfJoin() throws Exception
   {
      Map<String, List<List<String>>> expectedNative = new LinkedHashMap<>(nativeBefore);
      List<List<String>> people = new ArrayList<>();
      for(List<String> row : nativeBefore.get("person"))
      {
         people.add(new ArrayList<>(row));
      }
      expectedNative.put("person", people);
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         try(ResultSet row = statement.executeQuery("SELECT * FROM person WHERE id=5"))
         {
            assertTrue(row.next());
            assertEquals(1, row.getInt("days_worked"));
            int idColumn = row.findColumn("id") - 1;
            int daysColumn = row.findColumn("days_worked") - 1;
            for(List<String> person : people)
            {
               if("5".equals(person.get(idColumn)))
               {
                  person.set(daysColumn, null);
               }
            }
            assertFalse(row.next());
         }
         assertEquals(1, statement.executeUpdate("UPDATE person SET days_worked=NULL WHERE id=5 AND days_worked=1"));
      }
      Map<String, List<Map<String, String>>> expectedMemory = new LinkedHashMap<>(memoryBefore);
      List<Map<String, String>> memoryPeople = new ArrayList<>();
      for(Map<String, String> row : memoryBefore.get("person"))
      {
         Map<String, String> copy = new LinkedHashMap<>(row);
         if("5".equals(copy.get("id")))
         {
            assertEquals("1", copy.put("daysWorked", null));
         }
         memoryPeople.add(copy);
      }
      expectedMemory.put("person", memoryPeople);
      QContext.init(memoryInstance, session);
      List<QRecord> updated = MemoryRecordStore.getInstance().update(new UpdateInput("person")
         .withRecord(new QRecord().withValue("id", 5).withValue("daysWorked", null)), true);
      assertEquals(1, updated.size());
      assertTrue(updated.get(0).getErrors().isEmpty());
      assertAll(() -> assertEquals(expectedNative, nativeRows()), () -> assertEquals(expectedMemory, memoryRows()));
      nativeBefore = expectedNative;
      memoryBefore = expectedMemory;
      assertSeed("person", List.of("id", "firstName", "lastName", "daysWorked"), "SELECT id,first_name,last_name,days_worked FROM person ORDER BY id");
      for(QInstance instance : List.of(nativeInstance, memoryInstance))
      {
         instance.addJoin(new QJoinMetaData().withName("personWorkedSelf").withLeftTable("person").withRightTable("person")
            .withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("daysWorked", "daysWorked")));
      }
      metadataBefore = metadata();
   }



   /*******************************************************************************
    ** Canonical Person metadata is privately reduced to the four identically seeded
    ** fields, so default projections can be compared without explicit select lists.
    *******************************************************************************/
   private void assertUnselectedQuery(QInstance instance, String alias, List<List<String>> expected) throws Exception
   {
      QContext.init(instance, session);
      QueryInput input = new QueryInput("person").withInputSource(QInputSource.USER)
         .withQueryJoin(new QueryJoin("pet").withAlias(alias).withSelect(false).withType(QueryJoin.Type.INNER))
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      assertTrue(input.getFieldNamesToInclude() == null, "The default projection is the subject of this control");
      List<String> fields = List.of("id", "firstName", "lastName", "daysWorked");
      List<QRecord> records = new QueryAction().execute(input).getRecords();
      List<List<String>> actual = new ArrayList<>();
      for(QRecord record : records)
      {
         List<String> row = new ArrayList<>();
         fields.forEach(field ->
         {
            assertVirtualProjectionType(field, record.getValue(field));
            row.add(value(record.getValue(field)));
         });
         actual.add(row);
      }
      String serialized = JsonUtils.toJson(records);
      assertAll(
         () ->
         {
            for(QRecord record : records)
            {
               assertEquals(new LinkedHashSet<>(fields), record.getValues().keySet());
               assertTrue(record.getDisplayValues().isEmpty());
               assertTrue(record.getAssociatedRecords().isEmpty());
               assertTrue(record.getErrors().isEmpty());
            }
         },
         () -> assertEquals(expected, actual, instance == memoryInstance ? "Memory unselected join" : "RDBMS unselected join"),
         () -> assertFalse(serialized.contains("pet."), "Unselected named fields must not be serialized"),
         () -> assertFalse(serialized.contains("animal."), "Unselected alias fields must not be serialized"),
         () ->
         {
            for(List<String> child : sql("SELECT name FROM pet ORDER BY id"))
            {
               assertFalse(serialized.contains(JsonUtils.toJson(child.get(0))), "Unselected child password values must not be serialized");
            }
         },
         () -> assertTrue(input.getFieldNamesToInclude() == null),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"));
   }



   /*******************************************************************************
    ** Explicit registered metadata avoids any ambiguous self-join graph inference.
    *******************************************************************************/
   private QueryJoin selfJoin(QueryJoin.Type type)
   {
      return new QueryJoin("person").withAlias("peer").withBaseTableOrAlias("person").withSelect(false).withType(type)
         .withJoinMetaData(QContext.getQInstance().getJoin("personWorkedSelf"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput selfAggregate(QueryJoin.Type type)
   {
      return input("person", QInputSource.SYSTEM, count("id"), count("peer.id"), new Aggregate("id", AggregateOperator.COUNT_DISTINCT))
         .withQueryJoin(selfJoin(type));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryInput selfQuery(QueryJoin.Type type)
   {
      return new QueryInput("person").withInputSource(QInputSource.SYSTEM).withQueryJoin(selfJoin(type).withSelect(true))
         .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withOrderBy(new QFilterOrderBy("peer.id")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CountInput selfCount(QueryJoin.Type type)
   {
      CountInput input = new CountInput("person");
      input.setInputSource(QInputSource.SYSTEM);
      input.setQueryJoins(List.of(selfJoin(type)));
      input.setIncludeDistinctCount(true);
      return input;
   }



   /*******************************************************************************
    ** Bypassing the security-expanded join plan is now a deliberately refused
    ** native mode. Always restore the singleton setting, including on a red run.
    *******************************************************************************/
   @Test
   void testLegacyMemoryJoinSwitchRefusesAndRestores() throws Exception
   {
      register(ChildRead.class);
      QContext.init(memoryInstance, session);
      MemoryRecordStore store = MemoryRecordStore.getInstance();
      boolean original = store.getBuildJoinCrossProductFromJoinContext();
      try
      {
         store.setBuildJoinCrossProductFromJoinContext(false);
         assertAll(
            () -> assertDeliberateRefusal(() -> new AggregateAction().execute(people(QueryJoin.Type.INNER, QInputSource.USER))),
            () -> assertDeliberateRefusal(() -> new QueryAction().execute(publicPeople(QueryJoin.Type.LEFT, QInputSource.USER))),
            () -> assertDeliberateRefusal(() -> new CountAction().execute(countPeople(QueryJoin.Type.INNER, QInputSource.USER))));
      }
      finally
      {
         store.setBuildJoinCrossProductFromJoinContext(original);
      }
      assertEquals(original, store.getBuildJoinCrossProductFromJoinContext());
      assertBoth(() -> people(QueryJoin.Type.INNER, QInputSource.USER),
         "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id AND a.species_id=2");
   }



   /*******************************************************************************
    ** Heavy STRING operands remain raw internally for Aggregate. Public Query
    ** returns lengths by default and values only with its explicit fetch option.
    ** The hidden INTEGER root key remains operational for distinct Count.
    *******************************************************************************/
   @Test
   void testHeavyPublicProjectionAndInternalAggregateCount() throws Exception
   {
      register(HeavySharedMetadata.class);
      assertAll(
         () -> assertHeavyQuery(nativeInstance, false),
         () -> assertHeavyQuery(memoryInstance, false),
         () -> assertHeavyQuery(nativeInstance, true),
         () -> assertHeavyQuery(memoryInstance, true),
         () -> assertBoth(() -> input("person", QInputSource.USER,
            new Aggregate("firstName", AggregateOperator.MIN), new Aggregate("animal.name", AggregateOperator.MAX), count("id"))
            .withQueryJoin(childJoin(QueryJoin.Type.INNER)),
            "SELECT MIN(p.first_name),MAX(a.name),COUNT(p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.INNER, QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertCountBoth(() -> countPeople(QueryJoin.Type.LEFT, QInputSource.USER),
            "SELECT COUNT(*),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id"));
   }



   /*******************************************************************************
    ** Metadata and unsupported-mode refusal are checked, not native SQL/NPE errors.
    *******************************************************************************/
   private void assertDeliberateRefusal(Executable action)
   {
      QException failure = assertThrows(QException.class, action);
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         assertFalse(cause instanceof SQLException, "SQL failure is not a deliberate mode refusal");
         assertFalse(cause instanceof NullPointerException, "Mode refusal must be deliberate");
      }
   }



   /*******************************************************************************
    ** Only already-seeded INTEGER/text values participate. Both base and selected
    ** alias heavy lengths are compared directly with native CHAR_LENGTH results.
    *******************************************************************************/
   private void assertHeavyQuery(QInstance instance, boolean fetchHeavy) throws Exception
   {
      QContext.init(instance, session);
      QueryInput input = publicPeople(QueryJoin.Type.INNER, QInputSource.USER).withShouldFetchHeavyFields(fetchHeavy)
         .withFieldNamesToInclude(new LinkedHashSet<>(List.of("id", "firstName", "lastName", "daysWorked", "animal.id", "animal.name")));
      List<String> fields = fetchHeavy ? List.of("firstName", "lastName", "daysWorked", "animal.id", "animal.name")
         : List.of("lastName", "daysWorked", "animal.id");
      String select = fetchHeavy ? "p.first_name,p.last_name,p.days_worked,a.id,a.name" : "p.last_name,p.days_worked,a.id";
      List<List<String>> expected = sql("SELECT " + select + " FROM person p INNER JOIN pet a ON p.id=a.person_id ORDER BY p.id,a.id");
      List<List<String>> lengths = sql("SELECT CHAR_LENGTH(p.first_name),CHAR_LENGTH(a.name) FROM person p INNER JOIN pet a ON p.id=a.person_id ORDER BY p.id,a.id");
      String filterBefore = JsonUtils.toJson(input.getFilter());
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      List<QRecord> records = new QueryAction().execute(input).getRecords();
      List<List<String>> actual = new ArrayList<>();
      for(QRecord record : records)
      {
         List<String> row = new ArrayList<>();
         fields.forEach(field ->
         {
            assertVirtualProjectionType(field, record.getValue(field));
            row.add(value(record.getValue(field)));
         });
         actual.add(row);
      }
      assertAll(
         () -> assertEquals(expected, actual, instance == memoryInstance ? "Memory heavy Query" : "RDBMS heavy Query"),
         () ->
         {
            assertEquals(lengths.size(), records.size());
            for(int index = 0; index < records.size(); index++)
            {
               QRecord record = records.get(index);
               assertEquals(new LinkedHashSet<>(fields), record.getValues().keySet());
               assertTrue(record.getDisplayValues().isEmpty());
               assertTrue(record.getAssociatedRecords().isEmpty());
               assertTrue(record.getErrors().isEmpty());
               if(!fetchHeavy)
               {
                  Map<?, ?> fieldLengths = assertInstanceOf(Map.class, record.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS));
                  assertEquals(new LinkedHashSet<>(List.of("firstName", "animal.name")), fieldLengths.keySet());
                  assertEquals(lengths.get(index).get(0), value(fieldLengths.get("firstName")));
                  assertEquals(lengths.get(index).get(1), value(fieldLengths.get("animal.name")));
               }
            }
         },
         () ->
         {
            if(!fetchHeavy)
            {
               String serialized = JsonUtils.toJson(records);
               for(List<String> row : sql("SELECT first_name FROM person UNION SELECT name FROM pet"))
               {
                  assertFalse(serialized.contains(JsonUtils.toJson(row.get(0))), "Heavy values must not be serialized by a light Query");
               }
            }
         },
         () -> assertTrue(input.getTable().getField("id").getIsHidden(), "Count/Query private identity metadata remains active"),
         () -> assertEquals(filterBefore, JsonUtils.toJson(input.getFilter())),
         () -> assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins())),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"));
   }



   /*******************************************************************************
    ** Physical output isolates evaluation of a base virtual predicate. Wrapping
    ** the same predicate in one AND subtree cannot change the native row set.
    *******************************************************************************/
   @Test
   void testBaseVirtualFlatAndNestedFilterMatchNativeSql() throws Exception
   {
      String originalMetadata = addVirtualLengths();
      try
      {
         String where = " FROM person WHERE CHAR_LENGTH(first_name)>4";
         assertEquals(List.of(List.of("1"), List.of("2"), List.of("3"), List.of("5")), sql("SELECT id" + where + " ORDER BY id"));
         List<Executable> cases = new ArrayList<>();
         for(boolean nested : List.of(false, true))
         {
            cases.add(() -> assertAll("Base virtual " + (nested ? "nested AND" : "flat"),
               () -> assertQueryBoth(() -> new QueryInput("person").withInputSource(QInputSource.SYSTEM)
               .withFilter(virtualFilter("firstNameLength", nested).withOrderBy(new QFilterOrderBy("id"))), List.of("id"), "SELECT id" + where + " ORDER BY id"),
               () -> assertCountBoth(() -> virtualCount(false, virtualFilter("firstNameLength", nested)), "SELECT COUNT(*),COUNT(DISTINCT id)" + where),
               () -> assertBoth(() -> input("person", QInputSource.SYSTEM, count("id")).withFilter(virtualFilter("firstNameLength", nested)), "SELECT COUNT(id)" + where)));
         }

         assertAll(cases);
      }
      finally
      {
         removeVirtualLengths(originalMetadata);
      }
   }



   /*******************************************************************************
    ** Pet.name differs from Person.firstName; a child-local function cannot use
    ** the root record. Flat/nested filters project only physical fields here.
    *******************************************************************************/
   @Test
   void testJoinedVirtualFlatAndNestedFilterMatchNativeSql() throws Exception
   {
      String originalMetadata = addVirtualLengths();
      try
      {
         String where = " FROM person p INNER JOIN pet a ON p.id=a.person_id WHERE CHAR_LENGTH(a.name)>4";
         assertEquals(List.of(List.of("1", "1", "Charlie"), List.of("1", "3", "Louie"), List.of("1", "4", "Barkley")),
            sql("SELECT p.id,a.id,a.name" + where + " ORDER BY p.id,a.id"));
         List<Executable> cases = new ArrayList<>();
         for(boolean nested : List.of(false, true))
         {
            cases.add(() -> assertAll("Joined virtual " + (nested ? "nested AND" : "flat"),
               () -> assertQueryBoth(() -> publicPeople(QueryJoin.Type.INNER, QInputSource.SYSTEM)
               .withFilter(virtualFilter("animal.nameLength", nested).withOrderBy(new QFilterOrderBy("id")).withOrderBy(new QFilterOrderBy("animal.id"))),
               List.of("id", "animal.id", "animal.name"), "SELECT p.id,a.id,a.name" + where + " ORDER BY p.id,a.id"),
               () -> assertCountBoth(() -> virtualCount(true, virtualFilter("animal.nameLength", nested)), "SELECT COUNT(*),COUNT(DISTINCT p.id)" + where),
               () -> assertBoth(() -> input("person", QInputSource.SYSTEM, count("id")).withQueryJoin(childJoin(QueryJoin.Type.INNER))
               .withFilter(virtualFilter("animal.nameLength", nested)), "SELECT COUNT(p.id)" + where)));
         }

         assertAll(cases);
      }
      finally
      {
         removeVirtualLengths(originalMetadata);
      }
   }



   /*******************************************************************************
    ** Physical selection and filtering isolate qualified virtual sort evaluation.
    ** Barkley must sort before the lower-id Coco; native id ordering is a decoy.
    *******************************************************************************/
   @Test
   void testJoinedVirtualSortUsesChildFieldFunction() throws Exception
   {
      String originalMetadata = addVirtualLengths();
      try
      {
         String query = "SELECT p.id,a.id,a.name FROM person p INNER JOIN pet a ON p.id=a.person_id WHERE a.id>1 ORDER BY CHAR_LENGTH(a.name) DESC,p.id,a.id";
         assertEquals(List.of(List.of("1", "4", "Barkley"), List.of("1", "3", "Louie"), List.of("1", "2", "Coco"), List.of("2", "5", "Toby"), List.of("3", "6", "Mae")), sql(query));
         assertQueryBoth(() -> publicPeople(QueryJoin.Type.INNER, QInputSource.SYSTEM)
            .withFilter(new QQueryFilter(new QFilterCriteria("animal.id", QCriteriaOperator.GREATER_THAN, 1))
               .withOrderBy(new QFilterOrderBy("animal.nameLength", false)).withOrderBy(new QFilterOrderBy("id")).withOrderBy(new QFilterOrderBy("animal.id"))),
            List.of("id", "animal.id", "animal.name"), query);
      }
      finally
      {
         removeVirtualLengths(originalMetadata);
      }
   }



   /*******************************************************************************
    ** Aggregate must receive materialized child virtual values even when its
    ** source join is not publicly selected. LEFT unmatched operands remain NULL.
    *******************************************************************************/
   @Test
   void testBaseAndJoinedVirtualAggregatesAndGroupsMatchNativeSql() throws Exception
   {
      String originalMetadata = addVirtualLengths();
      try
      {
         String joined = " FROM person p LEFT JOIN pet a ON p.id=a.person_id";
         assertEquals(List.of(List.of("6", "30", "3", "7")), sql("SELECT COUNT(CHAR_LENGTH(a.name)),SUM(CHAR_LENGTH(a.name)),MIN(CHAR_LENGTH(a.name)),MAX(CHAR_LENGTH(a.name))" + joined));
         assertAll(
            () -> assertBoth(() -> input("person", QInputSource.SYSTEM, count("firstNameLength"), new Aggregate("firstNameLength", AggregateOperator.SUM)),
               "SELECT COUNT(CHAR_LENGTH(first_name)),SUM(CHAR_LENGTH(first_name)) FROM person"),
            () -> assertRefusedBoth(() -> input("person", QInputSource.SYSTEM, count("person.firstNameLength"), new Aggregate("person.firstNameLength", AggregateOperator.SUM))),
            () -> assertBoth(() -> input("person", QInputSource.SYSTEM, count("animal.nameLength"), new Aggregate("animal.nameLength", AggregateOperator.SUM),
               new Aggregate("animal.nameLength", AggregateOperator.MIN), new Aggregate("animal.nameLength", AggregateOperator.MAX)).withQueryJoin(childJoin(QueryJoin.Type.LEFT)),
               "SELECT COUNT(CHAR_LENGTH(a.name)),SUM(CHAR_LENGTH(a.name)),MIN(CHAR_LENGTH(a.name)),MAX(CHAR_LENGTH(a.name))" + joined),
            () -> assertVirtualGroupsWithProviderNullOrder(true),
            () -> assertVirtualGroupsWithProviderNullOrder(false));
      }
      finally
      {
         removeVirtualLengths(originalMetadata);
      }
   }



   /*******************************************************************************
    ** Keep each provider's declared null rank, with exact native SQL oracles.
    ** Memory uses NULLS LAST ascending and NULLS FIRST descending; H2 uses its
    ** actual defaults. Returned rows are never reordered or normalized here.
    *******************************************************************************/
   private void assertVirtualGroupsWithProviderNullOrder(boolean ascending) throws Exception
   {
      String direction = ascending ? " ASC" : " DESC";
      String query = "SELECT CHAR_LENGTH(a.name),COUNT(p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id"
         + " GROUP BY CHAR_LENGTH(a.name) ORDER BY CHAR_LENGTH(a.name)" + direction;
      List<List<String>> nativeExpected = sql(query);
      List<List<String>> memoryExpected = sql(query + (ascending ? " NULLS LAST" : " NULLS FIRST"));
      Supplier<AggregateInput> request = () ->
      {
         GroupBy group = new GroupBy(QFieldType.INTEGER, "animal.nameLength");
         return input("person", QInputSource.SYSTEM, count("id")).withQueryJoin(childJoin(QueryJoin.Type.LEFT)).withGroupBy(group)
            .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(group, ascending)));
      };
      assertAll("Virtual groups" + direction,
         () -> assertAll("H2 default null order", () -> assertResult(nativeInstance, request, nativeExpected)),
         () -> assertAll("Memory explicit null order", () -> assertResult(memoryInstance, request, memoryExpected)));
   }



   /*******************************************************************************
    ** Both owned instances declare the same existing function on different local
    ** source fields. Preserve the original graphs after removing this variation.
    *******************************************************************************/
   private String addVirtualLengths()
   {
      String originalMetadata = metadataBefore;
      for(QInstance instance : List.of(nativeInstance, memoryInstance))
      {
         for(String tableName : List.of("person", "pet"))
         {
            QTableMetaData table = instance.getTable(tableName);
            String name = tableName.equals("person") ? "firstNameLength" : "nameLength";
            String source = tableName.equals("person") ? "firstName" : "name";
            Map<String, QVirtualFieldMetaData> original = table.getVirtualFields();
            assertFalse(original != null && original.containsKey(name));
            virtualFieldsBefore.put(table, original);
            table.setVirtualFields(original == null ? new LinkedHashMap<>() : new LinkedHashMap<>(original));
            table.withVirtualField(lengthField(name, source));
         }
      }
      metadataBefore = metadata();
      return originalMetadata;
   }



   /*******************************************************************************
    ** Assert the full declared variation before restoring original metadata.
    *******************************************************************************/
   private void removeVirtualLengths(String originalMetadata)
   {
      try
      {
         assertTrue(metadataBefore.equals(metadata()), "Virtual field metadata must remain unchanged");
      }
      finally
      {
         virtualFieldsBefore.forEach(QTableMetaData::setVirtualFields);
         virtualFieldsBefore.clear();
         metadataBefore = originalMetadata;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QVirtualFieldMetaData lengthField(String name, String source)
   {
      return new QVirtualFieldMetaData(name, QFieldType.INTEGER).withIsQuerySelectable(true).withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction().withFieldName(source).withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER));
   }



   /*******************************************************************************
    ** Only the location of the identical criterion changes between flat/nested.
    *******************************************************************************/
   private QQueryFilter virtualFilter(String field, boolean nested)
   {
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria(field, QCriteriaOperator.GREATER_THAN, 4));
      return nested ? new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.AND).withSubFilter(filter) : filter;
   }



   /*******************************************************************************
    ** Both raw fanout and distinct parent counts use the same virtual predicate.
    *******************************************************************************/
   private CountInput virtualCount(boolean joined, QQueryFilter filter)
   {
      CountInput input = new CountInput("person");
      input.setInputSource(QInputSource.SYSTEM);
      input.setFilter(filter);
      input.setIncludeDistinctCount(true);
      if(joined)
      {
         input.setQueryJoins(List.of(childJoin(QueryJoin.Type.INNER)));
      }
      return input;
   }



   /*******************************************************************************
    ** Integer function results and selected identities cannot pass as Strings.
    *******************************************************************************/
   private void assertVirtualProjectionType(String field, Object value)
   {
      if(value != null && (field.equals("firstNameLength") || field.equals("animal.nameLength") || field.equals("id") || field.endsWith(".id")))
      {
         assertInstanceOf(Integer.class, value);
      }
      if(value != null && field.equals("animal.name"))
      {
         assertInstanceOf(String.class, value);
      }
   }



   /*******************************************************************************
    ** Each store executes independently against the same precomputed native SQL.
    *******************************************************************************/
   private void assertBoth(Supplier<AggregateInput> request, String query) throws Exception
   {
      List<List<String>> expected = sql(query);
      assertAll(query,
         () -> assertAll("RDBMS Aggregate", () -> assertResult(nativeInstance, request, expected)),
         () -> assertAll("Memory Aggregate", () -> assertResult(memoryInstance, request, expected)));
   }



   /*******************************************************************************
    ** Fresh inputs prevent one adapter's metadata/filter work affecting the other.
    *******************************************************************************/
   private void assertResult(QInstance instance, Supplier<AggregateInput> request, List<List<String>> expected) throws Exception
   {
      QContext.init(instance, session);
      AggregateInput input = request.get();
      String filterBefore = JsonUtils.toJson(input.getFilter());
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      AggregateOutput output = new AggregateAction().execute(input);
      List<List<String>> actual = new ArrayList<>();
      for(AggregateResult result : output.getResults())
      {
         assertEquals(new LinkedHashSet<>(input.getAggregates()), result.getAggregateValues().keySet());
         assertEquals(new LinkedHashSet<>(input.getGroupBys()), result.getGroupByValues().keySet());
         List<String> row = new ArrayList<>();
         input.getGroupBys().forEach(group ->
         {
            assertVirtualProjectionType(group.getFieldName(), result.getGroupByValue(group));
            row.add(value(result.getGroupByValue(group)));
         });
         input.getAggregates().forEach(aggregate ->
         {
            assertVirtualProjectionType(aggregate.getFieldName(), result.getAggregateValue(aggregate));
            row.add(value(result.getAggregateValue(aggregate)));
         });
         actual.add(row);
      }
      assertAll(
         () -> assertEquals(expected, actual, instance == memoryInstance ? "Memory Aggregate" : "H2 Aggregate"),
         () -> assertEquals(filterBefore, JsonUtils.toJson(input.getFilter())),
         () -> assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins())),
         () -> assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged"));
   }



   /*******************************************************************************
    ** Metadata refusal must not be a native SQL or missing-table dereference.
    *******************************************************************************/
   private void assertRefusedBoth(Supplier<AggregateInput> request)
   {
      assertAll(() -> assertRefused(nativeInstance, request), () -> assertRefused(memoryInstance, request));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRefused(QInstance instance, Supplier<AggregateInput> request)
   {
      QContext.init(instance, session);
      AggregateInput input = request.get();
      String filterBefore = JsonUtils.toJson(input.getFilter());
      String joinsBefore = JsonUtils.toJson(input.getQueryJoins());
      QException failure = assertThrows(QException.class, () -> new AggregateAction().execute(input));
      for(Throwable cause = failure; cause != null; cause = cause.getCause())
      {
         assertFalse(cause instanceof SQLException, "SQL failure is not a metadata refusal");
         assertFalse(cause instanceof NullPointerException, "Metadata refusal must be deliberate");
      }
      assertEquals(filterBefore, JsonUtils.toJson(input.getFilter()));
      assertEquals(joinsBefore, JsonUtils.toJson(input.getQueryJoins()));
      assertTrue(metadataBefore.equals(metadata()), "Canonical metadata must remain unchanged");
   }



   /*******************************************************************************
    ** The supported native store read observes every seeded field, without Query
    ** presentation masking. SYSTEM removes no fields in these fixture policies.
    *******************************************************************************/
   private Map<String, List<Map<String, String>>> memoryRows() throws Exception
   {
      QContext.init(memoryInstance, session);
      Map<String, List<Map<String, String>>> tables = new LinkedHashMap<>();
      for(String table : List.of("person", "pet"))
      {
         List<Map<String, String>> rows = new ArrayList<>();
         for(QRecord record : MemoryRecordStore.getInstance().query(new QueryInput(table).withInputSource(QInputSource.SYSTEM)
            .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")))))
         {
            Map<String, String> row = new LinkedHashMap<>();
            record.getValues().forEach((field, stored) -> row.put(field, value(stored)));
            rows.add(row);
         }
         tables.put(table, rows);
      }
      return tables;
   }



   /*******************************************************************************
    ** Check the typed JDBC-to-Memory copy before exercising any aggregate.
    *******************************************************************************/
   private void assertSeed(String table, List<String> fields, String query) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      for(Map<String, String> record : memoryBefore.get(table))
      {
         List<String> row = new ArrayList<>();
         fields.forEach(field -> row.add(record.get(field)));
         rows.add(row);
      }
      assertEquals(sql(query), rows);
   }



   /*******************************************************************************
    ** Full canonical H2 rows remain unchanged, including unrelated Note/FieldLab.
    *******************************************************************************/
   private Map<String, List<List<String>>> nativeRows() throws Exception
   {
      Map<String, List<List<String>>> tables = new LinkedHashMap<>();
      for(String table : List.of("person", "pet", "pet_note", "field_lab"))
      {
         tables.put(table, sql("SELECT * FROM " + table + " ORDER BY id"));
      }
      return tables;
   }



   /*******************************************************************************
    ** Fixed fixture SQL only; aggregate results here are integer counts and text.
    *******************************************************************************/
   private List<List<String>> sql(String query) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
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
      return rows;
   }



   /*******************************************************************************
    ** Independent instances retain their canonical fields and join registry.
    *******************************************************************************/
   private String metadata()
   {
      List<String> values = new ArrayList<>();
      for(QInstance instance : List.of(nativeInstance, memoryInstance))
      {
         values.add(JsonUtils.toJson(List.of(instance.getTables(), instance.getJoins())));
      }
      return JsonUtils.toJson(values);
   }



   /*******************************************************************************
    ** Policy registration changes no table/field object in either instance.
    *******************************************************************************/
   private void register(Class<? extends TableMetaDataPersonalizerInterface> customizer)
   {
      for(QInstance instance : List.of(nativeInstance, memoryInstance))
      {
         instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(customizer));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput input(String table, QInputSource source, Aggregate... aggregates)
   {
      return new AggregateInput(table).withInputSource(source).withAggregates(List.of(aggregates));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput people(QueryJoin.Type type, QInputSource source)
   {
      return input("person", source, count("id"), count("animal.id"), new Aggregate("id", AggregateOperator.COUNT_DISTINCT))
         .withQueryJoin(childJoin(type));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput filteredPeople(QueryJoin.Type type, QInputSource source)
   {
      return people(type, source).withFilter(new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))
         .withCriteria(new QFilterCriteria("animal.name", QCriteriaOperator.EQUALS, "Mae")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private AggregateInput baseCountJoin(QInputSource source)
   {
      return input("person", source, count("id")).withQueryJoin(childJoin(QueryJoin.Type.INNER));
   }



   /*******************************************************************************
    ** These are registered query joins, independent of association metadata.
    *******************************************************************************/
   private QueryJoin childJoin(QueryJoin.Type type)
   {
      return new QueryJoin("pet").withAlias("animal").withSelect(false).withType(type);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Aggregate count(String field)
   {
      return new Aggregate(field, AggregateOperator.COUNT);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String value(Object value)
   {
      return value == null ? null : value.toString();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
      assertEquals("jdbc:h2:mem:test_database", connection.getMetaData().getURL());
      return connection;
   }



   /*******************************************************************************
    ** The physical/native types stay INTEGER for id and STRING for names. Only
    ** active presentation flags change; all canonical metadata stays untouched.
    *******************************************************************************/
   public static class HeavySharedMetadata implements TableMetaDataPersonalizerInterface
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
         if("person".equals(input.getTableName()))
         {
            table.getField("id").setIsHidden(true);
            table.getField("firstName").setIsHeavy(true);
         }
         if("pet".equals(input.getTableName()))
         {
            table.getField("name").setIsHeavy(true);
         }
         return table;
      }
   }



   /*******************************************************************************
    ** Only active USER child metadata is private; the stored values stay unchanged.
    *******************************************************************************/
   public static class UnselectedPrivateChild implements TableMetaDataPersonalizerInterface
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
         if("person".equals(input.getTableName()))
         {
            table.getFields().keySet().retainAll(List.of("id", "firstName", "lastName", "daysWorked"));
         }
         if("pet".equals(input.getTableName()))
         {
            table.getField("name").setType(QFieldType.PASSWORD);
            table.getField("speciesId").setIsHidden(true);
         }
         return table;
      }
   }



   /*******************************************************************************
    ** Two independent unavailable-metadata boundaries in the same USER fixture.
    *******************************************************************************/
   public static class SharedRemovedMetadata implements TableMetaDataPersonalizerInterface
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
         if("pet".equals(input.getTableName()))
         {
            return null;
         }
         if("person".equals(input.getTableName()))
         {
            QTableMetaData table = input.getTable().clone();
            table.getFields().remove("id");
            return table;
         }
         return input.getTable();
      }
   }



   /*******************************************************************************
    ** Only USER gets the additional child policy; the original table is untouched.
    *******************************************************************************/
   public static class ChildRead implements TableMetaDataPersonalizerInterface
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
         return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withFieldName("speciesId")
            .withSecurityKeyType("memoryJoinSpecies").withLockScope(RecordSecurityLock.LockScope.READ)
            .withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemoveChildName implements TableMetaDataPersonalizerInterface
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
         QTableMetaData table = input.getTable().clone();
         table.getFields().remove("name");
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemoveStructuralKeys extends ChildRead
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         QTableMetaData table = super.execute(input);
         if(input.getInputSource() == QInputSource.USER && "pet".equals(input.getTableName()))
         {
            table.getFields().remove("speciesId");
            table.getFields().remove("personId");
         }
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemoveChildTable implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         return input.getInputSource() == QInputSource.USER && "pet".equals(input.getTableName()) ? null : input.getTable();
      }
   }
}
