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

package com.kingsrook.qqq.backend.core.model.actions.tables.query;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS;
import static com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.IS_BLANK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;


/*******************************************************************************
 ** Unit test for QFilterCriteria
 *******************************************************************************/
class QFilterCriteriaTest extends BaseTest
{

   /*******************************************************************************
    ** Make sure that the constructors that takes a List or Serializable... and does
    ** the right thing - e.g., never making a List-of-List, or List of array, and
    ** that we never have null values - always an empty list as the degenerate case.
    *******************************************************************************/
   @Test
   void test()
   {
      assertEquals(1, new QFilterCriteria("foo", EQUALS, "A").getValues().size());
      assertEquals(1, new QFilterCriteria("foo", EQUALS, List.of("A")).getValues().size());
      assertEquals(2, new QFilterCriteria("foo", EQUALS, List.of("A", "B")).getValues().size());
      assertEquals(2, new QFilterCriteria("foo", EQUALS, "A", "B").getValues().size());

      List<Serializable> list = List.of("A", "B", "C");
      assertEquals(3, new QFilterCriteria("foo", EQUALS, list).getValues().size());
      assertEquals(List.of("A", "B", "C"), new QFilterCriteria("foo", EQUALS, list).getValues());

      Serializable[] array = new Serializable[] { "A", "B", "C", "D" };
      assertEquals(4, new QFilterCriteria("foo", EQUALS, array).getValues().size());
      assertEquals(List.of("A", "B", "C", "D"), new QFilterCriteria("foo", EQUALS, array).getValues());

      assertEquals(3, new QFilterCriteria("foo", EQUALS, new ArrayList<>(list)).getValues().size());
      assertEquals(List.of("A", "B", "C"), new QFilterCriteria("foo", EQUALS, new ArrayList<>(list)).getValues());

      assertEquals(0, new QFilterCriteria("foo", IS_BLANK).getValues().size());

      Serializable maybeNull = null;
      assertEquals(0, new QFilterCriteria("foo", EQUALS, maybeNull).getValues().size());

      List<Serializable> nullList = null;
      assertEquals(0, new QFilterCriteria("foo", EQUALS, nullList).getValues().size());

      assertEquals(0, new QFilterCriteria("foo", EQUALS, (List<Serializable>) null).getValues().size());
   }



   /*******************************************************************************
    ** Verify that clone() deep-copies the fieldFunction, so mutating the clone's
    ** arguments does not affect the original.
    *******************************************************************************/
   @Test
   void testCloneWithFieldFunction()
   {
      Map<String, Serializable> args = new HashMap<>(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2));

      QFilterCriteria original = new QFilterCriteria("name", EQUALS, "x")
         .withFieldFunction(new FieldFunction()
            .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
            .withFieldName("name")
            .withArguments(args));

      QFilterCriteria clone = original.clone();

      /////////////////////////////////////////////
      // clone should have its own fieldFunction //
      /////////////////////////////////////////////
      assertNotNull(clone.getFieldFunction());
      assertNotSame(original.getFieldFunction(), clone.getFieldFunction());
      assertEquals("name", clone.getFieldFunction().getFieldName());

      ////////////////////////////////////////////////////////////////////
      // mutating the clone's arguments should not affect the original  //
      ////////////////////////////////////////////////////////////////////
      clone.getFieldFunction().getArguments().put(SubStringFunction.FROM_INDEX_PARAM, 99);
      assertEquals(2, original.getFieldFunction().getArguments().get(SubStringFunction.FROM_INDEX_PARAM));
   }

}