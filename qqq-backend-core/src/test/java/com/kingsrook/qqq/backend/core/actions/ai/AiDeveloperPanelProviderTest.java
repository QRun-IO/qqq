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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.developer.DeveloperPanelsAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlock;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlockType;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.TABLE_ASSIST;
import static com.kingsrook.qqq.backend.core.actions.ai.AiKernelTestSupport.TABLE_CUSTOMER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for {@link AiDeveloperPanelProvider}.
 *******************************************************************************/
class AiDeveloperPanelProviderTest extends BaseTest
{
   private QInstance qInstance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      qInstance = AiKernelTestSupport.defineInstance();
      QContext.init(qInstance, new QSession());
      AiKernelTestSupport.insertData();
      QContext.clear();
   }



   /*******************************************************************************
    ** A registered provider describes the user's assistant access to a record,
    ** with tools, fields and the ask-the-assistant deep link.
    *******************************************************************************/
   @Test
   void testExposedRecord() throws QException
   {
      DeveloperPanelsMetaData.ofOrWithNew(qInstance).withProvider(new QCodeReference(AiDeveloperPanelProvider.class));
      QContext.init(qInstance, AiKernelTestSupport.store1User());

      DeveloperPanel panel = assistantPanel(TABLE_CUSTOMER, 1);
      assertEquals("Assistant", panel.getTitle());
      assertEquals("VALUES", panel.getStatusLabel());
      assertEquals("VALUES", panel.getBlocks().get(0).getItems().get(0).getValue());
      assertEquals("yes", panel.getBlocks().get(0).getItems().get(1).getValue());

      DeveloperPanelBlock tools = panel.getBlocks().get(1);
      assertEquals(DeveloperPanelBlockType.TABLE, tools.getType());
      assertThat(tools.getRows()).anyMatch(row -> row.get(0).equals("query_records") && row.get(2).equals("yes"));

      DeveloperPanelBlock links = panel.getBlocks().get(panel.getBlocks().size() - 1);
      assertEquals(DeveloperPanelBlockType.LINKS, links.getType());
      assertThat(links.getLinks().get(0).getHref()).startsWith("/app/" + TABLE_CUSTOMER + "/1?assistant=open&assistantPrompt=Tell%20me%20about%20");
   }



   /*******************************************************************************
    ** A table the assistant may not use is reported, without the deep link.
    *******************************************************************************/
   @Test
   void testNotExposedTable() throws QException
   {
      DeveloperPanelsMetaData.ofOrWithNew(qInstance).withProvider(new QCodeReference(AiDeveloperPanelProvider.class));
      QContext.init(qInstance, AiKernelTestSupport.store1User());

      DeveloperPanel panel = assistantPanel(TABLE_ASSIST, 1);
      assertEquals("NONE", panel.getStatusLabel());
      assertTrue(panel.getBlocks().stream().noneMatch(block -> block.getType() == DeveloperPanelBlockType.LINKS));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private DeveloperPanel assistantPanel(String tableName, Integer id)
   {
      DeveloperPanelContext context = new DeveloperPanelContext()
         .withTable(qInstance.getTable(tableName))
         .withRecord(new QRecord().withValue("id", id));
      List<DeveloperPanel> panels = new DeveloperPanelsAction().execute(context);
      return (panels.stream().filter(p -> AiDeveloperPanelProvider.ID.equals(p.getId())).findFirst().orElseThrow());
   }

}
