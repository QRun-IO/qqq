/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for BulkInsertPrepareValueMappingStep 
 *******************************************************************************/
class BulkInsertPrepareValueMappingStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      assertEquals(TestUtils.TABLE_NAME_ORDER, BulkInsertPrepareValueMappingStep.getTableAndField(TestUtils.TABLE_NAME_ORDER, "orderNo").table().getName());
      assertEquals("orderNo", BulkInsertPrepareValueMappingStep.getTableAndField(TestUtils.TABLE_NAME_ORDER, "orderNo").field().getName());

      assertEquals(TestUtils.TABLE_NAME_LINE_ITEM, BulkInsertPrepareValueMappingStep.getTableAndField(TestUtils.TABLE_NAME_ORDER, "orderLine.sku").table().getName());
      assertEquals("sku", BulkInsertPrepareValueMappingStep.getTableAndField(TestUtils.TABLE_NAME_ORDER, "orderLine.sku").field().getName());

      assertEquals(TestUtils.TABLE_NAME_LINE_ITEM_EXTRINSIC, BulkInsertPrepareValueMappingStep.getTableAndField(TestUtils.TABLE_NAME_ORDER, "orderLine.extrinsics.key").table().getName());
      assertEquals("key", BulkInsertPrepareValueMappingStep.getTableAndField(TestUtils.TABLE_NAME_ORDER, "orderLine.extrinsics.key").field().getName());

   }

}