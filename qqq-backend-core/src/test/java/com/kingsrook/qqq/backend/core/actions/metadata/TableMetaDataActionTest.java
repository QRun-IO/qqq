/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.metadata;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.ExamplePersonalizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for TableMetaDataAction
 **
 *******************************************************************************/
class TableMetaDataActionTest extends BaseTest
{

   /*******************************************************************************
    ** Test basic success case.
    **
    *******************************************************************************/
   @Test
   public void test() throws QException
   {
      TableMetaDataInput request = new TableMetaDataInput();
      request.setTableName("person");
      TableMetaDataOutput result = new TableMetaDataAction().execute(request);
      assertNotNull(result);
      assertNotNull(result.getTable());
      assertEquals("person", result.getTable().getName());
      assertEquals("Person", result.getTable().getLabel());
   }



   /*******************************************************************************
    ** Test exception is thrown for the "not-found" case.
    **
    *******************************************************************************/
   @Test
   public void test_notFound()
   {
      assertThrows(QUserFacingException.class, () ->
      {
         TableMetaDataInput request = new TableMetaDataInput();
         request.setTableName("willNotBeFound");
         new TableMetaDataAction().execute(request);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersonalization() throws QException
   {
      ////////////////////////////////////////////////
      // establish baseline without personalization //
      ////////////////////////////////////////////////
      TableMetaDataInput request = new TableMetaDataInput();
      request.setTableName(TestUtils.TABLE_NAME_ORDER);
      request.setInputSource(QInputSource.USER);
      TableMetaDataOutput result = new TableMetaDataAction().execute(request);
      assertTrue(result.getTable().getFields().containsKey("orderNo"));
      assertTrue(result.getTable().getFields().containsKey("total"));
      assertEquals("orderLine", result.getTable().getExposedJoins().get(0).getJoinTable().getName());
      assertTrue(result.getTable().getExposedJoins().get(0).getJoinTable().getFields().containsKey("lineNumber"));

      //////////////////////////////////////////////////
      // now make total and lineNumber fields go away //
      //////////////////////////////////////////////////
      QContext.getQSession().getUser().setIdReference("jdoe");
      ExamplePersonalizer.registerInQInstance();
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_ORDER);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_ORDER, "total", "jdoe");
      ExamplePersonalizer.addCustomizableTable(TestUtils.TABLE_NAME_LINE_ITEM);
      ExamplePersonalizer.addFieldToRemoveForUserId(TestUtils.TABLE_NAME_LINE_ITEM, "lineNumber", "jdoe");
      result = new TableMetaDataAction().execute(request);
      assertTrue(result.getTable().getFields().containsKey("orderNo"));
      assertFalse(result.getTable().getFields().containsKey("total"));
      assertEquals("orderLine", result.getTable().getExposedJoins().get(0).getJoinTable().getName());
      assertFalse(result.getTable().getExposedJoins().get(0).getJoinTable().getFields().containsKey("lineNumber"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPostMetaDataAction() throws QException
   {
      //////////////////////////////////////////////////////
      // establish baseline without post meta data action //
      //////////////////////////////////////////////////////
      TableMetaDataInput request = new TableMetaDataInput();
      request.setTableName(TestUtils.TABLE_NAME_ORDER);
      request.setInputSource(QInputSource.USER);
      TableMetaDataOutput result = new TableMetaDataAction().execute(request);
      assertFalse(result.getTable().getFields().containsKey("hello"));
      assertTrue(result.getTable().getFields().containsKey("orderNo"));
      assertTrue(result.getTable().getFields().containsKey("total"));
      assertEquals("orderLine", result.getTable().getExposedJoins().get(0).getJoinTable().getName());
      assertTrue(result.getTable().getExposedJoins().get(0).getJoinTable().getFields().containsKey("lineNumber"));

      //////////////////////////////////////////////////
      // now make total and lineNumber fields go away //
      //////////////////////////////////////////////////
      QContext.getQSession().getUser().setIdReference("jdoe");
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER).withCustomizer(TableCustomizers.POST_META_DATA_ACTION.getRole(), new QCodeReference(TestPostMetaDataCustomizer.class));
      result = new TableMetaDataAction().execute(request);
      assertTrue(result.getTable().getFields().containsKey("hello"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class TestPostMetaDataCustomizer implements TableCustomizerInterface
   {

      /*******************************************************************************
       ** custom actions to run after table meta data was created by the meta data action
       **
       *******************************************************************************/
      @Override
      public void postMetaDataAction(TableMetaDataInput tableMetaDataInput, TableMetaDataOutput tableMetaDataOutput) throws QException
      {
         tableMetaDataOutput.getTable().getFields().put("hello", new QFrontendFieldMetaData(new QFieldMetaData("hello", QFieldType.INTEGER)));
      }
   }

}
