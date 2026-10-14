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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.RawReadReason;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test (on the memory backend) for KeysetPager, and for the permission
 ** check and audit of QueryInput.withRawRead.  The database-backed tests live in
 ** the rdbms and postgres modules.
 *******************************************************************************/
class KeysetPagerAndRawReadTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void seed() throws Exception
   {
      new AuditsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, null);

      List<QRecord> records = new ArrayList<>();
      for(int i = 1; i <= 11; i++)
      {
         records.add(new QRecord().withValue("firstName", "First" + i).withValue("lastName", "Last" + i).withValue("noOfShoes", i % 3 == 0 ? null : i % 2));
      }
      TestUtils.insertRecords(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPagingEqualsFullScan() throws QException
   {
      List<QQueryFilter> filters = new ArrayList<>();
      filters.add(null);
      filters.add(new QQueryFilter(new QFilterCriteria("noOfShoes", QCriteriaOperator.EQUALS, 1)));
      filters.add(new QQueryFilter(new QFilterCriteria("noOfShoes", QCriteriaOperator.IS_BLANK)));
      filters.add(new QQueryFilter(new QFilterCriteria("noOfShoes", QCriteriaOperator.EQUALS, 0), new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "First9"))
         .withBooleanOperator(QQueryFilter.BooleanOperator.OR));

      for(QQueryFilter filter : filters)
      {
         for(int pageSize : new int[] { 1, 3, 11, 50 })
         {
            List<QRecord> expected = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(filter == null ? null : filter.clone())).getRecords();
            List<Serializable> expectedKeys = new ArrayList<>(expected.stream().map(r -> r.getValue("id")).toList());
            expectedKeys.sort((a, b) -> ((Integer) a).compareTo((Integer) b));

            List<Serializable> actualKeys = new ArrayList<>();
            KeysetPager pager = KeysetPager.forTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(filter).withPageSize(pageSize);
            pager.forEachPage(page -> page.forEach(r -> actualKeys.add(r.getValue("id"))));
            assertEquals(expectedKeys, actualKeys, "filter " + filter + " page size " + pageSize);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testResume() throws QException
   {
      KeysetPager firstPager = KeysetPager.forTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withPageSize(4);
      firstPager.pageIterator().next();
      assertNull(firstPager.getLastKey());

      List<Serializable> keys = new ArrayList<>();
      KeysetPager.forTable(TestUtils.TABLE_NAME_PERSON_MEMORY).withPageSize(4).startingAfter(8).forEachPage(page -> page.forEach(r -> keys.add(r.getValue("id"))));
      assertEquals(List.of(9, 10, 11), keys);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRawReadNeedsPermissionAndIsAudited() throws QException
   {
      RawReadReason reason = RawReadReason.requiringPermission("nightly backup", "backups.run");

      QContext.setQSession(new QSession());
      assertThrows(QPermissionDeniedException.class, () -> new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRawRead(reason)));
      assertTrue(TestUtils.queryTable("audit").isEmpty(), "a refused raw read is not audited as a read");

      QContext.setQSession(new QSession().withPermission("backups.run"));
      QueryInput input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRawRead(reason);
      assertTrue(input.getShouldFetchHeavyFields());
      assertFalse(input.getShouldOmitHiddenFields());
      assertFalse(input.getShouldMaskPasswords());
      assertFalse(input.getShouldTranslatePossibleValues());
      assertFalse(input.getShouldGenerateDisplayValues());
      assertEquals(11, new QueryAction().execute(input).getRecords().size());

      List<QRecord> audits = TestUtils.queryTable("audit");
      assertEquals(1, audits.size());
      assertTrue(audits.get(0).getValueString("message").contains("nightly backup"));
   }



   /*******************************************************************************
    ** Adding a raw read to an input after the fact is checked just the same, and a
    ** normal query is untouched.
    *******************************************************************************/
   @Test
   void testNormalQueryIsNotARawRead() throws QException
   {
      QueryInput input = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertNull(input.getRawReadReason());
      assertTrue(input.getShouldOmitHiddenFields());
      assertTrue(input.getShouldMaskPasswords());
      assertFalse(input.getShouldFetchHeavyFields());

      new QueryAction().execute(input);
      assertTrue(TestUtils.queryTable("audit").isEmpty());
   }

}
