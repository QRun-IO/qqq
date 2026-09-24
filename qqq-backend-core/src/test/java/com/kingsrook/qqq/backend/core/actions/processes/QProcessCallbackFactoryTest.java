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
import java.util.Collections;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for QProcessCallbackFactory 
 *******************************************************************************/
class QProcessCallbackFactoryTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      QProcessCallback qProcessCallback = QProcessCallbackFactory.forFilter(new QQueryFilter(new QFilterCriteria("foo", QCriteriaOperator.EQUALS, "bar")));

      QQueryFilter queryFilter = qProcessCallback.getQueryFilter();
      assertEquals(1, queryFilter.getCriteria().size());
      assertEquals("foo", queryFilter.getCriteria().get(0).getFieldName());
      assertEquals(QCriteriaOperator.EQUALS, queryFilter.getCriteria().get(0).getOperator());
      assertEquals("bar", queryFilter.getCriteria().get(0).getValues().get(0));

      assertEquals(Collections.emptyMap(), qProcessCallback.getFieldValues(new ArrayList<>()));
   }



   /*******************************************************************************
    ** forPrimaryKey must produce an EQUALS filter on the given field and value.
    *******************************************************************************/
   @Test
   void testForPrimaryKey_createsEqualsFilter()
   {
      QProcessCallback callback = QProcessCallbackFactory.forPrimaryKey("id", 99);

      QQueryFilter filter = callback.getQueryFilter();
      assertThat(filter.getCriteria()).hasSize(1);
      assertThat(filter.getCriteria().get(0).getFieldName()).isEqualTo("id");
      assertThat(filter.getCriteria().get(0).getOperator()).isEqualTo(QCriteriaOperator.EQUALS);
      assertThat(filter.getCriteria().get(0).getValues()).containsExactly(99);
   }



   /*******************************************************************************
    ** forPrimaryKeys must produce an IN filter on the given collection.
    *******************************************************************************/
   @Test
   void testForPrimaryKeys_createsInFilter()
   {
      List<Integer> ids = List.of(1, 2, 3);
      QProcessCallback callback = QProcessCallbackFactory.forPrimaryKeys("id", ids);

      QQueryFilter filter = callback.getQueryFilter();
      assertThat(filter.getCriteria()).hasSize(1);
      assertThat(filter.getCriteria().get(0).getOperator()).isEqualTo(QCriteriaOperator.IN);
      assertThat(filter.getCriteria().get(0).getValues()).containsExactlyInAnyOrderElementsOf(ids);
   }



   /*******************************************************************************
    ** forRecord must produce an EQUALS filter on the table's primary key field
    ** when the record carries a tableName (resolves field from QInstance).
    *******************************************************************************/
   @Test
   void testForRecord_withTableName_usesPrimaryKeyFromInstance()
   {
      QRecord record = new QRecord()
         .withTableName(TestUtils.TABLE_NAME_PERSON)
         .withValue("id", 42);

      QProcessCallback callback = QProcessCallbackFactory.forRecord(record);

      QQueryFilter filter = callback.getQueryFilter();
      assertThat(filter.getCriteria()).hasSize(1);
      assertThat(filter.getCriteria().get(0).getFieldName()).isEqualTo("id");
      assertThat(filter.getCriteria().get(0).getOperator()).isEqualTo(QCriteriaOperator.EQUALS);
      assertThat(filter.getCriteria().get(0).getValues().get(0)).isEqualTo(42);
   }



   /*******************************************************************************
    ** forRecord must throw QRuntimeException when the primary-key value is null.
    *******************************************************************************/
   @Test
   void testForRecord_nullPrimaryKeyValue_throwsRuntimeException()
   {
      QRecord record = new QRecord()
         .withTableName(TestUtils.TABLE_NAME_PERSON);
      // "id" is intentionally absent — value is null

      assertThatThrownBy(() -> QProcessCallbackFactory.forRecord(record))
         .isInstanceOf(QRuntimeException.class)
         .hasMessageContaining("primary key field");
   }

}