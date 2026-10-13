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

package com.kingsrook.qqq.backend.core.actions.tables.picker;


import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.QRelatedRecordPicker;
import com.kingsrook.qqq.backend.core.model.metadata.tables.picker.RelatedRecordPickerMode;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.tables.picker.RelatedRecordPickerActionTest.ORDER;
import static com.kingsrook.qqq.backend.core.actions.tables.picker.RelatedRecordPickerActionTest.addPickerTables;
import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationFailureReasonsAllowingExtraReasons;
import static com.kingsrook.qqq.backend.core.instances.QInstanceValidatorTest.assertValidationSuccess;


/*******************************************************************************
 ** QInstanceValidator rules for related-record pickers.
 *******************************************************************************/
class RelatedRecordPickerValidationTest extends BaseTest
{

   /*******************************************************************************
    ** Add the picker tables, then apply a change to the order table's pickers.
    *******************************************************************************/
   private static Consumer<QInstance> with(Consumer<QInstance> change)
   {
      return (qInstance ->
      {
         addPickerTables(qInstance);
         change.accept(qInstance);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRelatedRecordPicker picker(QInstance qInstance, String name)
   {
      return (qInstance.getTable(ORDER).getRelatedRecordPicker(name));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValid()
   {
      assertValidationSuccess(with(qInstance ->
      {
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testModeAttributes()
   {
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "customer").setMode(null)), "customer on table rrp_order: missing mode");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "customer").setFieldName("nope")), "SINGLE needs fieldName");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "customer").setAssociationName("orderLines")), "SINGLE does not take associationName");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setFieldName("customerId")), "MULTI_CHILD does not take fieldName");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setAssociationName("nope")), "associationName nope is not an association");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setChildTargetFieldName("note")), "the target table does not resolve");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "tags").setReviewFieldNames(List.of())), "LINK does not take reviewFieldNames");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "tags").setMaxSelections(0)), "maxSelections must be 1 to 1000");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> qInstance.getTable(ORDER).getRelatedRecordPickers().put("other", new QRelatedRecordPicker().withName("x"))), "missing or inconsistent name");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldReferences()
   {
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setColumns(List.of("nope"))), "column nope is not a field of rrp_sku");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setPreviewFields(List.of("nope"))), "preview field nope");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setThumbnailField("nope")), "thumbnailField nope");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setFilter(new QQueryFilter(new QFilterCriteria("nope", QCriteriaOperator.EQUALS, 1)))), "filter criteria field nope");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "customer").setDefaultFilter(new QQueryFilter().withSubFilter(new QQueryFilter(new QFilterCriteria("nope", QCriteriaOperator.EQUALS, 1))))), "defaultFilter criteria field nope");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setChildFieldDefaults(Map.of("orderId", 1))), "childFieldDefaults key orderId");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setChildValuesFromTarget(Map.of("unitPrice", "nope"))), "childValuesFromTarget value nope");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setReviewFieldNames(List.of("skuId"))), "review field skuId");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> picker(qInstance, "addSkus").setTargetTableName("nope")), "the target table does not resolve");
      assertValidationFailureReasonsAllowingExtraReasons(with(qInstance -> qInstance.getTable(RelatedRecordPickerActionTest.CUSTOMER).setQuickCreateSectionName("nope")), "quickCreateSectionName nope is not a section");
   }



   /*******************************************************************************
    ** The picker map's clone is independent.
    *******************************************************************************/
   @Test
   void testClone()
   {
      QRelatedRecordPicker picker = new QRelatedRecordPicker().withName("a").withMode(RelatedRecordPickerMode.LINK).withColumns(new java.util.ArrayList<>(List.of("x")))
         .withFilter(new QQueryFilter(new QFilterCriteria("x", QCriteriaOperator.EQUALS, 1)));
      QRelatedRecordPicker clone = picker.clone();
      clone.getColumns().add("y");
      org.junit.jupiter.api.Assertions.assertEquals(1, picker.getColumns().size());
   }
}
