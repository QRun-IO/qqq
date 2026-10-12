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

package com.kingsrook.qqq.backend.core.actions.developer;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlock;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlockType;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelProviderInterface;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for {@link DeveloperPanelsAction} and the built-in record security
 ** provider.
 *******************************************************************************/
class DeveloperPanelsActionTest extends BaseTest
{

   /*******************************************************************************
    ** Without registered providers, only the built-in record security panel.
    *******************************************************************************/
   @Test
   void testBuiltInOnly()
   {
      List<DeveloperPanel> panels = new DeveloperPanelsAction().execute(personContext());
      assertEquals(List.of(RecordSecurityDeveloperPanelProvider.ID), panels.stream().map(DeveloperPanel::getId).toList());
      assertEquals("No locks", panels.get(0).getStatusLabel());
   }



   /*******************************************************************************
    ** Registered providers: available ones are built (with provider defaults
    ** filled in) and sorted by order; unavailable ones are omitted; a failing
    ** one yields an error panel; a duplicate registration is ignored.
    *******************************************************************************/
   @Test
   void testRegisteredProviders()
   {
      DeveloperPanelsMetaData.ofOrWithNew(QContext.getQInstance())
         .withProvider(new QCodeReference(AvailableProvider.class))
         .withProvider(new QCodeReference(AvailableProvider.class))
         .withProvider(new QCodeReference(UnavailableProvider.class))
         .withProvider(new QCodeReference(FailingProvider.class));

      List<DeveloperPanel> panels = new DeveloperPanelsAction().execute(personContext());
      assertEquals(List.of("failing", RecordSecurityDeveloperPanelProvider.ID, "available"), panels.stream().map(DeveloperPanel::getId).toList());

      DeveloperPanel failing = panels.get(0);
      assertThat(failing.getError()).contains("could not be built");
      assertEquals("Failing", failing.getTitle());
      assertNull(failing.getBlocks());

      DeveloperPanel available = panels.get(2);
      assertEquals("Available", available.getTitle());
      assertEquals(700, available.getOrder());
      assertEquals(DeveloperPanelBlockType.KEY_VALUES, available.getBlocks().get(0).getType());
      assertEquals("1", available.getBlocks().get(0).getItems().get(0).getValue());
   }



   /*******************************************************************************
    ** The record security panel explains an all-access key and a matching key.
    *******************************************************************************/
   @Test
   void testRecordSecurityLocks()
   {
      DeveloperPanelContext context = new DeveloperPanelContext()
         .withTable(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER))
         .withRecord(new QRecord().withValue("id", 1).withValue("storeId", 42));

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE, 42);
      DeveloperPanel panel = new DeveloperPanelsAction().execute(context).get(0);
      assertEquals("1 lock", panel.getStatusLabel());
      DeveloperPanelBlock locks = panel.getBlocks().get(1);
      assertEquals(DeveloperPanelBlockType.TABLE, locks.getType());
      List<String> row = locks.getRows().get(0);
      assertEquals(List.of("store", "storeId", "READ_AND_WRITE", "DENY", "42", "Your session has key value 42"), row);

      QContext.getQSession().withSecurityKeyValue(TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS, true);
      row = new DeveloperPanelsAction().execute(context).get(0).getBlocks().get(1).getRows().get(0);
      assertEquals("All access (" + TestUtils.SECURITY_KEY_TYPE_STORE_ALL_ACCESS + ")", row.get(5));

      DeveloperPanelContext nullStore = new DeveloperPanelContext().withTable(context.getTable()).withRecord(new QRecord().withValue("id", 2));
      QContext.getQSession().clearSecurityKeyValues();
      row = new DeveloperPanelsAction().execute(nullStore).get(0).getBlocks().get(1).getRows().get(0);
      assertEquals("null", row.get(4));
      assertThat(row.get(5)).startsWith("Null value (denied");
      assertNotNull(nullStore.getPrimaryKey());
   }



   /*******************************************************************************
    ** context for person 1.
    *******************************************************************************/
   private DeveloperPanelContext personContext()
   {
      return (new DeveloperPanelContext()
         .withTable(QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY))
         .withRecord(new QRecord().withValue("id", 1).withValue("firstName", "Darin")));
   }



   /*******************************************************************************
    ** A provider that shows the record's id.
    *******************************************************************************/
   public static class AvailableProvider implements DeveloperPanelProviderInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getId()
      {
         return ("available");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getTitle()
      {
         return ("Available");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Integer getOrder()
      {
         return (700);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public boolean isAvailable(DeveloperPanelContext context)
      {
         return (true);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public DeveloperPanel build(DeveloperPanelContext context) throws QException
      {
         return (new DeveloperPanel().withBlock(DeveloperPanelBlock.keyValues("Record").withItem("id", context.getPrimaryKey())));
      }
   }



   /*******************************************************************************
    ** A provider that is never available.
    *******************************************************************************/
   public static class UnavailableProvider extends AvailableProvider
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getId()
      {
         return ("unavailable");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public boolean isAvailable(DeveloperPanelContext context)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** A provider whose build fails.
    *******************************************************************************/
   public static class FailingProvider extends AvailableProvider
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getId()
      {
         return ("failing");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getTitle()
      {
         return ("Failing");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Integer getOrder()
      {
         return (10);
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public DeveloperPanel build(DeveloperPanelContext context) throws QException
      {
         throw (new QException("boom"));
      }
   }

}
