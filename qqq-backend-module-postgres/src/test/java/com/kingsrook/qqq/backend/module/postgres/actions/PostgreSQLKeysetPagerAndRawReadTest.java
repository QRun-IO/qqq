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

package com.kingsrook.qqq.backend.module.postgres.actions;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.KeysetPager;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.RawReadReason;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.MetaDataWithPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.postgres.BaseTest;
import com.kingsrook.qqq.backend.module.postgres.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Tests (on PostgreSQL, in a Testcontainer) of KeysetPager (equal to a full scan, filters, nulls and
 ** duplicates, resuming, transactions) and of QueryInput.withRawRead.
 *******************************************************************************/
class PostgreSQLKeysetPagerAndRawReadTest extends BaseTest
{
   private static final String TABLE      = "keysetItem";
   private static final String TEXT_TABLE = "keysetTextKeyItem";

   private static final String PERMISSION = "backups.run";

   private static final int ROW_COUNT = 25;



   /*******************************************************************************
    ** 25 rows, with ids 2, 4, 6...50 (gaps), a "grp" that is duplicated across rows
    ** and null for every 5th, and alternating store ids 1 and 2.
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      sql("CREATE TABLE keyset_item(id INT PRIMARY KEY, name VARCHAR(20), grp VARCHAR(5), secret VARCHAR(20), pw VARCHAR(20), payload VARCHAR(40), store_id INT)");
      for(int i = 1; i <= ROW_COUNT; i++)
      {
         String grp = i % 5 == 0 ? "NULL" : ("'g" + (i % 3) + "'");
         sql("INSERT INTO keyset_item VALUES(" + (i * 2) + ",'name" + i + "'," + grp + ",'secret" + i + "','pw" + i + "','heavy" + i + "'," + (i % 2 == 0 ? 2 : 1) + ")");
      }

      QContext.getQInstance().addTable(defineKeysetTable(TABLE, QFieldType.INTEGER, "keyset_item"));
      QContext.getQInstance().addTable(defineKeysetTable(TEXT_TABLE, QFieldType.TEXT, "keyset_text_key_item"));
      sql("CREATE TABLE keyset_text_key_item(id VARCHAR(10) PRIMARY KEY, name VARCHAR(20), grp VARCHAR(5), secret VARCHAR(20), pw VARCHAR(20), payload VARCHAR(40), store_id INT)");
      for(int i = 1; i <= 7; i++)
      {
         sql("INSERT INTO keyset_text_key_item VALUES('k" + i + "','name" + i + "','g','s','p','h',1)");
      }
      new QInstanceValidator().revalidate(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData defineKeysetTable(String name, QFieldType idType, String sqlTable)
   {
      return new QTableMetaData().withName(name).withBackendName(TestUtils.DEFAULT_BACKEND_NAME).withPrimaryKeyField("id")
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName(sqlTable))
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(TestUtils.TABLE_NAME_STORE).withFieldName("storeId"))
         .withField(new QFieldMetaData("id", idType))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("grp", QFieldType.STRING))
         .withField(new QFieldMetaData("secret", QFieldType.STRING).withIsHidden(true))
         .withField(new QFieldMetaData("pw", QFieldType.PASSWORD))
         .withField(new QFieldMetaData("payload", QFieldType.STRING).withIsHeavy(true))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER).withBackendName("store_id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void cleanUp() throws Exception
   {
      sql("DROP TABLE IF EXISTS keyset_item");
      sql("DROP TABLE IF EXISTS keyset_text_key_item");
   }



   /*******************************************************************************
    ** a session that can see every row (and may do a raw read)
    *******************************************************************************/
   private static void useAllAccessSession(boolean withPermission)
   {
      QSession session = new QSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_STORE_ALL_ACCESS, true);
      QContext.setQSession(withPermission ? session.withPermission(PERMISSION) : session);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<Serializable> fullScanKeys(QQueryFilter filter) throws QException
   {
      QQueryFilter scanFilter = filter == null ? new QQueryFilter() : filter.clone();
      scanFilter.setOrderBys(new ArrayList<>(List.of(new QFilterOrderBy("id", true))));
      List<Serializable> keys = new ArrayList<>();
      for(QRecord record : new QueryAction().execute(new QueryInput(TABLE).withFilter(scanFilter)).getRecords())
      {
         keys.add(record.getValue("id"));
      }
      return (keys);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<Serializable> pagedKeys(KeysetPager pager, List<Integer> pageSizesSeen) throws QException
   {
      List<Serializable> keys = new ArrayList<>();
      pager.forEachPage(page ->
      {
         pageSizesSeen.add(page.size());
         page.forEach(r -> keys.add(r.getValue("id")));
      });
      return (keys);
   }



   /*******************************************************************************
    ** paging is the same as one full, ordered scan - for page sizes that divide
    ** the rows, don't, are 1, are exactly the row count, and exceed it.
    *******************************************************************************/
   @Test
   void testPagingEqualsFullScan() throws Exception
   {
      useAllAccessSession(false);
      List<Serializable> expected = fullScanKeys(null);
      assertEquals(ROW_COUNT, expected.size());

      for(int pageSize : new int[] { 1, 5, 7, 24, 25, 26, 1000 })
      {
         List<Integer>      pageSizes = new ArrayList<>();
         List<Serializable> actual    = pagedKeys(KeysetPager.forTable(TABLE).withPageSize(pageSize), pageSizes);
         assertEquals(expected, actual, "page size " + pageSize);
         assertTrue(pageSizes.stream().allMatch(s -> s <= pageSize), "page size " + pageSize);
         assertTrue(pageSizes.subList(0, pageSizes.size() - 1).stream().allMatch(s -> s == pageSize), "all but the last page are full, for page size " + pageSize);
      }
   }



   /*******************************************************************************
    ** filters (including an OR, a duplicated value, and nulls) combine with the key
    ** criteria, and the user's own order-by is replaced by key order.
    *******************************************************************************/
   @Test
   void testPagingWithFilters() throws Exception
   {
      useAllAccessSession(false);

      List<QQueryFilter> filters = List.of(
         new QQueryFilter(new QFilterCriteria("grp", QCriteriaOperator.EQUALS, "g1")),
         new QQueryFilter(new QFilterCriteria("grp", QCriteriaOperator.IS_BLANK)),
         new QQueryFilter(new QFilterCriteria("grp", QCriteriaOperator.NOT_EQUALS, "g1")),
         new QQueryFilter(new QFilterCriteria("grp", QCriteriaOperator.EQUALS, "g0"), new QFilterCriteria("name", QCriteriaOperator.EQUALS, "name3"))
            .withBooleanOperator(QQueryFilter.BooleanOperator.OR),
         new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.BETWEEN, List.of(10, 30))).withOrderBy(new QFilterOrderBy("name", false)),
         new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "no such name")));

      for(QQueryFilter filter : filters)
      {
         for(int pageSize : new int[] { 2, 3 })
         {
            List<Serializable> expected = fullScanKeys(filter);
            List<Serializable> actual   = pagedKeys(KeysetPager.forTable(TABLE).withFilter(filter).withPageSize(pageSize), new ArrayList<>());
            assertEquals(expected, actual, "filter " + filter + " page size " + pageSize);
         }
      }

      assertEquals(5, fullScanKeys(filters.get(1)).size(), "expected the null grp rows to be found");
   }



   /*******************************************************************************
    ** resume after a failure, from the last key of the last page that was handled
    *******************************************************************************/
   @Test
   void testResumeFromLastKey() throws Exception
   {
      useAllAccessSession(false);
      List<Serializable> expected = fullScanKeys(null);

      KeysetPager        firstPager = KeysetPager.forTable(TABLE).withPageSize(10);
      List<Serializable> firstKeys  = new ArrayList<>();
      assertThrows(QException.class, () -> firstPager.forEachPage(page ->
      {
         if(firstKeys.size() >= 20)
         {
            throw new QException("boom, while handling the third page");
         }
         page.forEach(r -> firstKeys.add(r.getValue("id")));
      }));

      assertEquals(expected.subList(0, 20), firstKeys);
      assertEquals(expected.get(19), firstPager.getLastKey());

      List<Serializable> resumedKeys = pagedKeys(KeysetPager.forTable(TABLE).withPageSize(10).startingAfter(firstPager.getLastKey()), new ArrayList<>());
      assertEquals(expected.subList(20, ROW_COUNT), resumedKeys);

      ///////////////////////////////////////////////////////////////////////////////
      // resuming from a key that isn't a row's key (gaps) works the same way, and //
      // from the last key gives no pages                                          //
      ///////////////////////////////////////////////////////////////////////////////
      assertEquals(expected.subList(2, ROW_COUNT), pagedKeys(KeysetPager.forTable(TABLE).withPageSize(4).startingAfter(5), new ArrayList<>()));
      assertTrue(pagedKeys(KeysetPager.forTable(TABLE).startingAfter(expected.get(ROW_COUNT - 1)), new ArrayList<>()).isEmpty());
   }



   /*******************************************************************************
    ** the iterator styles, including how the last key advances
    *******************************************************************************/
   @Test
   void testIterators() throws Exception
   {
      useAllAccessSession(false);
      List<Serializable> expected = fullScanKeys(null);

      List<Serializable> keys = new ArrayList<>();
      Iterator<QRecord>  it   = KeysetPager.forTable(TABLE).withPageSize(4).recordIterator();
      while(it.hasNext())
      {
         keys.add(it.next().getValue("id"));
      }
      assertEquals(expected, keys);

      KeysetPager           pager = KeysetPager.forTable(TABLE).withPageSize(10);
      Iterator<List<QRecord>> pages = pager.pageIterator();
      assertNull(pager.getLastKey());
      assertEquals(10, pages.next().size());
      assertNull(pager.getLastKey(), "the last key advances when the page is moved on from");
      assertEquals(10, pages.next().size());
      assertEquals(expected.get(9), pager.getLastKey());
      assertEquals(5, pages.next().size());
      assertFalse(pages.hasNext());
      assertEquals(expected.get(ROW_COUNT - 1), pager.getLastKey());
      assertEquals(3, pager.getPagesRead());
      assertEquals(ROW_COUNT, pager.getRecordsRead());

      assertFalse(KeysetPager.forTable(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "nope"))).recordIterator().hasNext());
   }



   /*******************************************************************************
    ** every page's query runs inside the supplied transaction
    *******************************************************************************/
   @Test
   void testRunsInsideSuppliedTransaction() throws Exception
   {
      useAllAccessSession(false);

      QBackendTransaction transaction = QBackendTransaction.openFor(new InsertInput(TABLE));
      try
      {
         InsertInput insertInput = new InsertInput(TABLE).withTransaction(transaction);
         insertInput.setRecords(List.of(
            new QRecord().withValue("id", 1001).withValue("name", "uncommitted1").withValue("storeId", 1),
            new QRecord().withValue("id", 1002).withValue("name", "uncommitted2").withValue("storeId", 2)));
         new InsertAction().execute(insertInput);

         List<Serializable> inTransaction = pagedKeys(KeysetPager.forTable(TABLE).withPageSize(10).withTransaction(transaction), new ArrayList<>());
         assertEquals(ROW_COUNT + 2, inTransaction.size());
         assertEquals(1002, inTransaction.get(inTransaction.size() - 1));

         assertEquals(ROW_COUNT, pagedKeys(KeysetPager.forTable(TABLE).withPageSize(10), new ArrayList<>()).size(), "the uncommitted rows aren't visible outside the transaction");
      }
      finally
      {
         transaction.rollback();
         transaction.close();
      }
   }



   /*******************************************************************************
    ** record locks apply to paging (a session with one store key sees only that
    ** store's rows - paging equals a full scan for that session)
    *******************************************************************************/
   @Test
   void testPagingAppliesRecordLocks() throws Exception
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      List<Serializable> expected = fullScanKeys(null);
      assertEquals(13, expected.size());
      assertEquals(expected, pagedKeys(KeysetPager.forTable(TABLE).withPageSize(4), new ArrayList<>()));
   }



   /*******************************************************************************
    ** a primary key that is not orderable gets one streamed query, chunked in pages
    *******************************************************************************/
   @Test
   void testNonOrderablePrimaryKeyFallsBackToStreaming() throws Exception
   {
      useAllAccessSession(false);

      KeysetPager pager = KeysetPager.forTable(TEXT_TABLE).withPageSize(3);
      assertFalse(pager.isPrimaryKeyOrderable());

      List<Integer> pageSizes = new ArrayList<>();
      List<String>  keys      = new ArrayList<>();
      pager.forEachPage(page ->
      {
         pageSizes.add(page.size());
         page.forEach(r -> keys.add(r.getValueString("id")));
      });
      assertEquals(List.of(3, 3, 1), pageSizes);
      assertEquals(7, keys.stream().distinct().count());

      assertThrows(QException.class, () -> KeysetPager.forTable(TEXT_TABLE).startingAfter("k3").forEachPage(PostgreSQLKeysetPagerAndRawReadTest::ignore));
      assertThrows(QException.class, () -> KeysetPager.forTable(TEXT_TABLE).pageIterator());
   }



   /*******************************************************************************
    ** a filter that has its own limit/skip is rejected, as is a bad table, page size
    *******************************************************************************/
   @Test
   void testBadUsage() throws Exception
   {
      useAllAccessSession(false);
      assertThrows(QException.class, () -> KeysetPager.forTable(TABLE).withFilter(new QQueryFilter().withLimit(5)).forEachPage(PostgreSQLKeysetPagerAndRawReadTest::ignore));
      assertThrows(QException.class, () -> KeysetPager.forTable(TABLE).withFilter(new QQueryFilter().withSkip(5)).forEachPage(PostgreSQLKeysetPagerAndRawReadTest::ignore));
      assertThrows(QException.class, () -> KeysetPager.forTable("noSuchTable").forEachPage(PostgreSQLKeysetPagerAndRawReadTest::ignore));
      assertThrows(IllegalArgumentException.class, () -> KeysetPager.forTable(TABLE).withPageSize(0));
   }



   /*******************************************************************************
    ** by default hidden, password, and heavy fields are not available; a raw read
    ** returns all of them - but only to a session with the permission.
    *******************************************************************************/
   @Test
   void testRawReadRequiresPermissionAndReturnsEverything() throws Exception
   {
      useAllAccessSession(true);

      QRecord normal = new QueryAction().execute(new QueryInput(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2)))).getRecords().get(0);
      assertFalse(normal.getValues().containsKey("secret"));
      assertEquals("************", normal.getValue("pw"));
      assertFalse(normal.getValues().containsKey("payload"));

      QueryInput rawInput = new QueryInput(TABLE).withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2)))
         .withRawRead(RawReadReason.requiringPermission("test backup", PERMISSION));
      QRecord raw = new QueryAction().execute(rawInput).getRecords().get(0);
      assertEquals("secret1", raw.getValue("secret"));
      assertEquals("pw1", raw.getValue("pw"));
      assertEquals("heavy1", raw.getValue("payload"));
      assertTrue(raw.getDisplayValues() == null || raw.getDisplayValues().isEmpty());

      ///////////////////////////////////////////////////////////////////////////////////////
      // flipping flags back after withRawRead doesn't undo it (the query re-applies them) //
      ///////////////////////////////////////////////////////////////////////////////////////
      rawInput.setShouldMaskPasswords(true);
      assertEquals("pw1", new QueryAction().execute(rawInput).getRecords().get(0).getValue("pw"));

      //////////////////////////////////////////////////////
      // without the permission, the raw read is refused  //
      //////////////////////////////////////////////////////
      useAllAccessSession(false);
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE)
         .withRawRead(RawReadReason.requiringPermission("test backup", PERMISSION))));
      QContext.setQSession(new QSession().withPermission("some.other.permission"));
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE)
         .withRawRead(RawReadReason.requiringPermission("test backup", PERMISSION))));

      /////////////////////////////////////////////////////////////////////////
      // a custom checker decides, instead of a permission name                 //
      /////////////////////////////////////////////////////////////////////////
      useAllAccessSession(false);
      RawReadReason denying = RawReadReason.checkedBy("custom", (input, metaData) ->
      {
         throw new QPermissionDeniedException("Permission denied.");
      });
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TABLE).withRawRead(denying)));

      RawReadReason allowing = RawReadReason.checkedBy("custom", PostgreSQLKeysetPagerAndRawReadTest::allow);
      assertEquals(ROW_COUNT, new QueryAction().execute(new QueryInput(TABLE).withRawRead(allowing)).getRecords().size());

      assertThrows(IllegalArgumentException.class, () -> RawReadReason.requiringPermission("reason", " "));
      assertThrows(IllegalArgumentException.class, () -> RawReadReason.requiringPermission(" ", PERMISSION));
      assertThrows(IllegalArgumentException.class, () -> RawReadReason.checkedBy("reason", null));
   }



   /*******************************************************************************
    ** a raw read still honors record locks (unless all-access keys), and the pager
    ** can do raw reads.
    *******************************************************************************/
   @Test
   void testRawReadStillAppliesRecordLocks() throws Exception
   {
      RawReadReason reason = RawReadReason.requiringPermission("test backup", PERMISSION);

      QContext.setQSession(new QSession().withPermission(PERMISSION).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 2));
      List<QRecord> storeTwo = new QueryAction().execute(new QueryInput(TABLE).withRawRead(reason)).getRecords();
      assertEquals(12, storeTwo.size());
      assertTrue(storeTwo.stream().allMatch(r -> Integer.valueOf(2).equals(r.getValueInteger("storeId"))));
      assertTrue(storeTwo.stream().allMatch(r -> r.getValue("secret") != null && r.getValue("payload") != null));

      QContext.setQSession(new QSession().withPermission(PERMISSION));
      assertEquals(0, new QueryAction().execute(new QueryInput(TABLE).withRawRead(reason)).getRecords().size(), "no keys, no rows, even raw");

      useAllAccessSession(true);
      assertEquals(ROW_COUNT, new QueryAction().execute(new QueryInput(TABLE).withRawRead(reason)).getRecords().size());

      ///////////////////////////////////////////////////
      // paging raw, with locks, then without the keys //
      ///////////////////////////////////////////////////
      QContext.setQSession(new QSession().withPermission(PERMISSION).withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      List<QRecord> pagedRaw = new ArrayList<>();
      KeysetPager.forTable(TABLE).withPageSize(5).withRawRead(reason).forEachPage(pagedRaw::addAll);
      assertEquals(13, pagedRaw.size());
      assertTrue(pagedRaw.stream().allMatch(r -> r.getValueString("secret").startsWith("secret") && r.getValueString("pw").startsWith("pw") && r.getValueString("payload").startsWith("heavy")));
      assertNotNull(pagedRaw.get(0).getValue("id"));

      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));
      assertThrows(QPermissionDeniedException.class, () -> KeysetPager.forTable(TABLE).withRawRead(reason).forEachPage(PostgreSQLKeysetPagerAndRawReadTest::ignore));
   }



   /*******************************************************************************
    ** a page consumer that does nothing
    *******************************************************************************/
   private static void ignore(List<QRecord> page)
   {
   }



   /*******************************************************************************
    ** a permission checker that allows everything
    *******************************************************************************/
   private static void allow(AbstractActionInput input, MetaDataWithPermissionRules metaData)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void sql(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineBackend()); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }

}
