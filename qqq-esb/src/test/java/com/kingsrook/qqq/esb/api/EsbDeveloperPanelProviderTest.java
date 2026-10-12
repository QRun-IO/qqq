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

package com.kingsrook.qqq.esb.api;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.developer.DeveloperPanelsAction;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlock;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlockType;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelsMetaData;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for {@link EsbDeveloperPanelProvider}.
 *******************************************************************************/
class EsbDeveloperPanelProviderTest extends EsbApiTestBase
{

   /*******************************************************************************
    ** The producer registers the provider (the metadata package is scanned by
    ** the test base), and the panel describes the order table's events.
    *******************************************************************************/
   @Test
   void testOrderPanel()
   {
      assertNotNull(DeveloperPanelsMetaData.of(qInstance));
      assertThat(DeveloperPanelsMetaData.of(qInstance).getProviders()).anyMatch(reference -> EsbDeveloperPanelProvider.class.getName().equals(reference.getName()));

      DeveloperPanelContext context = new DeveloperPanelContext()
         .withTable(qInstance.getTable(TABLE_NAME_ORDER))
         .withRecord(new QRecord().withValue("id", 7).withValue("orderNo", "A-7"));

      DeveloperPanel panel = new DeveloperPanelsAction().execute(context).stream()
         .filter(p -> EsbDeveloperPanelProvider.ID.equals(p.getId())).findFirst().orElseThrow();
      assertEquals("ESB Events", panel.getTitle());
      assertEquals(null, panel.getError());

      DeveloperPanelBlock attributes = panel.getBlocks().get(0);
      assertEquals(DeveloperPanelBlockType.KEY_VALUES, attributes.getType());
      assertEquals("qqq.table." + TABLE_NAME_ORDER + ".inserted, qqq.table." + TABLE_NAME_ORDER + ".updated", attributes.getItems().get(0).getValue());
      assertEquals("7", attributes.getItems().get(2).getValue());

      DeveloperPanelBlock publications = panel.getBlocks().get(1);
      assertEquals(2, publications.getRows().size());
      assertEquals(DESTINATION_ORDER_EVENTS, publications.getRows().get(0).get(0));

      DeveloperPanelBlock sample = panel.getBlocks().stream().filter(block -> block.getType() == DeveloperPanelBlockType.CODE).findFirst().orElseThrow();
      JSONObject event = new JSONObject(sample.getCode());
      assertEquals("qqq.table." + TABLE_NAME_ORDER + ".updated", event.getString("type"));
      assertEquals("7", event.getString("subject"));
      assertEquals("A-7", event.getJSONObject("data").getJSONObject("record").getString("orderNo"));

      assertEquals(DeveloperPanelBlockType.LINKS, panel.getBlocks().get(panel.getBlocks().size() - 1).getType());
   }



   /*******************************************************************************
    ** A table without ESB publications gets no panel.
    *******************************************************************************/
   @Test
   void testTableWithoutPublications()
   {
      DeveloperPanelContext context = new DeveloperPanelContext()
         .withTable(qInstance.getTable(TABLE_NAME_CUSTOMER))
         .withRecord(new QRecord().withValue("id", 1));
      assertFalse(new EsbDeveloperPanelProvider().isAvailable(context));
      List<DeveloperPanel> panels = new DeveloperPanelsAction().execute(context);
      assertTrue(panels.stream().noneMatch(panel -> EsbDeveloperPanelProvider.ID.equals(panel.getId())));
   }

}
