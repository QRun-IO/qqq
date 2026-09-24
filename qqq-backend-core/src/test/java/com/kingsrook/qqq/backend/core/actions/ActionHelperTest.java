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

package com.kingsrook.qqq.backend.core.actions;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for ActionHelper
 *******************************************************************************/
class ActionHelperTest extends BaseTest
{

   /*******************************************************************************
    ** validateSession should succeed when QContext holds a valid mock session.
    *******************************************************************************/
   @Test
   void testValidateSession_validMockSession_succeeds() throws QException
   {
      ActionHelper.validateSession(null);
   }



   /*******************************************************************************
    ** validateSession should throw QException when QInstance is absent from context.
    *******************************************************************************/
   @Test
   void testValidateSession_noQInstance_throwsQException()
   {
      QContext.clear();

      assertThatThrownBy(() -> ActionHelper.validateSession(null))
         .isInstanceOf(QException.class)
         .hasMessageContaining("QInstance was not set");
   }



   /*******************************************************************************
    ** validateSession should throw QException when QSession is absent from context.
    *******************************************************************************/
   @Test
   void testValidateSession_noQSession_throwsQException()
   {
      QContext.init(TestUtils.defineInstance(), null);

      assertThatThrownBy(() -> ActionHelper.validateSession(null))
         .isInstanceOf(QException.class)
         .hasMessageContaining("QSession was not set");
   }



   /*******************************************************************************
    ** editFirstValue should apply the function to the first list element.
    *******************************************************************************/
   @Test
   void testEditFirstValue_nonEmptyList_transformsFirstElement()
   {
      List<Serializable> values = new ArrayList<>(List.of("hello", "world"));
      ActionHelper.editFirstValue(values, String::toUpperCase);

      assertEquals("HELLO", values.get(0));
      assertEquals("world", values.get(1));
   }



   /*******************************************************************************
    ** editFirstValue on an empty list should not throw.
    *******************************************************************************/
   @Test
   void testEditFirstValue_emptyList_noOp()
   {
      List<Serializable> values = new ArrayList<>();
      ActionHelper.editFirstValue(values, String::toUpperCase);

      assertThat(values).isEmpty();
   }



   /*******************************************************************************
    ** editFirstValue with a null-producing function should store null at index 0.
    *******************************************************************************/
   @Test
   void testEditFirstValue_functionReturnsNull_storesNull()
   {
      List<Serializable> values = new ArrayList<>(List.of("original"));
      ActionHelper.editFirstValue(values, s -> null);

      assertNull(values.get(0));
   }

}
