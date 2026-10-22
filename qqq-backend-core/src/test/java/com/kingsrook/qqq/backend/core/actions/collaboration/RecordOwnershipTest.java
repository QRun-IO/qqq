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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.CurrentUser;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordOwnership;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Ownership publication follows resolved feature inheritance and validation.
 *******************************************************************************/
class RecordOwnershipTest extends BaseTest
{
   /*******************************************************************************
    ** Either ownership field can independently enable the view.
    *******************************************************************************/
   @Test
   void publishesBuiltInViewOnlyWithAssignmentAndFields() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = instance.getTable(TestUtils.TABLE_NAME_ORDER);
      assertNull(frontend(table).getBuiltInViews());
      RecordFeaturesMetaData features = RecordFeaturesMetaData.ofOrWithNew(table).withEnabled(RecordFeature.ASSIGNMENT, true);
      for(RecordOwnership ownership : List.of(new RecordOwnership().withOwnerFieldName("shipToName"),
         new RecordOwnership().withAssigneeFieldName("storeId"),
         new RecordOwnership().withOwnerFieldName("shipToName").withAssigneeFieldName("storeId")))
      {
         features.setOwnership(ownership);
         RuntimeMetaDataOverlay.refresh(instance);
         QFrontendTableMetaData frontend = frontend(table);
         assertEquals("myRecords", frontend.getBuiltInViews().get(0).getName());
         assertEquals("My records", frontend.getBuiltInViews().get(0).getLabel());
         assertEquals(ownership.getOwnerFieldName(), frontend.getRecordFeatures().getOwnership().getOwnerFieldName());
         assertEquals(ownership.getAssigneeFieldName(), frontend.getRecordFeatures().getOwnership().getAssigneeFieldName());
         QQueryFilter filter = frontend.getBuiltInViews().get(0).getFilter();
         assertEquals(QQueryFilter.BooleanOperator.OR, filter.getBooleanOperator());
         assertEquals(ownership.getOwnerFieldName() != null && ownership.getAssigneeFieldName() != null ? 2 : 1, filter.getCriteria().size());
         filter.getCriteria().forEach(criterion -> assertInstanceOf(CurrentUser.class, criterion.getValues().get(0)));
      }
      features.withOwnership(new RecordOwnership());
      RuntimeMetaDataOverlay.refresh(instance);
      assertNull(frontend(table).getBuiltInViews());
      features.withOwnership(new RecordOwnership().withOwnerFieldName("shipToName")).withEnabled(RecordFeature.ASSIGNMENT, false);
      RuntimeMetaDataOverlay.refresh(instance);
      assertNull(frontend(table).getBuiltInViews());
   }



   /*******************************************************************************
    ** Inherited ownership is checked against every table to which it applies.
    *******************************************************************************/
   @Test
   void validatesInheritedOwnershipAndTableOverride() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = instance.getTable(TestUtils.TABLE_NAME_ORDER);
      RecordFeaturesAppMetaData appFeatures = new RecordFeaturesAppMetaData().withEnabled(RecordFeature.ASSIGNMENT, true)
         .withOwnership(new RecordOwnership().withOwnerFieldName("missingOwner"));
      instance.addApp(new QAppMetaData().withName("ownershipApp").withSection(new QAppSection().withName("records").withTables(List.of(table.getName())))
         .withSupplementalMetaData(appFeatures));
      new QInstanceEnricher(instance).enrich();
      QInstanceValidator validator = new QInstanceValidator();
      assertThrows(QException.class, () -> validator.revalidate(instance));
      assertTrue(validator.getErrors().stream().anyMatch(error -> error.contains("ownership field [missingOwner] does not exist")));
      RecordFeaturesMetaData.ofOrWithNew(table).withOwnership(new RecordOwnership().withOwnerFieldName("shipToName").withAssigneeFieldName("storeId"));
      new QInstanceValidator().revalidate(instance);
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(2, frontend(table).getBuiltInViews().get(0).getFilter().getCriteria().size());
   }



   /*******************************************************************************
    ** Existing non-user-id fields are rejected even while assignment is disabled.
    *******************************************************************************/
   @Test
   void rejectsWrongFieldType() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      QTableMetaData table = instance.getTable(TestUtils.TABLE_NAME_ORDER);
      RecordFeaturesMetaData.ofOrWithNew(table).withOwnership(new RecordOwnership().withAssigneeFieldName("createDate"));
      QInstanceValidator validator = new QInstanceValidator();
      assertThrows(QException.class, () -> validator.revalidate(instance));
      assertTrue(validator.getErrors().stream().anyMatch(error -> error.contains("ownership field [createDate] must be STRING or INTEGER")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QFrontendTableMetaData frontend(QTableMetaData table)
   {
      return (new QFrontendTableMetaData(new TableMetaDataInput(), QContext.getQInstance().getBackend(table.getBackendName()), table, true, false));
   }
}
