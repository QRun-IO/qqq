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

package com.kingsrook.qqq.middleware.javalin;


import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/*******************************************************************************
 ** Unit test for QJavalinUtils 
 *******************************************************************************/
class QJavalinUtilsTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      ///////////////////////////////////////////////////////////////////////
      // demonstrate that calling formParam or queryParam can throw        //
      ///////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> mockContext(false, false).queryParam("foo"));
      assertThatThrownBy(() -> mockContext(false, false).formParam("foo"));
      assertEquals("query:foo", mockContext(true, false).queryParam("foo"));
      assertEquals("form:foo", mockContext(false, true).formParam("foo"));

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // now demonstrate that calling these wrapping util methods avoid such exceptions (which was their intent.) //
      // and, that when the context can return values, that the right ones are used                               //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertNull(QJavalinUtils.getQueryParamOrFormParam(mockContext(false, false), "foo"));
      assertEquals("query:foo", QJavalinUtils.getQueryParamOrFormParam(mockContext(true, false), "foo"));
      assertEquals("form:foo", QJavalinUtils.getQueryParamOrFormParam(mockContext(false, true), "foo"));
      assertEquals("query:foo", QJavalinUtils.getQueryParamOrFormParam(mockContext(true, true), "foo"));

      assertNull(QJavalinUtils.getFormParamOrQueryParam(mockContext(false, false), "foo"));
      assertEquals("form:foo", QJavalinUtils.getFormParamOrQueryParam(mockContext(false, true), "foo"));
      assertEquals("query:foo", QJavalinUtils.getFormParamOrQueryParam(mockContext(true, false), "foo"));
      assertEquals("form:foo", QJavalinUtils.getFormParamOrQueryParam(mockContext(true, true), "foo"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static Context mockContext(Boolean returnsQueryParams, Boolean returnsFormParams)
   {
      Context context = mock(Context.class);
      if(returnsQueryParams)
      {
         when(context.queryParam(anyString())).thenAnswer(invocation -> "query:" + invocation.getArgument(0));
      }
      else
      {
         when(context.queryParam(anyString())).thenThrow(new IllegalStateException("Query parameters are unavailable"));
      }

      if(returnsFormParams)
      {
         when(context.formParam(anyString())).thenAnswer(invocation -> "form:" + invocation.getArgument(0));
      }
      else
      {
         when(context.formParam(anyString())).thenThrow(new IllegalStateException("Form parameters are unavailable"));
      }
      return context;
   }
}