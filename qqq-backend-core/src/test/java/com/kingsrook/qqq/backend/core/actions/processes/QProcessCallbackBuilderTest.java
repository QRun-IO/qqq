/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.actions.processes;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for QProcessCallbackBuilder
 *******************************************************************************/
@TestMethodOrder(MethodOrderer.MethodName.class)
class QProcessCallbackBuilderTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(QProcessCallbackBuilderTest.class);



   /*******************************************************************************
    ** withFilter followed by build should expose that exact filter via the callback.
    *******************************************************************************/
   @Test
   void testBuild_withFilter_exposesFilter()
   {
      QQueryFilter filter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("status", QCriteriaOperator.EQUALS, "active"));

      QProcessCallback callback = new QProcessCallbackBuilder()
         .withFilter(filter)
         .build();

      assertThat(callback.getQueryFilter()).isEqualTo(filter);
   }



   /*******************************************************************************
    ** withPrimaryKey generates a single EQUALS criteria.
    *******************************************************************************/
   @Test
   void testBuild_withPrimaryKey_generatesSingleEqualsCriteria()
   {
      QProcessCallback callback = new QProcessCallbackBuilder()
         .withPrimaryKey("id", 42)
         .build();

      QQueryFilter filter = callback.getQueryFilter();
      assertThat(filter).isNotNull();
      assertThat(filter.getCriteria()).hasSize(1);
      assertThat(filter.getCriteria().get(0).getFieldName()).isEqualTo("id");
      assertThat(filter.getCriteria().get(0).getOperator()).isEqualTo(QCriteriaOperator.EQUALS);
      assertThat(filter.getCriteria().get(0).getValues().get(0)).isEqualTo(42);
   }



   /*******************************************************************************
    ** withPrimaryKeys generates an IN criteria from a collection.
    *******************************************************************************/
   @Test
   void testBuild_withPrimaryKeys_generatesInCriteria()
   {
      List<Integer> ids = List.of(1, 2, 3);

      QProcessCallback callback = new QProcessCallbackBuilder()
         .withPrimaryKeys("id", ids)
         .build();

      QQueryFilter filter = callback.getQueryFilter();
      assertThat(filter).isNotNull();
      assertThat(filter.getCriteria()).hasSize(1);
      assertThat(filter.getCriteria().get(0).getOperator()).isEqualTo(QCriteriaOperator.IN);
      assertThat(filter.getCriteria().get(0).getValues()).containsExactlyInAnyOrderElementsOf(ids);
   }



   /*******************************************************************************
    ** withFieldValues exposes field values map from the callback.
    *******************************************************************************/
   @Test
   void testBuild_withFieldValues_exposesValues()
   {
      Map<String, java.io.Serializable> values = Map.of("color", "blue", "size", 10);

      QProcessCallback callback = new QProcessCallbackBuilder()
         .withPrimaryKey("id", 1)
         .withFieldValues(values)
         .build();

      assertThat(callback.getFieldValues(new ArrayList<>())).containsAllEntriesOf(values);
   }



   /*******************************************************************************
    ** withQueryInputCustomizer is stored and invoked by the callback.
    *******************************************************************************/
   @Test
   void testBuild_withQueryInputCustomizer_customizerIsInvoked()
   {
      boolean[] invoked = { false };

      QProcessCallback callback = new QProcessCallbackBuilder()
         .withPrimaryKey("id", 1)
         .withQueryInputCustomizer((runBackendStepInput, queryInput) -> invoked[0] = true)
         .build();

      callback.customizeInputPreQuery(null, null);
      assertThat(invoked[0]).isTrue();
   }



   /*******************************************************************************
    ** Without a queryInputCustomizer, customizeInputPreQuery must not throw.
    *******************************************************************************/
   @Test
   void testBuild_noQueryInputCustomizer_customizeInputPreQueryIsNoop()
   {
      QProcessCallback callback = new QProcessCallbackBuilder()
         .withPrimaryKey("id", 1)
         .build();

      /////////////////////////////
      // must not throw          //
      /////////////////////////////
      callback.customizeInputPreQuery(null, null);
   }



   /*******************************************************************************
    ** Setter-based (non-fluent) accessors on the builder are consistent with
    ** the fluent ones.
    *******************************************************************************/
   @Test
   void testSetterGetterConsistency()
   {
      QQueryFilter filter = new QQueryFilter();
      Map<String, java.io.Serializable> fieldValues = Map.of("x", "y");

      QProcessCallbackBuilder builder = new QProcessCallbackBuilder();
      builder.setFilter(filter);
      builder.setFieldValues(fieldValues);
      builder.setQueryInputCustomizer((a, b) -> {});

      assertThat(builder.getFilter()).isEqualTo(filter);
      assertThat(builder.getQueryInputCustomizer()).isNotNull();
   }

}
