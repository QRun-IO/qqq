/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.model.metadata.tables;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import com.kingsrook.qqq.api.BaseTest;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.actions.QRecordApiAdapter;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Replacement names address current table fields directly, independently of
 ** their API exposure. They are not a chain of historical API field aliases.
 *******************************************************************************/
class ApiReplacementFieldValidationTest extends BaseTest
{
   /*******************************************************************************
    ** Each test changes metadata under the same module-fixture API names.
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void clearCaches()
   {
      GetTableApiFieldsAction.clearCaches();
   }



   /*******************************************************************************
    ** A typo on a historical field must fail instance validation before serving.
    *******************************************************************************/
   @Test
   void testMissingRemovedFieldTargetIsRejected()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON);
      QFieldMetaData field = ApiTableMetaDataContainer.of(table).getApiTableMetaData(TestUtils.API_NAME).getRemovedApiFields().get(0);
      ApiFieldMetaDataContainer.of(field).getApiFieldMetaData(TestUtils.API_NAME).setReplacedByFieldName("missingTarget");
      assertInvalidTarget("shoeCount", "missingTarget");
   }



   /*******************************************************************************
    ** The same mapping property on a current field has the same target contract.
    *******************************************************************************/
   @Test
   void testMissingCurrentFieldTargetIsRejected()
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getField("noOfShoes")
         .withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(TestUtils.API_NAME,
            new ApiFieldMetaData().withInitialVersion(TestUtils.V2023_Q1).withReplacedByFieldName("missingTarget")));
      assertInvalidTarget("noOfShoes", "missingTarget");
   }



   /*******************************************************************************
    ** Revalidation must inspect current metadata even after field maps are cached.
    *******************************************************************************/
   @Test
   void testReplacementRevalidationUsesCurrentMetadata()
   {
      assertDoesNotThrow(() -> new QInstanceValidator().revalidate(QContext.getQInstance()));
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getFields().put("noOfShoes",
         new QFieldMetaData("noOfShoes", QFieldType.INTEGER).withSupplementalMetaData(new ApiFieldMetaDataContainer()
            .withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion(TestUtils.V2023_Q1).withReplacedByFieldName("missingTarget"))));
      assertInvalidTarget("noOfShoes", "missingTarget");
   }



   /*******************************************************************************
    ** The physical target can be absent from the historical version's API shape.
    *******************************************************************************/
   @Test
   void testHistoricalReplacementMayTargetFieldHiddenInRequestedVersion() throws Exception
   {
      assertDoesNotThrow(() -> new QInstanceValidator().revalidate(QContext.getQInstance()));
      Map<String, Serializable> old = QRecordApiAdapter.qRecordToApiMap(TestUtils.getTim2ShoesRecord(), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4);
      assertEquals(2, old.get("shoeCount"));
      assertFalse(old.containsKey("noOfShoes"));
      QRecord input = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("{\"shoeCount\":7}"), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4, false);
      assertEquals(7, input.getValueInteger("noOfShoes"));
   }



   /*******************************************************************************
    ** A target field may have its own mapping, but adapters still address its
    ** physical value directly instead of recursively applying that mapping.
    *******************************************************************************/
   @Test
   void testReplacementDoesNotTraverseAnotherFieldsMapping() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON);
      ApiFieldMetaDataContainer.of(table.getField("noOfShoes")).getApiFieldMetaData(TestUtils.API_NAME).setReplacedByFieldName("cost");
      assertDoesNotThrow(() -> new QInstanceValidator().revalidate(QContext.getQInstance()));
      QRecord record = TestUtils.getTim2ShoesRecord();
      assertEquals(2, QRecordApiAdapter.qRecordToApiMap(record, table.getName(), TestUtils.API_NAME, TestUtils.V2022_Q4).get("shoeCount"));
      assertEquals(new BigDecimal("3.50"), QRecordApiAdapter.qRecordToApiMap(record, table.getName(), TestUtils.API_NAME, TestUtils.V2023_Q1).get("noOfShoes"));
      QRecord input = QRecordApiAdapter.apiJsonObjectToQRecord(new JSONObject("{\"shoeCount\":7}"), table.getName(), TestUtils.API_NAME, TestUtils.V2022_Q4, false);
      assertEquals(7, input.getValueInteger("noOfShoes"));
      assertFalse(input.getValues().containsKey("cost"));
   }



   /*******************************************************************************
    ** A removed alias alone is not a physical target; no chain resolver exists.
    *******************************************************************************/
   @Test
   void testRemovedToRemovedReplacementIsRejected()
   {
      ApiTableMetaDataContainer.of(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)).getApiTableMetaData(TestUtils.API_NAME)
         .withRemovedApiField(new QFieldMetaData("oldShoeCount", QFieldType.INTEGER).withSupplementalMetaData(new ApiFieldMetaDataContainer()
            .withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion(TestUtils.V2022_Q4)
               .withFinalVersion(TestUtils.V2022_Q4).withReplacedByFieldName("shoeCount"))));
      assertInvalidTarget("oldShoeCount", "shoeCount");
   }



   /*******************************************************************************
    ** Derived API values use the custom mapper and need no replacement target.
    *******************************************************************************/
   @Test
   void testCustomMappedRemovedFieldNeedsNoReplacementTarget() throws Exception
   {
      ApiTableMetaDataContainer.of(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON)).getApiTableMetaData(TestUtils.API_NAME)
         .withRemovedApiField(new QFieldMetaData("shoeDescription", QFieldType.STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer()
            .withApiFieldMetaData(TestUtils.API_NAME, new ApiFieldMetaData().withInitialVersion(TestUtils.V2022_Q4)
               .withFinalVersion(TestUtils.V2022_Q4).withCustomValueMapper(new QCodeReference(ShoeDescriptionMapper.class)))));
      assertDoesNotThrow(() -> new QInstanceValidator().revalidate(QContext.getQInstance()));
      assertEquals("2 shoes", QRecordApiAdapter.qRecordToApiMap(TestUtils.getTim2ShoesRecord(), TestUtils.TABLE_NAME_PERSON, TestUtils.API_NAME, TestUtils.V2022_Q4).get("shoeDescription"));
   }



   /*******************************************************************************
    ** A useful startup error identifies the table, API, source and missing target.
    *******************************************************************************/
   private void assertInvalidTarget(String source, String target)
   {
      QInstanceValidationException error = assertThrows(QInstanceValidationException.class,
         () -> new QInstanceValidator().revalidate(QContext.getQInstance()));
      assertTrue(error.getMessage().contains(TestUtils.TABLE_NAME_PERSON));
      assertTrue(error.getMessage().contains(TestUtils.API_NAME));
      assertTrue(error.getMessage().contains(source));
      assertTrue(error.getMessage().contains(target));
   }



   /*******************************************************************************
    ** Application-defined computed value, not a replacement-field alias.
    *******************************************************************************/
   public static class ShoeDescriptionMapper extends ApiFieldCustomValueMapper
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Serializable produceApiValue(QRecord record, String apiFieldName)
      {
         return record.getValueInteger("noOfShoes") + " shoes";
      }
   }
}
