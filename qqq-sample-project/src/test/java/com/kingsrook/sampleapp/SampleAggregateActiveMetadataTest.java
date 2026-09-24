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


import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
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
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
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
 ** Active Aggregate metadata must reach native SQL without changing canonical
 ** schema or making private structural fields public selectable operands.
 *******************************************************************************/
class SampleAggregateActiveMetadataTest
{
   private QInstance instance;
   private Map<String, List<List<String>>> before;



   /*******************************************************************************
    ** Numeric root mapping and existing Person/Pet fields supply distinct oracles.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value) VALUES(1,'Alpha',10),(2,'Beta',20),(3,'Gamma',30)"));
      }
      before = snapshot();
   }



   /*******************************************************************************
    ** All four canonical tables remain byte-for-byte unchanged by these reads.
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
    ** USER maps logical longValue to existing id; SYSTEM uses long_value. Aggregate,
    ** grouping, ordering and predicate resolution must use that same active field.
    *******************************************************************************/
   @Test
   void testRootPhysicalColumnMappingControlsNativeOperations() throws Exception
   {
      register(RootMapping.class);
      Map<String, String> metadata = metadata();
      GroupBy group = new GroupBy(QFieldType.LONG, "longValue");
      assertAll(
         () -> assertNative(input("fieldLab", sum("longValue")), "SELECT SUM(id) FROM field_lab"),
         () -> assertNative(input("fieldLab", sum("longValue")).withInputSource(QInputSource.SYSTEM), "SELECT SUM(long_value) FROM field_lab"),
         () -> assertNative(input("fieldLab", count("id")).withGroupBy(group).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(group))),
            "SELECT id,COUNT(id) FROM field_lab GROUP BY id ORDER BY id"),
         () -> assertNative(input("fieldLab", sum("longValue")).withFilter(new QQueryFilter(new QFilterCriteria("longValue", QCriteriaOperator.EQUALS, 2L))),
            "SELECT SUM(id) FROM field_lab WHERE id=2"),
         () -> assertEquals(metadata, metadata()));
   }



   /*******************************************************************************
    ** Joined USER READ policy belongs in the native INNER/LEFT join semantics.
    ** A denied child must not remove its permitted parent from a LEFT aggregate.
    *******************************************************************************/
   @Test
   void testJoinedPersonalizedReadLockAndSystemControl() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("aggregateSpecies"));
      QContext.setQSession(new QSession().withSecurityKeyValue("aggregateSpecies", 1));
      register(JoinedReadPolicy.class);
      Map<String, String> metadata = metadata();
      assertAll(
         () -> assertNative(people(QueryJoin.Type.INNER), "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id AND a.species_id=1"),
         () -> assertNative(people(QueryJoin.Type.LEFT), "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id AND a.species_id=1"),
         () -> assertNative(people(QueryJoin.Type.INNER).withInputSource(QInputSource.SYSTEM), "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertNative(people(QueryJoin.Type.LEFT).withInputSource(QInputSource.SYSTEM), "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p LEFT JOIN pet a ON p.id=a.person_id"),
         () -> assertEquals(metadata, metadata()));
   }



   /*******************************************************************************
    ** Joined logical firstName maps to native last_name only for USER. The values
    ** and group multiplicities differ from canonical first_name, including filters.
    *******************************************************************************/
   @Test
   void testJoinedPhysicalMappingAndSystemControl() throws Exception
   {
      register(JoinedMapping.class);
      Map<String, String> metadata = metadata();
      GroupBy group = new GroupBy(QFieldType.STRING, "owner.firstName");
      Aggregate first = new Aggregate("owner.firstName", AggregateOperator.MIN);
      assertAll(
         () -> assertNative(pets(first), "SELECT MIN(p.last_name) FROM pet a INNER JOIN person p ON a.person_id=p.id"),
         () -> assertNative(pets(first).withInputSource(QInputSource.SYSTEM), "SELECT MIN(p.first_name) FROM pet a INNER JOIN person p ON a.person_id=p.id"),
         () -> assertNative(pets(count("id")).withGroupBy(group).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByGroupBy(group))),
            "SELECT p.last_name,COUNT(a.id) FROM pet a INNER JOIN person p ON a.person_id=p.id GROUP BY p.last_name ORDER BY p.last_name"),
         () -> assertNative(pets(count("id")).withFilter(new QQueryFilter(new QFilterCriteria("owner.firstName", QCriteriaOperator.EQUALS, "Sample"))),
            "SELECT COUNT(a.id) FROM pet a INNER JOIN person p ON a.person_id=p.id WHERE p.last_name='Sample'"),
         () -> assertEquals(metadata, metadata()));
   }



   /*******************************************************************************
    ** Removed joined fields remain invalid user-supplied operands, group/filter
    ** references and typed aggregate sorts. SYSTEM keeps its canonical control.
    *******************************************************************************/
   @Test
   void testRemovedJoinedFieldDoesNotReappearAsPublicOperand() throws Exception
   {
      register(RemoveJoinedField.class);
      Map<String, String> metadata = metadata();
      Aggregate removed = new Aggregate("owner.firstName", AggregateOperator.MIN);
      assertAll(
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(pets(removed))),
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(pets(count("id")).withGroupBy(new GroupBy(QFieldType.STRING, "owner.firstName")))),
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(pets(count("id")).withFilter(new QQueryFilter(new QFilterCriteria("owner.firstName", QCriteriaOperator.EQUALS, "Avery"))))),
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(pets(count("id")).withFilter(new QQueryFilter().withOrderBy(new QFilterOrderByAggregate(removed))))),
         () -> assertNative(pets(removed).withInputSource(QInputSource.SYSTEM), "SELECT MIN(p.first_name) FROM pet a INNER JOIN person p ON a.person_id=p.id"),
         () -> assertEquals(metadata, metadata()));
   }



   /*******************************************************************************
    ** Existing structural READ/JoinOn keys may be privately needed after USER
    ** field removal. Permitted COUNT works; explicit removed operands still reject.
    *******************************************************************************/
   @Test
   void testRemovedStructuralKeysRetainReadAndJoinSemantics() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("aggregateOwner"));
      instance.getTable("fieldLab").withRecordSecurityLock(new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("aggregateOwner")
         .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      QContext.setQSession(new QSession().withSecurityKeyValue("aggregateOwner", 20L));
      register(RemoveStructuralFields.class);
      Map<String, String> metadata = metadata();
      AggregateInput root = input("fieldLab", count("id"));
      assertAll(
         () ->
         {
            assertNative(root, "SELECT COUNT(id) FROM field_lab WHERE long_value=20");
            assertFalse(root.getTable().getFields().containsKey("longValue"), "Private execution fields must not hydrate public input metadata");
         },
         () -> assertNative(people(QueryJoin.Type.INNER), "SELECT COUNT(p.id),COUNT(a.id),COUNT(DISTINCT p.id) FROM person p INNER JOIN pet a ON p.id=a.person_id"),
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(input("fieldLab", sum("longValue")))),
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(input("person", sum("animal.personId"))
            .withQueryJoin(new QueryJoin("pet").withAlias("animal").withSelect(false)))),
         () -> assertNative(input("fieldLab", sum("longValue")).withInputSource(QInputSource.SYSTEM), "SELECT SUM(long_value) FROM field_lab WHERE long_value=20"),
         () -> assertTrue(instance.getTable("fieldLab").getFields().containsKey("longValue")),
         () -> assertTrue(instance.getTable("pet").getFields().containsKey("personId")),
         () -> assertEquals(metadata, metadata()));
   }



   /*******************************************************************************
    ** Returning no active table must not restore its canonical table implicitly.
    ** USER's permitted base Pet and SYSTEM tables remain independent controls.
    *******************************************************************************/
   @Test
   void testRemovedRootAndJoinedTablesReject() throws Exception
   {
      register(RemoveTables.class);
      Map<String, String> metadata = metadata();
      assertAll(
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(input("fieldLab", count("id")))),
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(pets(count("id")))),
         () -> assertThrows(QException.class, () -> new AggregateAction().execute(pets(new Aggregate("owner.firstName", AggregateOperator.MIN)))),
         () -> assertNative(input("fieldLab", sum("longValue")).withInputSource(QInputSource.SYSTEM), "SELECT SUM(long_value) FROM field_lab"),
         () -> assertNative(pets(count("id")).withInputSource(QInputSource.SYSTEM), "SELECT COUNT(a.id) FROM pet a INNER JOIN person p ON a.person_id=p.id"),
         () -> assertNative(input("pet", count("id")), "SELECT COUNT(id) FROM pet"),
         () -> assertEquals(metadata, metadata()));
   }



   /*******************************************************************************
    ** Compare complete result tuples and exact descriptor-key sets to native SQL.
    *******************************************************************************/
   private AggregateOutput assertNative(AggregateInput input, String query) throws Exception
   {
      AggregateOutput output = new AggregateAction().execute(input);
      List<List<String>> actual = new ArrayList<>();
      for(AggregateResult result : output.getResults())
      {
         assertEquals(new LinkedHashSet<>(input.getAggregates()), result.getAggregateValues().keySet());
         assertEquals(new LinkedHashSet<>(input.getGroupBys()), result.getGroupByValues().keySet());
         List<String> row = new ArrayList<>();
         input.getGroupBys().forEach(group -> row.add(value(result.getGroupByValue(group))));
         input.getAggregates().forEach(aggregate -> row.add(value(result.getAggregateValue(aggregate))));
         actual.add(row);
      }
      List<List<String>> expected = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(query))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
            {
               row.add(value(result.getObject(index)));
            }
            expected.add(row);
         }
      }
      assertEquals(expected, actual);
      return output;
   }



   /*******************************************************************************
    ** All native field values remain unchanged; no BLOBs are populated here.
    *******************************************************************************/
   private Map<String, List<List<String>>> snapshot() throws Exception
   {
      Map<String, List<List<String>>> tables = new LinkedHashMap<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         for(String table : List.of("field_lab", "person", "pet", "pet_note"))
         {
            List<List<String>> rows = new ArrayList<>();
            try(ResultSet result = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id"))
            {
               while(result.next())
               {
                  List<String> row = new ArrayList<>();
                  for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
                  {
                     row.add(result.getString(index));
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
    ** Canonical metadata stays authoritative and untouched outside private views.
    *******************************************************************************/
   private Map<String, String> metadata()
   {
      Map<String, String> result = new LinkedHashMap<>();
      for(String table : List.of("fieldLab", "person", "pet"))
      {
         result.put(table, JsonUtils.toJson(instance.getTable(table)));
      }
      return result;
   }



   /*******************************************************************************
    ** Register one finite trusted policy per independently initialized fixture.
    *******************************************************************************/
   private void register(Class<?> customizer)
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(customizer));
   }



   /*******************************************************************************
    ** No driver-specific numeric decimal scale is required.
    *******************************************************************************/
   private static String value(Object value)
   {
      return value instanceof Number ? new BigDecimal(value.toString()).stripTrailingZeros().toPlainString() : value == null ? null : value.toString();
   }



   /*******************************************************************************
    ** Sources are explicit at every boundary.
    *******************************************************************************/
   private AggregateInput input(String table, Aggregate... aggregates)
   {
      return new AggregateInput(table).withAggregates(List.of(aggregates)).withInputSource(QInputSource.USER);
   }



   /*******************************************************************************
    ** Direct joins use canonical exposed associations with independent aliases.
    *******************************************************************************/
   private AggregateInput people(QueryJoin.Type type)
   {
      return input("person", count("id"), count("animal.id"), new Aggregate("id", AggregateOperator.COUNT_DISTINCT))
         .withQueryJoin(new QueryJoin("pet").withAlias("animal").withSelect(false).withType(type));
   }



   /*******************************************************************************
    ** Reverse lookup of the registered Person/Pet join is existing read behavior.
    *******************************************************************************/
   private AggregateInput pets(Aggregate... aggregates)
   {
      return input("pet", aggregates).withQueryJoin(new QueryJoin("person").withAlias("owner").withSelect(false).withType(QueryJoin.Type.INNER));
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
   private static Aggregate sum(String field)
   {
      return new Aggregate(field, AggregateOperator.SUM);
   }



   /*******************************************************************************
    ** Every native connection is fixture-owned.
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    ** Change a numeric field's physical mapping only in active USER metadata.
    *******************************************************************************/
   public static class RootMapping implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !"fieldLab".equals(input.getTableName()))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getField("longValue").setBackendName("id");
         return table;
      }
   }



   /*******************************************************************************
    ** Joined READ policy is distinct from HTTP table permission checks.
    *******************************************************************************/
   public static class JoinedReadPolicy implements TableMetaDataPersonalizerInterface
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
         return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withFieldName("speciesId").withSecurityKeyType("aggregateSpecies")
            .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
      }
   }



   /*******************************************************************************
    ** Same-type physical columns provide an unambiguous joined mapping control.
    *******************************************************************************/
   public static class JoinedMapping implements TableMetaDataPersonalizerInterface
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
         table.getField("firstName").setBackendName("last_name");
         return table;
      }
   }



   /*******************************************************************************
    ** Removed joined fields remain unavailable to explicit caller expressions.
    *******************************************************************************/
   public static class RemoveJoinedField implements TableMetaDataPersonalizerInterface
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
         table.getFields().remove("firstName");
         return table;
      }
   }



   /*******************************************************************************
    ** Only canonical structural definitions survive privately after field removal.
    *******************************************************************************/
   public static class RemoveStructuralFields implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !("fieldLab".equals(input.getTableName()) || "pet".equals(input.getTableName())))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getFields().remove("fieldLab".equals(input.getTableName()) ? "longValue" : "personId");
         return table;
      }
   }



   /*******************************************************************************
    ** No active USER metadata is supplied for these two canonical tables.
    *******************************************************************************/
   public static class RemoveTables implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() == QInputSource.USER && ("fieldLab".equals(input.getTableName()) || "person".equals(input.getTableName())))
         {
            return null;
         }
         return input.getTable();
      }
   }
}
