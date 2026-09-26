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
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for FilterValidationHelper
 *******************************************************************************/
class FilterValidationHelperTest extends BaseTest
{

   /*******************************************************************************
    ** Aggregate sorts carry their field in a typed payload, not the base property.
    *******************************************************************************/
   @Test
   void testAggregateOrderingValidatesActualFields()
   {
      AggregateInput input = new AggregateInput();
      input.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withVirtualField(new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
            .withIsQuerySelectable(true).withIsQueryCriteria(false)
            .withFieldFunction(new FieldFunction().withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER).withFieldName("firstName")));
      for(QFilterOrderBy orderBy : List.of(
         new QFilterOrderByAggregate(new Aggregate("noOfShoes", AggregateOperator.SUM)),
         new QFilterOrderByGroupBy(new GroupBy(QFieldType.STRING, "lastName")),
         new QFilterOrderByAggregate(new Aggregate("firstNameLength", AggregateOperator.SUM)),
         new QFilterOrderByGroupBy(new GroupBy(QFieldType.INTEGER, "firstNameLength"))))
      {
         input.setFilter(new QQueryFilter().withOrderBy(orderBy));
         assertDoesNotThrow(() -> FilterValidationHelper.validateFieldNamesInFilter(input));
      }
      for(QFilterOrderBy orderBy : List.of(new QFilterOrderByAggregate(), new QFilterOrderByGroupBy(),
         new QFilterOrderByAggregate().withFieldName("id"), new QFilterOrderByGroupBy().withFieldName("id"),
         new QFilterOrderByAggregate(new Aggregate("missingAggregateField", AggregateOperator.SUM)),
         new QFilterOrderByGroupBy(new GroupBy(QFieldType.STRING, "missingGroupByField"))))
      {
         input.setFilter(new QQueryFilter().withOrderBy(orderBy));
         assertThrows(QException.class, () -> FilterValidationHelper.validateFieldNamesInFilter(input));
      }
   }



   /*******************************************************************************
    ** Failed personalization must not expose the original joined table's fields.
    *******************************************************************************/
   @Test
   void testJoinedPersonalizationFailureRejectsFilter() throws Exception
   {
      AtomicBoolean failPersonalization = new AtomicBoolean();
      QContext.setObject("failJoinedPersonalization", failPersonalization);
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE,
         new QCodeReference(FailingJoinedPersonalizer.class));

      QQueryFilter filter = new QQueryFilter().withSubFilter(new QQueryFilter(new QFilterCriteria("line.sku", QCriteriaOperator.EQUALS, "BASIC1"))
         .withCriteria(new QFilterCriteria().withFieldName("id").withOperator(QCriteriaOperator.EQUALS).withOtherFieldName("line.quantity")))
         .withOrderBy(new QFilterOrderBy("line.id"));
      List<QueryJoin> joins = List.of(new QueryJoin(TestUtils.TABLE_NAME_LINE_ITEM).withAlias("line").withSelect(false));
      QueryInput queryInput = new QueryInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.USER).withFilter(filter).withQueryJoins(joins);
      CountInput countInput = new CountInput(TestUtils.TABLE_NAME_ORDER).withInputSource(QInputSource.USER).withFilter(filter).withQueryJoins(joins);
      assertDoesNotThrow(() -> FilterValidationHelper.validateFieldNamesInFilter(queryInput));
      assertDoesNotThrow(() -> FilterValidationHelper.validateFieldNamesInFilter(countInput));

      failPersonalization.set(true);
      String expected = "Query Filter contained 3 unrecognized field names: line.id,line.sku,line.quantity";
      assertAll(
         () -> assertEquals(expected, assertThrows(QUserFacingException.class, () -> FilterValidationHelper.validateFieldNamesInFilter(queryInput)).getMessage()),
         () -> assertEquals(expected, assertThrows(QUserFacingException.class, () -> FilterValidationHelper.validateFieldNamesInFilter(countInput)).getMessage()));

      queryInput.setInputSource(QInputSource.SYSTEM);
      countInput.setInputSource(QInputSource.SYSTEM);
      assertDoesNotThrow(() -> FilterValidationHelper.validateFieldNamesInFilter(queryInput));
      assertDoesNotThrow(() -> FilterValidationHelper.validateFieldNamesInFilter(countInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRegularFieldInFilterIsAccepted() throws Exception
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Darin")));

      assertDoesNotThrow(() -> FilterValidationHelper.validateFieldNamesInFilter(queryInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVirtualFieldWithQueryCriteriaAllowedInFilter() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withVirtualField(new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("firstName")));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("firstNameLength", QCriteriaOperator.EQUALS, List.of(5))));

      assertDoesNotThrow(() -> FilterValidationHelper.validateFieldNamesInFilter(queryInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testVirtualFieldWithoutQueryCriteriaRejectedInFilter() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.withVirtualField(new QVirtualFieldMetaData("firstNameLength", QFieldType.INTEGER)
         .withIsQueryCriteria(false)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("firstName")));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("firstNameLength", QCriteriaOperator.EQUALS, List.of(5))));

      assertThrows(QUserFacingException.class, () -> FilterValidationHelper.validateFieldNamesInFilter(queryInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownFieldInFilterIsRejected()
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria("noSuchField", QCriteriaOperator.EQUALS, "x")));

      assertThrows(QUserFacingException.class, () -> FilterValidationHelper.validateFieldNamesInFilter(queryInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinTableVirtualFieldWithQueryCriteriaAllowedInFilter()
   {
      QTableMetaData lineItemTable = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM);
      lineItemTable.withVirtualField(new QVirtualFieldMetaData("skuLength", QFieldType.INTEGER)
         .withIsQueryCriteria(true)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("sku")));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setQueryJoins(List.of(new QueryJoin().withJoinTable(TestUtils.TABLE_NAME_LINE_ITEM).withSelect(true)));
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_LINE_ITEM + ".skuLength", QCriteriaOperator.EQUALS, List.of(5))));

      assertDoesNotThrow(() -> FilterValidationHelper.validateFieldNamesInFilter(queryInput));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testJoinTableVirtualFieldWithoutQueryCriteriaRejectedInFilter()
   {
      QTableMetaData lineItemTable = QContext.getQInstance().getTable(TestUtils.TABLE_NAME_LINE_ITEM);
      lineItemTable.withVirtualField(new QVirtualFieldMetaData("skuLength", QFieldType.INTEGER)
         .withIsQueryCriteria(false)
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
            .withFieldName("sku")));

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(TestUtils.TABLE_NAME_ORDER);
      queryInput.setQueryJoins(List.of(new QueryJoin().withJoinTable(TestUtils.TABLE_NAME_LINE_ITEM).withSelect(true)));
      queryInput.setFilter(new QQueryFilter(new QFilterCriteria(TestUtils.TABLE_NAME_LINE_ITEM + ".skuLength", QCriteriaOperator.EQUALS, List.of(5))));

      assertThrows(QUserFacingException.class, () -> FilterValidationHelper.validateFieldNamesInFilter(queryInput));
   }



   /*******************************************************************************
    ** The production personalizer loader uses a class reference.
    *******************************************************************************/
   public static class FailingJoinedPersonalizer implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input) throws QException
      {
         AtomicBoolean fail = (AtomicBoolean) QContext.getObject("failJoinedPersonalization");
         if(fail.get() && TestUtils.TABLE_NAME_LINE_ITEM.equals(input.getTableName()) && QInputSource.USER.equals(input.getInputSource()))
         {
            throw new QException("Personalization unavailable");
         }
         return input.getTable();
      }
   }
}
