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


import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.values.BasicCustomPossibleValueProvider;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheOf;
import com.kingsrook.qqq.backend.core.model.metadata.tables.cache.CacheUseCase;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.utils.RecordLookupHelper;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native join cardinalities and lookup/provider contracts on owned sample data.
 *******************************************************************************/
class SampleLookupAndJoinTest
{
   private QInstance instance;
   private static final String CACHE = "ownedPersonCache";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      sql("DROP TABLE IF EXISTS owned_person_cache");
      sql("CREATE TABLE owned_person_cache (id INTEGER AUTO_INCREMENT PRIMARY KEY, email VARCHAR(250) UNIQUE, first_name VARCHAR(100), last_name VARCHAR(100), cached_date TIMESTAMP)");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getTable("person").withUniqueKey(new UniqueKey("email"));
      instance.addTable(personProjection("ownedPersonMirror", "person"));
      instance.addTable(personProjection(CACHE, "owned_person_cache")
         .withField(new QFieldMetaData("cachedDate", QFieldType.DATE_TIME).withBackendName("cached_date"))
         .withCacheOf(new CacheOf().withSourceTable("person").withExpirationSeconds(60).withCachedDateFieldName("cachedDate")
            .withUseCase(new CacheUseCase().withType(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY)
               .withCacheUniqueKey(new UniqueKey("email")).withSourceUniqueKey(new UniqueKey("email"))
               .withDoCopySourcePrimaryKeyToCache(true))));
      instance.addJoin(new QJoinMetaData().withName("ownedOneToOne").withLeftTable("person").withRightTable("ownedPersonMirror")
         .withType(JoinType.ONE_TO_ONE).withJoinOn(new JoinOn("id", "id")));
      instance.addJoin(new QJoinMetaData().withName("ownedManyToMany").withLeftTable("person").withRightTable("ownedPersonMirror")
         .withType(JoinType.MANY_TO_MANY).withJoinOn(new JoinOn("lastName", "lastName")));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable("person").withName("ownedPersonEmail")
         .withIdType(QFieldType.STRING).withOverrideIdField("email"));
      instance.addPossibleValueSource(new QPossibleValueSource().withName("ownedCustomValues").withType(QPossibleValueSourceType.CUSTOM)
         .withIdType(QFieldType.INTEGER).withCustomCodeReference(new QCodeReference(OwnedValues.class)));
      QContext.init(instance, new QSession().withPermissions());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneToOneAndManyToManyNativeJoinCardinality() throws Exception
   {
      for(String name : List.of("ownedOneToOne", "ownedManyToMany"))
      {
         boolean oneToOne = name.equals("ownedOneToOne");
         QueryInput input = new QueryInput("person").withInputSource(QInputSource.USER)
            .withQueryJoin(new QueryJoin(instance.getJoin(name)).withAlias("peer").withSelect(true).withType(QueryJoin.Type.INNER))
            .withFieldNamesToInclude(Set.of("id", "peer.id"))
            .withFilter(new QQueryFilter().withOrderBy(new QFilterOrderBy("id")).withOrderBy(new QFilterOrderBy("peer.id")));
         List<QRecord> records = new QueryAction().execute(input).getRecords();
         String condition = oneToOne ? "p.id=q.id" : "p.last_name=q.last_name";
         List<String> expected = rows("SELECT p.id || ':' || q.id FROM person p JOIN person q ON " + condition + " ORDER BY p.id,q.id");
         assertEquals(oneToOne ? 5 : 25, expected.size());
         assertEquals(expected, records.stream().map(record -> record.getValueInteger("id") + ":" + record.getValueInteger("peer.id")).toList());
      }
      assertEquals(List.of("5"), rows("SELECT COUNT(*) FROM person"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTableEnumAndCustomPossibleValueSearchSelectionAndSort() throws Exception
   {
      instance.getPossibleValueSource("person").setOrderByFields(List.of(new QFilterOrderBy("firstName", false)));
      assertEquals(List.of("5:Morgan Sample", "4:Drew Sample", "3:Casey Sample", "2:Blair Sample", "1:Avery Sample"), values(search("person")));
      assertEquals(List.of("1:Avery Sample"), values(search("person").withSearchTerm("Av")));
      assertEquals(List.of("3:Casey Sample", "1:Avery Sample"), values(search("person").withIdList(List.of("1", 3))));
      assertEquals(List.of(), values(search("person").withIdList(List.of(999))));
      assertEquals(List.of("1:Dog"), values(search("petSpecies").withSearchTerm("Do")));
      assertEquals(List.of("2:Cat"), values(search("petSpecies").withIdList(List.of("2"))));
      assertEquals(List.of("2:Cat"), values(search("petSpecies").withLabelList(List.of("cAt"))));
      assertEquals(List.of(), values(search("petSpecies").withIdList(List.of("not-an-integer"))));
      assertEquals(List.of("101:Alpha", "102:Beta"), values(search("ownedCustomValues")));
      assertEquals(List.of("101:Alpha"), values(search("ownedCustomValues").withSearchTerm("al")));
      assertEquals(List.of("102:Beta"), values(search("ownedCustomValues").withIdList(List.of("102"))));
      assertEquals(List.of(), values(search("ownedCustomValues").withIdList(List.of(999))));
      assertThrows(QException.class, () -> values(search("missingProvider")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPossibleValueFallbackAndDeniedForeignRecord() throws Exception
   {
      QPossibleValueSource source = instance.getPossibleValueSource("person");
      source.withValueFormatIfNotFound("Missing %s").withValueFieldsIfNotFound(List.of("id"));
      QPossibleValueTranslator translator = new QPossibleValueTranslator(instance, QContext.getQSession());
      assertEquals("Missing 999", translator.buildTranslatedPossibleValueList(source, List.of(999)).get(0).getLabel());
      assertTrue(translator.buildTranslatedPossibleValueList(source, List.of()).isEmpty());
      protect("person");
      QContext.getQSession().withSecurityKeyValue("ownedPerson", 1);
      assertEquals(List.of("1:Avery Sample"), values(search("person")));
      assertEquals(List.of(), values(search("person").withIdList(List.of(2))));
      translator = new QPossibleValueTranslator(instance, QContext.getQSession());
      assertEquals("Missing 2", translator.buildTranslatedPossibleValueList(source, List.of(2)).get(0).getLabel());
      assertEquals(List.of("Blair"), rows("SELECT first_name FROM person WHERE id=2"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOverrideIdentifierSearchAndTranslationWithSelectedLookupLimitation() throws Exception
   {
      QPossibleValueSource source = instance.getPossibleValueSource("ownedPersonEmail");
      assertEquals(List.of("avery@example.invalid:Avery Sample"), values(search(source.getName()).withSearchTerm("Av")));
      QPossibleValueTranslator translator = new QPossibleValueTranslator(instance, QContext.getQSession());
      assertEquals("Avery Sample", translator.buildTranslatedPossibleValueList(source, List.of("avery@example.invalid")).get(0).getLabel());
      QException exception = assertThrows(QException.class, () -> values(search(source.getName()).withIdList(List.of("avery@example.invalid"))));
      assertTrue(exception.getCause() instanceof QValueException);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRecordLookupPreloadUniqueKeysLazyFetchAndMisses() throws Exception
   {
      RecordLookupHelper filtered = new RecordLookupHelper();
      filtered.preloadRecords("person", "id", new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, 2)));
      filtered.setMayNotDoOneOffLookups("person", "id");
      assertEquals("Avery", filtered.getRecordValue("person", "firstName", "id", 1));
      assertNull(filtered.getRecordByKey("person", "id", 3));
      RecordLookupHelper preloaded = new RecordLookupHelper();
      preloaded.preloadRecords("person", "id", List.of(1, 3, 999));
      assertEquals("Casey", preloaded.getRecordByKey("person", "id", 3).getValueString("firstName"));
      assertNull(preloaded.getRecordByKey("person", "id", 999));
      preloaded.preloadRecords("person", "email");
      assertEquals(2, preloaded.getRecordId("person", "email", "blair@example.invalid"));
      RecordLookupHelper lazy = new RecordLookupHelper();
      assertEquals("Avery", lazy.getRecordByUniqueKey("person", Map.of("email", "avery@example.invalid")).getValueString("firstName"));
      assertEquals("Casey", lazy.getRecordValue("person", "firstName", "id", "3", String.class));
      sql("UPDATE person SET first_name='Fresh' WHERE id=1");
      assertEquals(List.of("Fresh"), rows("SELECT first_name FROM person WHERE id=1"));
      assertEquals("Avery", lazy.getRecordByUniqueKey("person", Map.of("email", "avery@example.invalid")).getValueString("firstName"));
      assertEquals("Fresh", new RecordLookupHelper().getRecordByKey("person", "id", 1).getValueString("firstName"));
      assertThrows(QValueException.class, () -> lazy.getRecordByKey("person", "id", "not-an-integer"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCacheGetExpiryAndMissingSource() throws Exception
   {
      assertEquals("Avery", cached("avery@example.invalid").getValueString("firstName"));
      assertEquals(List.of("Avery"), rows("SELECT first_name FROM owned_person_cache"));
      sql("UPDATE person SET first_name='Fresh' WHERE id=1");
      assertEquals("Avery", cached("avery@example.invalid").getValueString("firstName"));
      sql("UPDATE owned_person_cache SET cached_date=TIMESTAMP '2000-01-01 00:00:00'");
      assertEquals("Fresh", cached("avery@example.invalid").getValueString("firstName"));
      assertEquals(List.of("Fresh"), rows("SELECT first_name FROM owned_person_cache"));
      sql("DELETE FROM person WHERE id=1");
      assertNotNull(cached("avery@example.invalid"));
      sql("UPDATE owned_person_cache SET cached_date=TIMESTAMP '2000-01-01 00:00:00'");
      assertNull(cached("avery@example.invalid"));
      assertEquals(List.of("0"), rows("SELECT COUNT(*) FROM owned_person_cache"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCacheQueryRefreshAndWarmCacheRecordLocks() throws Exception
   {
      assertEquals(List.of("Avery", "Blair"), cachedQuery("avery@example.invalid", "blair@example.invalid").stream().map(record -> record.getValueString("firstName")).toList());
      assertEquals(List.of("2"), rows("SELECT COUNT(*) FROM owned_person_cache"));
      sql("UPDATE person SET first_name='Fresh' WHERE id=1");
      sql("UPDATE owned_person_cache SET cached_date=TIMESTAMP '2000-01-01 00:00:00'");
      assertEquals(List.of("Fresh", "Blair"), cachedQuery("avery@example.invalid", "blair@example.invalid").stream().map(record -> record.getValueString("firstName")).toList());
      protect("person");
      protect(CACHE);
      QContext.getQSession().withSecurityKeyValue("ownedPerson", 2);
      assertEquals(List.of("Blair"), cachedQuery("avery@example.invalid", "blair@example.invalid").stream().map(record -> record.getValueString("firstName")).toList());
      assertNull(cached("avery@example.invalid"));
      assertEquals(List.of("2"), rows("SELECT COUNT(*) FROM owned_person_cache"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeclaredCacheModesAndMissCachingRemainUnsupported() throws Exception
   {
      CacheUseCase useCase = instance.getTable(CACHE).getCacheOf().getUseCases().get(0);
      for(CacheUseCase.Type type : List.of(CacheUseCase.Type.PRIMARY_KEY_TO_PRIMARY_KEY, CacheUseCase.Type.UNIQUE_KEY_TO_PRIMARY_KEY))
      {
         useCase.setType(type);
         assertNull(cached("avery@example.invalid"));
         assertTrue(cachedQuery("avery@example.invalid").isEmpty());
         assertEquals(List.of("Avery"), rows("SELECT first_name FROM person WHERE email='avery@example.invalid'"));
         assertEquals(List.of("0"), rows("SELECT COUNT(*) FROM owned_person_cache"));
      }
      useCase.setType(CacheUseCase.Type.UNIQUE_KEY_TO_UNIQUE_KEY);
      useCase.setCacheSourceMisses(true);
      assertNull(cached("later@example.invalid"));
      assertEquals(List.of("0"), rows("SELECT COUNT(*) FROM owned_person_cache"));
      sql("INSERT INTO person(id,first_name,last_name,email) VALUES (99,'Later','Sample','later@example.invalid')");
      assertEquals("Later", cached("later@example.invalid").getValueString("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QTableMetaData personProjection(String name, String nativeTable)
   {
      return new QTableMetaData().withName(name).withBackendName(SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName(nativeTable)).withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("email")).withRecordLabelFields("firstName", "lastName").withRecordLabelFormat("%s %s")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("email", QFieldType.STRING))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING).withBackendName("first_name"))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING).withBackendName("last_name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void protect(String table)
   {
      if(instance.getSecurityKeyType("ownedPerson") == null)
      {
         instance.addSecurityKeyType(new QSecurityKeyType().withName("ownedPerson"));
      }
      instance.getTable(table).withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("ownedPerson")
         .withFieldName("id").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SearchPossibleValueSourceInput search(String source)
   {
      return new SearchPossibleValueSourceInput().withPossibleValueSourceName(source);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> values(SearchPossibleValueSourceInput input) throws QException
   {
      return new SearchPossibleValueSourceAction().execute(input).getResults().stream().map(value -> value.getId() + ":" + value.getLabel()).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord cached(String email) throws QException
   {
      return new GetAction().executeForRecord(new GetInput(CACHE).withUniqueKey(Map.of("email", email)).withInputSource(QInputSource.USER));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QRecord> cachedQuery(String... emails) throws QException
   {
      return new QueryAction().execute(new QueryInput(CACHE).withInputSource(QInputSource.USER)
         .withFilter(new QQueryFilter(new QFilterCriteria("email", QCriteriaOperator.IN, List.of(emails))).withOrderBy(new QFilterOrderBy("email")))).getRecords();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void sql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> rows(String sql) throws Exception
   {
      List<String> result = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql))
      {
         while(rows.next())
         {
            result.add(rows.getString(1));
         }
      }
      return result;
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OwnedValues extends BasicCustomPossibleValueProvider<QPossibleValue<Integer>, Integer>
   {


      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected QPossibleValue<Integer> makePossibleValue(QPossibleValue<Integer> source)
      {
         return source;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected QPossibleValue<Integer> getSourceObject(Serializable id)
      {
         return getAllSourceObjects().stream().filter(value -> String.valueOf(value.getId()).equals(String.valueOf(id))).findFirst().orElse(null);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected List<QPossibleValue<Integer>> getAllSourceObjects()
      {
         return List.of(new QPossibleValue<>(102, "Beta"), new QPossibleValue<>(101, "Alpha"));
      }
   }
}
