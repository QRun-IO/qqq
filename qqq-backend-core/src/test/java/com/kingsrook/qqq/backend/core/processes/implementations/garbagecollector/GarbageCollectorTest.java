/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.garbagecollector;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.NowWithOffset;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for GarbageCollectorTransformStep 
 *******************************************************************************/
class GarbageCollectorTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBasic() throws QException
   {
      QProcessMetaData process = GarbageCollectorProcessMetaDataProducer.createProcess(TestUtils.TABLE_NAME_PERSON_MEMORY, "timestamp", NowWithOffset.minus(30, ChronoUnit.DAYS), null);
      QContext.getQInstance().addProcess(process);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(getPersonRecords()));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(process.getName());
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      new RunProcessAction().execute(input);

      QueryOutput queryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(new QQueryFilter()));
      assertEquals(2, queryOutput.getRecords().size());
      assertEquals(Set.of(4, 5), queryOutput.getRecords().stream().map(r -> r.getValueInteger("id")).collect(Collectors.toSet()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> getPersonRecords()
   {
      List<QRecord> records = List.of(
         new QRecord().withValue("id", 1).withValue("timestamp", Instant.now().minus(90, ChronoUnit.DAYS)),
         new QRecord().withValue("id", 2).withValue("timestamp", Instant.now().minus(31, ChronoUnit.DAYS)),
         new QRecord().withValue("id", 3).withValue("timestamp", Instant.now().minus(30, ChronoUnit.DAYS).minus(5, ChronoUnit.MINUTES)),
         new QRecord().withValue("id", 4).withValue("timestamp", Instant.now().minus(29, ChronoUnit.DAYS).minus(23, ChronoUnit.HOURS)),
         new QRecord().withValue("id", 5).withValue("timestamp", Instant.now().minus(5, ChronoUnit.DAYS)));
      return records;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOverrideDate() throws QException
   {
      QProcessMetaData process = GarbageCollectorProcessMetaDataProducer.createProcess(TestUtils.TABLE_NAME_PERSON_MEMORY, "timestamp", NowWithOffset.minus(30, ChronoUnit.DAYS), null);
      QContext.getQInstance().addProcess(process);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(getPersonRecords()));

      ///////////////////////////////////////////////////////////////
      // run with a limit of 100 days ago, and 0 should be deleted //
      ///////////////////////////////////////////////////////////////
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(process.getName());
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.addValue("limitDate", Instant.now().minus(100, ChronoUnit.DAYS));
      new RunProcessAction().execute(input);

      QueryOutput queryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(new QQueryFilter()));
      assertEquals(5, queryOutput.getRecords().size());

      ///////////////////////////////////////////////////
      // re-run with 10 days, and all but 1 be deleted //
      ///////////////////////////////////////////////////
      input.addValue("limitDate", Instant.now().minus(10, ChronoUnit.DAYS));
      new RunProcessAction().execute(input);

      queryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(new QQueryFilter()));
      assertEquals(1, queryOutput.getRecords().size());

      ///////////////////////////////////////////////
      // re-run with 1 day, and all end up deleted //
      ///////////////////////////////////////////////
      input.addValue("limitDate", Instant.now().minus(1, ChronoUnit.DAYS));
      new RunProcessAction().execute(input);

      queryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(new QQueryFilter()));
      assertEquals(0, queryOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithDeleteAllJoins() throws QException
   {
      QProcessMetaData process = GarbageCollectorProcessMetaDataProducer.createProcess(TestUtils.TABLE_NAME_ORDER, "timestamp", NowWithOffset.minus(30, ChronoUnit.DAYS), "*");
      QContext.getQInstance().addProcess(process);

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(getOrderRecords()));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM).withRecords(getLineItemRecords()));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).withRecords(getLineItemExtrinsicRecords()));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(process.getName());
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      new RunProcessAction().execute(input);

      QueryOutput orderQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_ORDER).withFilter(new QQueryFilter()));
      assertEquals(2, orderQueryOutput.getRecords().size());
      assertEquals(Set.of(4, 5), orderQueryOutput.getRecords().stream().map(r -> r.getValueInteger("id")).collect(Collectors.toSet()));

      QueryOutput lineItemQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_LINE_ITEM).withFilter(new QQueryFilter()));
      assertEquals(9, lineItemQueryOutput.getRecords().size());
      assertEquals(Set.of(4, 5), lineItemQueryOutput.getRecords().stream().map(r -> r.getValueInteger("orderId")).collect(Collectors.toSet()));

      QueryOutput lineItemExtrinsicQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).withFilter(new QQueryFilter()));
      assertEquals(5, lineItemExtrinsicQueryOutput.getRecords().size());
      assertEquals(Set.of(7, 9, 11, 13, 15), lineItemExtrinsicQueryOutput.getRecords().stream().map(r -> r.getValueInteger("lineItemId")).collect(Collectors.toSet()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithDeleteSomeJoins() throws QException
   {
      QProcessMetaData process = GarbageCollectorProcessMetaDataProducer.createProcess(TestUtils.TABLE_NAME_ORDER, "timestamp", NowWithOffset.minus(30, ChronoUnit.DAYS), TestUtils.TABLE_NAME_LINE_ITEM);
      QContext.getQInstance().addProcess(process);

      //////////////////////////////////////////////////////////////////////////
      // remove table's associations - as they implicitly cascade the delete! //
      //////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER).withAssociations(new ArrayList<>());
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM).withAssociations(new ArrayList<>());

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(getOrderRecords()));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM).withRecords(getLineItemRecords()));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).withRecords(getLineItemExtrinsicRecords()));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(process.getName());
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      new RunProcessAction().execute(input);

      QueryOutput orderQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_ORDER).withFilter(new QQueryFilter()));
      assertEquals(2, orderQueryOutput.getRecords().size());
      assertEquals(Set.of(4, 5), orderQueryOutput.getRecords().stream().map(r -> r.getValueInteger("id")).collect(Collectors.toSet()));

      QueryOutput lineItemQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_LINE_ITEM).withFilter(new QQueryFilter()));
      assertEquals(9, lineItemQueryOutput.getRecords().size());
      assertEquals(Set.of(4, 5), lineItemQueryOutput.getRecords().stream().map(r -> r.getValueInteger("orderId")).collect(Collectors.toSet()));

      QueryOutput lineItemExtrinsicQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).withFilter(new QQueryFilter()));
      assertEquals(8, lineItemExtrinsicQueryOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithDeleteNoJoins() throws QException
   {
      QProcessMetaData process = GarbageCollectorProcessMetaDataProducer.createProcess(TestUtils.TABLE_NAME_ORDER, "timestamp", NowWithOffset.minus(30, ChronoUnit.DAYS), null);
      QContext.getQInstance().addProcess(process);

      ////////////////////////////////////////////////////////////////////////////////
      // remove order table's associations - as they implicitly cascade the delete! //
      ////////////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER).withAssociations(new ArrayList<>());

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);

      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecords(getOrderRecords()));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM).withRecords(getLineItemRecords()));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).withRecords(getLineItemExtrinsicRecords()));

      RunProcessInput input = new RunProcessInput();
      input.setProcessName(process.getName());
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      new RunProcessAction().execute(input);

      QueryOutput orderQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_ORDER).withFilter(new QQueryFilter()));
      assertEquals(2, orderQueryOutput.getRecords().size());
      assertEquals(Set.of(4, 5), orderQueryOutput.getRecords().stream().map(r -> r.getValueInteger("id")).collect(Collectors.toSet()));

      QueryOutput lineItemQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_LINE_ITEM).withFilter(new QQueryFilter()));
      assertEquals(15, lineItemQueryOutput.getRecords().size());

      QueryOutput lineItemExtrinsicQueryOutput = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC).withFilter(new QQueryFilter()));
      assertEquals(8, lineItemExtrinsicQueryOutput.getRecords().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> getOrderRecords()
   {
      List<QRecord> records = List.of(
         new QRecord().withValue("id", 1).withValue("timestamp", Instant.now().minus(90, ChronoUnit.DAYS)),
         new QRecord().withValue("id", 2).withValue("timestamp", Instant.now().minus(31, ChronoUnit.DAYS)),
         new QRecord().withValue("id", 3).withValue("timestamp", Instant.now().minus(30, ChronoUnit.DAYS).minus(5, ChronoUnit.MINUTES)),
         new QRecord().withValue("id", 4).withValue("timestamp", Instant.now().minus(29, ChronoUnit.DAYS).minus(23, ChronoUnit.HOURS)),
         new QRecord().withValue("id", 5).withValue("timestamp", Instant.now().minus(5, ChronoUnit.DAYS)));
      return records;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> getLineItemRecords()
   {
      List<QRecord> records = List.of(
         new QRecord().withValue("id", 1).withValue("orderId", 1),
         new QRecord().withValue("id", 2).withValue("orderId", 2),
         new QRecord().withValue("id", 3).withValue("orderId", 2),
         new QRecord().withValue("id", 4).withValue("orderId", 3),
         new QRecord().withValue("id", 5).withValue("orderId", 3),
         new QRecord().withValue("id", 6).withValue("orderId", 3),
         new QRecord().withValue("id", 7).withValue("orderId", 4),
         new QRecord().withValue("id", 8).withValue("orderId", 4),
         new QRecord().withValue("id", 9).withValue("orderId", 4),
         new QRecord().withValue("id", 10).withValue("orderId", 4),
         new QRecord().withValue("id", 11).withValue("orderId", 5),
         new QRecord().withValue("id", 12).withValue("orderId", 5),
         new QRecord().withValue("id", 13).withValue("orderId", 5),
         new QRecord().withValue("id", 14).withValue("orderId", 5),
         new QRecord().withValue("id", 15).withValue("orderId", 5));

      return records;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> getLineItemExtrinsicRecords()
   {
      List<QRecord> records = List.of(
         new QRecord().withValue("id", 1).withValue("lineItemId", 1),
         new QRecord().withValue("id", 2).withValue("lineItemId", 3),
         new QRecord().withValue("id", 3).withValue("lineItemId", 5),
         new QRecord().withValue("id", 4).withValue("lineItemId", 7),
         new QRecord().withValue("id", 5).withValue("lineItemId", 9),
         new QRecord().withValue("id", 6).withValue("lineItemId", 11),
         new QRecord().withValue("id", 7).withValue("lineItemId", 13),
         new QRecord().withValue("id", 8).withValue("lineItemId", 15));

      return records;
   }

}