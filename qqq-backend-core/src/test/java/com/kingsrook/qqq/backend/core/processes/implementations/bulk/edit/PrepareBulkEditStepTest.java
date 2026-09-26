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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for prepare bulk edit step
 *******************************************************************************/
class PrepareBulkEditStepTest extends BaseTest
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
   void testMultipleValuesInDependentField() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().setSecurityKeyValues(Map.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, List.of(true)));
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getField("storeId").withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("storeId", EQUALS, "${input.storeId}")));

      //////////////////////////////
      // insert some test records //
      //////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_ORDER), List.of(
         new QRecord().withValue("id", 1).withValue("storeId", 1),
         new QRecord().withValue("id", 2).withValue("storeId", 2),
         new QRecord().withValue("id", 3).withValue("storeId", 3)
      ));

      RunBackendStepInput input = new RunBackendStepInput();
      input.setTableName(TestUtils.TABLE_NAME_ORDER);
      input.addValue("recordIds", "1,2,3");
      RunBackendStepOutput output = new RunBackendStepOutput();
      new PrepareBulkEditStep().run(input, output);

      assertNotNull(output.getValue("nonDistinctPVSFields"));
      ListingHash<String, String> nonDistinctFields = (ListingHash<String, String>) output.getValue("nonDistinctPVSFields");
      assertNotNull(nonDistinctFields.get("Store Id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSameValueInDependentField() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().setSecurityKeyValues(Map.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, List.of(true)));
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getField("storeId").withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("storeId", EQUALS, "${input.storeId}")));

      //////////////////////////////
      // insert some test records //
      //////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_ORDER), List.of(
         new QRecord().withValue("id", 1).withValue("storeId", 3),
         new QRecord().withValue("id", 2).withValue("storeId", 3),
         new QRecord().withValue("id", 3).withValue("storeId", 3)
      ));

      RunBackendStepInput input = new RunBackendStepInput();
      input.setTableName(TestUtils.TABLE_NAME_ORDER);
      input.addValue("recordIds", "1,2,3");
      RunBackendStepOutput output = new RunBackendStepOutput();
      new PrepareBulkEditStep().run(input, output);

      assertNull(output.getValue("nonDistinctPVSFields"));
      assertNotNull(output.getValue("possibleValueFilterValueStoreId"));
      assertThat(output.getValueInteger("possibleValueFilterValueStoreId")).isEqualTo(3);
   }



   /*******************************************************************************
    ** multiple PVS fields sharing the same filter field (storeId)
    *******************************************************************************/
   @Test
   void testMultiplePvsFieldsUsingSameFilterField_storeId() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      QContext.getQSession().setSecurityKeyValues(Map.of(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, List.of(true)));

      /////////////////////////////////////////////////////////////////////////////////////////
      // Base table filter on the built-in storeId field (already exists on the Order table) //
      /////////////////////////////////////////////////////////////////////////////////////////
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getField("storeId")
         .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("storeId", EQUALS, "${input.storeId}")));
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).getField("storeId").withIsEditable(true);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      // Add 2 NEW PVS-backed fields inline on the table that both depend on the SAME filter field (storeId) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).addField(
         new QFieldMetaData("regionId", QFieldType.INTEGER)
            .withLabel("Region Id")
            .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("storeId", EQUALS, "${input.storeId}")))
      );
      qInstance.getTable(TestUtils.TABLE_NAME_ORDER).addField(
         new QFieldMetaData("storeGroupId", QFieldType.INTEGER)
            .withLabel("Store Group Id")
            .withPossibleValueSourceFilter(new QQueryFilter(new QFilterCriteria("storeId", EQUALS, "${input.storeId}")))
      );

      //////////////////////////////
      // insert some test records //
      //////////////////////////////
      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_ORDER), List.of(
         new QRecord().withValue("id", 1).withValue("storeId", 1),
         new QRecord().withValue("id", 2).withValue("storeId", 2),
         new QRecord().withValue("id", 3).withValue("storeId", 3)
      ));

      RunBackendStepInput input = new RunBackendStepInput();
      input.setTableName(TestUtils.TABLE_NAME_ORDER);
      input.addValue("recordIds", "1,2,3");

      RunBackendStepOutput output = new RunBackendStepOutput();
      assertThatNoException().isThrownBy(() -> new PrepareBulkEditStep().run(input, output));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      // When multiple selected records have different values for the filter field (storeId),               //
      // PrepareBulkEditStep should mark the dependent fields as non-distinct under the filter's label key. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      @SuppressWarnings("unchecked")
      ListingHash<String, String> nonDistinctFields = (ListingHash<String, String>) output.getValue("nonDistinctPVSFields");
      assertNotNull(nonDistinctFields);

      /////////////////////////////////////////////////////////////////////////////////////////////////
      // Keyed by the FILTER field's label "Store Id", should include BOTH dependent fields we added //
      /////////////////////////////////////////////////////////////////////////////////////////////////
      List<String> dependentsForStore = nonDistinctFields.get("Store Id");
      assertNotNull(dependentsForStore);
      assertThat(dependentsForStore).contains("Region Id", "Store Group Id");
   }

}
