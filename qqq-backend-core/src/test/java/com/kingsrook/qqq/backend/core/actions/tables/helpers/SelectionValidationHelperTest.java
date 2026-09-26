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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for SelectionValidationHelper
 *******************************************************************************/
class SelectionValidationHelperTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRegularFieldIsSelectable() throws Exception
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);

      List<String> unrecognized = SelectionValidationHelper.getUnrecognizedFieldNames(queryInput, Set.of("firstName", "lastName"));
      assertTrue(unrecognized.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVirtualFieldWithQuerySelectableIsAccepted() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withVirtualField(new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("firstName")));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);

      List<String> unrecognized = SelectionValidationHelper.getUnrecognizedFieldNames(queryInput, Set.of("firstName", "firstNameLength"));
      assertTrue(unrecognized.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVirtualFieldWithoutQuerySelectableIsRejected() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withVirtualField(new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withIsQuerySelectable(false)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("firstName")));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);

      List<String> unrecognized = SelectionValidationHelper.getUnrecognizedFieldNames(queryInput, Set.of("firstNameLength"));
      assertEquals(1, unrecognized.size());
      assertEquals("firstNameLength", unrecognized.get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownFieldIsRejected() throws Exception
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);

      List<String> unrecognized = SelectionValidationHelper.getUnrecognizedFieldNames(queryInput, Set.of("noSuchField"));
      assertEquals(1, unrecognized.size());
      assertEquals("noSuchField", unrecognized.get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinTableVirtualFieldWithQuerySelectableIsAccepted() throws Exception
   {
      QTableMetaData lineItemTable = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM);
      lineItemTable.withVirtualField(new QVirtualFieldMetaData("skuLength", QFieldType.INTEGER)
         .withIsQuerySelectable(true)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("sku")));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setQueryJoins(List.of(new QueryJoin().withJoinTable(TestUtils.TABLE_NAME_LINE_ITEM).withSelect(true)));

      List<String> unrecognized = SelectionValidationHelper.getUnrecognizedFieldNames(queryInput, Set.of(TestUtils.TABLE_NAME_LINE_ITEM + ".skuLength"));
      assertTrue(unrecognized.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinTableVirtualFieldWithoutQuerySelectableIsRejected() throws Exception
   {
      QTableMetaData lineItemTable = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM);
      lineItemTable.withVirtualField(new QVirtualFieldMetaData("skuLength", QFieldType.INTEGER)
         .withIsQuerySelectable(false)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("sku")));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setQueryJoins(List.of(new QueryJoin().withJoinTable(TestUtils.TABLE_NAME_LINE_ITEM).withSelect(true)));

      List<String> unrecognized = SelectionValidationHelper.getUnrecognizedFieldNames(queryInput, Set.of(TestUtils.TABLE_NAME_LINE_ITEM + ".skuLength"));
      assertEquals(1, unrecognized.size());
      assertEquals(TestUtils.TABLE_NAME_LINE_ITEM + ".skuLength", unrecognized.get(0));
   }

}
