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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Regression coverage for ConditionalUpdateAction.
 *******************************************************************************/
class ConditionalUpdateActionTest extends BaseTest
{

   /*******************************************************************************
    ** Only matching rows are changed and the returned count matches the update.
    *******************************************************************************/
   @Test
   void updatesOnlyMatchesAndReturnsCount() throws Exception
   {
      Integer first = insert("first");
      Integer second = insert("second");
      Integer untouched = insert("untouched");
      int count = new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(first, second))))
         .withValue("lastName", "changed"));
      assertThat(count).isEqualTo(2);
      assertThat(GetAction.execute(TABLE, first).getValueString("lastName")).isEqualTo("changed");
      assertThat(GetAction.execute(TABLE, second).getValueString("lastName")).isEqualTo("changed");
      assertThat(GetAction.execute(TABLE, untouched).getValueString("lastName")).isEqualTo("original");
      assertThat(new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE)
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -1)))
         .withValue("lastName", "absent"))).isZero();
   }



   /*******************************************************************************
    ** Sixteen simultaneous compare-and-set updates produce exactly one winner.
    *******************************************************************************/
   @Test
   void sixteenContendersHaveExactlyOneWinner() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      instance.getTable(TABLE).withField(new QFieldMetaData("version", QFieldType.INTEGER));
      Integer id = insert("contended");
      new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE).withFilter(idFilter(id)).withValue("version", 0));
      CountDownLatch ready = new CountDownLatch(16);
      CountDownLatch start = new CountDownLatch(1);
      var executor = Executors.newFixedThreadPool(16);
      try
      {
         List<Future<Integer>> results = new ArrayList<>();
         for(int i = 0; i < 16; i++)
         {
            results.add(executor.submit(() ->
            {
               QContext.init(instance, newSession());
               try
               {
                  ready.countDown();
                  assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                  return (new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE)
                     .withFilter(idFilter(id).withCriteria(new QFilterCriteria("version", QCriteriaOperator.EQUALS, 0)))
                     .withValue("version", 1)));
               }
               finally
               {
                  QContext.clear();
               }
            }));
         }
         assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
         start.countDown();
         List<Integer> counts = new ArrayList<>();
         for(Future<Integer> result : results)
         {
            counts.add(result.get(15, TimeUnit.SECONDS));
         }
         assertThat(counts).containsOnly(0, 1);
         assertThat(counts.stream().filter(count -> count == 1).count()).isEqualTo(1);
         assertThat(GetAction.execute(TABLE, id).getValueInteger("version")).isEqualTo(1);
      }
      finally
      {
         start.countDown();
         executor.shutdownNow();
         assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      }
   }



   /*******************************************************************************
    ** An absent or empty filter cannot update the entire table accidentally.
    *******************************************************************************/
   @Test
   void emptyFilterIsRejected() throws Exception
   {
      assertThatThrownBy(() -> new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE).withValue("lastName", "unsafe")))
         .isInstanceOf(QException.class).hasMessageContaining("filter");
      assertThatThrownBy(() -> new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE).withFilter(new QQueryFilter()).withValue("lastName", "unsafe")))
         .isInstanceOf(QException.class).hasMessageContaining("filter");
   }



   /*******************************************************************************
    ** A conditional update must have at least one value to assign.
    *******************************************************************************/
   @Test
   void emptyValuesAreRejected() throws Exception
   {
      assertThatThrownBy(() -> new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE).withFilter(idFilter(1))))
         .isInstanceOf(QException.class).hasMessageContaining("value");
      assertThatThrownBy(() -> new ConditionalUpdateAction().execute(new ConditionalUpdateInput(TABLE).withFilter(idFilter(1)).withValues(null)))
         .isInstanceOf(QException.class).hasMessageContaining("value");
   }



   /*******************************************************************************
    ** The memory backend clock lies between the surrounding clock reads.
    *******************************************************************************/
   @Test
   void databaseClockIsCurrent() throws Exception
   {
      Instant before = Instant.now();
      Instant actual = new ConditionalUpdateAction().databaseNow(TABLE);
      assertThat(actual).isBetween(before, Instant.now());
   }



   /*******************************************************************************
    ** The memory backend advertises conditional update support.
    *******************************************************************************/
   @Test
   void memorySupportsConditionalUpdates() throws Exception
   {
      assertThat(ConditionalUpdateAction.isSupported(QContext.getQInstance().getBackendForTable(TABLE))).isTrue();
   }



   private static final String TABLE = TestUtils.TABLE_NAME_PERSON_MEMORY;



   /*******************************************************************************
    ** Insert a distinct person and retain its generated primary key.
    *******************************************************************************/
   private Integer insert(String name) throws QException
   {
      return (new InsertAction().execute(new InsertInput(TABLE).withRecord(new QRecord()
         .withValue("firstName", name).withValue("lastName", "original").withValue("email", name + "@example.test")))
         .getRecords().get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Restrict a conditional update to one primary key.
    *******************************************************************************/
   private static QQueryFilter idFilter(Integer id)
   {
      return (new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, id)));
   }
}

