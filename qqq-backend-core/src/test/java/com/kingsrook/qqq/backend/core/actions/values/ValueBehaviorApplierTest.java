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

package com.kingsrook.qqq.backend.core.actions.values;


import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.CaseChangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldDisplayBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.FieldFilterBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for ValueBehaviorApplier - and also providing coverage for
 ** ValueTooLongBehavior (the first implementation, which was previously in the
 ** class under test).
 *******************************************************************************/
class ValueBehaviorApplierTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValueTooLongNormalCases()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("firstName").withMaxLength(10).withBehavior(ValueTooLongBehavior.TRUNCATE);
      table.getField("lastName").withMaxLength(10).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);
      table.getField("email").withMaxLength(20).withBehavior(ValueTooLongBehavior.ERROR);

      List<QRecord> recordList = List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "First name too long").withValue("lastName", "Smith").withValue("email", "john@smith.com"),
         new QRecord().withValue("id", 2).withValue("firstName", "John").withValue("lastName", "Last name too long").withValue("email", "john@smith.com"),
         new QRecord().withValue("id", 3).withValue("firstName", "First name too long").withValue("lastName", "Smith").withValue("email", "john.smith@emaildomainwayytolongtofit.com")
      );
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, recordList, null);

      assertEquals("First name", getRecordById(recordList, 1).getValueString("firstName"));
      assertEquals("Last na...", getRecordById(recordList, 2).getValueString("lastName"));
      assertEquals("john.smith@emaildomainwayytolongtofit.com", getRecordById(recordList, 3).getValueString("email"));
      assertFalse(getRecordById(recordList, 3).getErrors().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOmitBehaviors()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      table.getField("firstName").withMaxLength(10).withBehavior(ValueTooLongBehavior.TRUNCATE);
      table.getField("lastName").withMaxLength(10).withBehavior(ValueTooLongBehavior.TRUNCATE_ELLIPSIS);
      table.getField("email").withMaxLength(20).withBehavior(ValueTooLongBehavior.ERROR);

      List<QRecord> recordList = List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "First name too long").withValue("lastName", "Smith").withValue("email", "john@smith.com"),
         new QRecord().withValue("id", 2).withValue("firstName", "John").withValue("lastName", "Last name too long").withValue("email", "john@smith.com"),
         new QRecord().withValue("id", 3).withValue("firstName", "First name too long").withValue("lastName", "Smith").withValue("email", "john.smith@emaildomainwayytolongtofit.com")
      );

      Set<FieldBehavior<?>> behaviorsToOmit = Set.of(ValueTooLongBehavior.ERROR);
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, recordList, behaviorsToOmit);

      ///////////////////////////////////////////////////////////////////////////////////////////
      // the third error behavior was set to be omitted, so no errors should be on that record //
      ///////////////////////////////////////////////////////////////////////////////////////////
      assertEquals("First name", getRecordById(recordList, 1).getValueString("firstName"));
      assertEquals("Last na...", getRecordById(recordList, 2).getValueString("lastName"));
      assertEquals("john.smith@emaildomainwayytolongtofit.com", getRecordById(recordList, 3).getValueString("email"));
      assertTrue(getRecordById(recordList, 3).getErrors().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testValueTooLongEdgeCases()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // make sure PASS THROUGH actually does nothing, and that a maxLength w/ no behavior specified also does nothing (e.g., does PASS_THROUGH) //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      table.getField("firstName").withMaxLength(10).withBehavior(ValueTooLongBehavior.PASS_THROUGH);
      table.getField("lastName").withMaxLength(10);

      List<QRecord> recordList = List.of(
         ////////////////////////////////////////////////////////////////
         // make sure nulls and empty are okay, and don't get changed. //
         ////////////////////////////////////////////////////////////////
         new QRecord().withValue("id", 1).withValue("firstName", "First name too long").withValue("lastName", null).withValue("email", "john@smith.com"),
         new QRecord().withValue("id", 2).withValue("firstName", "").withValue("lastName", "Last name too long").withValue("email", "john@smith.com")
      );
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, recordList, null);

      assertEquals("First name too long", getRecordById(recordList, 1).getValueString("firstName"));
      assertNull(getRecordById(recordList, 1).getValueString("lastName"));
      assertEquals("Last name too long", getRecordById(recordList, 2).getValueString("lastName"));
      assertEquals("", getRecordById(recordList, 2).getValueString("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testApplyFormattingBehaviors()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      table.getField("firstName").withBehavior(ToUpperCaseBehavior.getInstance());
      table.getField("lastName").withBehavior(ToUpperCaseBehavior.NOOP);
      table.getField("ssn").withBehavior(ValueTooLongBehavior.TRUNCATE).withMaxLength(1);

      QRecord record = new QRecord().withValue("firstName", "Homer").withValue("lastName", "Simpson").withValue("ssn", "0123456789");
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.FORMATTING, qInstance, table, List.of(record), null);

      assertEquals("HOMER", record.getDisplayValue("firstName"));
      assertNull(record.getDisplayValue("lastName")); // noop will literally do nothing, not even pass value through.
      assertEquals("0123456789", record.getValueString("ssn")); // formatting action should not run the too-long truncate behavior

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now put to-upper-case behavior on lastName, but run INSERT actions - and make sure it doesn't get applied. //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      table.getField("lastName").withBehavior(ToUpperCaseBehavior.getInstance());
      ValueBehaviorApplier.applyFieldBehaviors(ValueBehaviorApplier.Action.INSERT, qInstance, table, List.of(record), null);
      assertNull(record.getDisplayValue("lastName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord getRecordById(List<QRecord> recordList, Integer id)
   {
      Optional<QRecord> recordOpt = recordList.stream().filter(r -> r.getValueInteger("id").equals(id)).findFirst();
      if(recordOpt.isEmpty())
      {
         fail("Didn't find record with id=" + id);
      }
      return (recordOpt.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ToUpperCaseBehavior implements FieldDisplayBehavior<ToUpperCaseBehavior>
   {
      private final boolean enabled;

      private static ToUpperCaseBehavior NOOP     = new ToUpperCaseBehavior(false);
      private static ToUpperCaseBehavior instance = new ToUpperCaseBehavior(true);



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      private ToUpperCaseBehavior(boolean enabled)
      {
         this.enabled = enabled;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ToUpperCaseBehavior getDefault()
      {
         return (NOOP);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public static ToUpperCaseBehavior getInstance()
      {
         return (instance);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
      {
         if(!enabled)
         {
            return;
         }

         for(QRecord record : CollectionUtils.nonNullList(recordList))
         {
            String displayValue = record.getValueString(field.getName());
            if(displayValue != null)
            {
               displayValue = displayValue.toUpperCase();
            }

            record.setDisplayValue(field.getName(), displayValue);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFilters()
   {
      QInstance      qInstance = QContext.getQInstance();
      QTableMetaData table     = qInstance.getTable(TestUtils.TABLE_NAME_SHAPE);
      QFieldMetaData field     = table.getField("name");
      field.setBehaviors(Set.of(CaseChangeBehavior.TO_LOWER_CASE));

      assertNull(ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, null, null));

      QQueryFilter emptyFilter = new QQueryFilter();
      assertSame(emptyFilter, ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, emptyFilter, null));

      QQueryFilter hasCriteriaButNotUpdated = new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1));
      assertSame(hasCriteriaButNotUpdated, ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasCriteriaButNotUpdated, null));

      QQueryFilter hasSubFiltersButNotUpdated = new QQueryFilter().withSubFilters(List.of(new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))));
      assertSame(hasSubFiltersButNotUpdated, ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasSubFiltersButNotUpdated, null));

      QQueryFilter hasCriteriaWithoutValues = new QQueryFilter().withSubFilters(List.of(new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS))));
      assertSame(hasCriteriaWithoutValues, ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasCriteriaWithoutValues, null));

      QQueryFilter hasCriteriaAndSubFiltersButNotUpdated = new QQueryFilter()
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))
         .withSubFilters(List.of(new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))));
      assertSame(hasCriteriaAndSubFiltersButNotUpdated, ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasCriteriaAndSubFiltersButNotUpdated, null));

      QQueryFilter hasCriteriaToUpdate = new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle"));
      QQueryFilter hasCriteriaUpdated  = ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasCriteriaToUpdate, null);
      assertNotSame(hasCriteriaToUpdate, hasCriteriaUpdated);
      assertEquals("triangle", hasCriteriaUpdated.getCriteria().get(0).getValues().get(0));
      assertEquals(hasCriteriaToUpdate.getSubFilters(), hasCriteriaUpdated.getSubFilters());

      QQueryFilter hasSubFilterToUpdate = new QQueryFilter().withSubFilter(new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Oval")));
      QQueryFilter hasSubFilterUpdated  = ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasSubFilterToUpdate, null);
      assertNotSame(hasSubFilterToUpdate, hasSubFilterUpdated);
      assertEquals("oval", hasSubFilterUpdated.getSubFilters().get(0).getCriteria().get(0).getValues().get(0));
      assertEquals(hasSubFilterToUpdate.getCriteria(), hasSubFilterUpdated.getCriteria());

      QQueryFilter hasCriteriaAndSubFilterToUpdate = new QQueryFilter()
         .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Square"))
         .withSubFilter(new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Circle")));
      QQueryFilter hasCriteriaAndSubFilterUpdated = ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasCriteriaAndSubFilterToUpdate, null);
      assertNotSame(hasCriteriaAndSubFilterToUpdate, hasCriteriaAndSubFilterUpdated);
      assertEquals("square", hasCriteriaAndSubFilterUpdated.getCriteria().get(0).getValues().get(0));
      assertEquals("circle", hasCriteriaAndSubFilterUpdated.getSubFilters().get(0).getCriteria().get(0).getValues().get(0));

      QQueryFilter hasMultiValueCriteriaToUpdate = new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.IN, "Triangle", "Square"));
      QQueryFilter hasMultiValueCriteriaUpdated  = ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasMultiValueCriteriaToUpdate, null);
      assertNotSame(hasMultiValueCriteriaToUpdate, hasMultiValueCriteriaUpdated);
      assertEquals(List.of("triangle", "square"), hasMultiValueCriteriaUpdated.getCriteria().get(0).getValues());
      assertEquals(hasMultiValueCriteriaToUpdate.getSubFilters(), hasMultiValueCriteriaUpdated.getSubFilters());

      QQueryFilter hasMultipleCriteriaOnlyToUpdate = new QQueryFilter()
         .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Square"))
         .withCriteria(new QFilterCriteria("id", QCriteriaOperator.IS_NOT_BLANK));

      QQueryFilter hasMultipleCriteriaOnlyOneUpdated = ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, hasMultipleCriteriaOnlyToUpdate, null);
      assertNotSame(hasMultipleCriteriaOnlyToUpdate, hasMultipleCriteriaOnlyOneUpdated);
      assertEquals(2, hasMultipleCriteriaOnlyOneUpdated.getCriteria().size());
      assertEquals(List.of("square"), hasMultipleCriteriaOnlyOneUpdated.getCriteria().get(0).getValues());
      assertEquals(hasMultipleCriteriaOnlyToUpdate.getSubFilters(), hasMultipleCriteriaOnlyOneUpdated.getSubFilters());

      //////////////////////////////////////////////////////////
      // set 2 behaviors on the field - make sure both happen //
      //////////////////////////////////////////////////////////
      field.setBehaviors(Set.of(CaseChangeBehavior.TO_LOWER_CASE, new AppendSomethingBehavior("-x")));
      QQueryFilter criteriaValueToUpdateTwice = new QQueryFilter().withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Triangle"));
      QQueryFilter criteriaValueUpdatedTwice  = ValueBehaviorApplier.applyFieldBehaviorsToFilter(qInstance, table, criteriaValueToUpdateTwice, null);
      assertNotSame(criteriaValueToUpdateTwice, criteriaValueUpdatedTwice);
      assertEquals("triangle-x", criteriaValueUpdatedTwice.getCriteria().get(0).getValues().get(0));
      assertEquals(criteriaValueToUpdateTwice.getSubFilters(), criteriaValueUpdatedTwice.getSubFilters());
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static class AppendSomethingBehavior implements FieldBehavior<AppendSomethingBehavior>, FieldFilterBehavior<AppendSomethingBehavior>
   {
      private String something;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public AppendSomethingBehavior(String something)
      {
         this.something = something;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public Serializable applyToFilterCriteriaValue(Serializable value, QInstance instance, QTableMetaData table, QFieldMetaData field)
      {
         return value + something;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public AppendSomethingBehavior getDefault()
      {
         return null;
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
      {
         //////////
         // noop //
         //////////
      }
   }

}
