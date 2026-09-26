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

package com.kingsrook.qqq.backend.core.actions.tables;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for CountAction
 **
 *******************************************************************************/
class CountActionTest extends BaseTest
{

   /*******************************************************************************
    ** At the core level, there isn't much that can be asserted, as it uses the
    ** mock implementation - just confirming that all of the "wiring" works.
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      CountInput request = new CountInput();
      request.setTableName("person");
      CountOutput result = new CountAction().execute(request);
      assertNotNull(result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStaticWrapper() throws QException
   {
      TestUtils.insertDefaultShapes(QContext.getQInstance());
      assertEquals(3, CountAction.execute(TestUtils.TABLE_NAME_SHAPE, null));
      assertEquals(3, CountAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidFieldNamesInFilter() throws QException
   {
      assertThatThrownBy(() -> CountAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter(new QFilterCriteria("notAField", QCriteriaOperator.IS_NOT_BLANK))))
         .hasMessageContaining("1 unrecognized field name: notAField");

      assertThatThrownBy(() -> CountAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter()
         .withSubFilter(new QQueryFilter(new QFilterCriteria("notAField", QCriteriaOperator.IS_NOT_BLANK)))))
         .hasMessageContaining("1 unrecognized field name: notAField");

      assertThatThrownBy(() -> CountAction.execute(TestUtils.TABLE_NAME_SHAPE, new QQueryFilter()
         .withOrderBy(new QFilterOrderBy("stillNotAField"))))
         .hasMessageContaining("1 unrecognized field name: stillNotAField");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTablePersonalization() throws QException
   {
      QContext.getQSession().getUser().setIdReference("jdoe");
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_PERSON_MEMORY, "noOfShoes", QContext.getQSession().getUser().getIdReference());

      ///////////////////////////////////////////////////////////////////////////
      // make sure not allowed to filter by a field we don't have in the table //
      ///////////////////////////////////////////////////////////////////////////
      CountInput countInput = new CountInput(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withFilter(new QQueryFilter(new QFilterCriteria("noOfShoes", QCriteriaOperator.EQUALS, 2)))
         .withInputSource(QInputSource.USER);
      assertThatThrownBy(() -> new CountAction().execute(countInput))
         .hasMessageContaining("Query Filter contained 1 unrecognized field name: noOfShoes");
   }

}
