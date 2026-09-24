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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.util.function.Function;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for JsonExportStreamer 
 *******************************************************************************/
class JsonExportStreamerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      Function<String, String> runOne = label -> new JsonExportStreamer().getLabelForJson(new QFieldMetaData("test", QFieldType.STRING).withLabel(label));
      assertEquals("sku", runOne.apply("SKU"));
      assertEquals("clientName", runOne.apply("Client Name"));
      assertEquals("slaStatus", runOne.apply("SLA Status"));
      assertEquals("lineItem:sku", runOne.apply("Line Item: SKU"));
      assertEquals("parcel:slaStatus", runOne.apply("Parcel: SLA Status"));
      assertEquals("order:client", runOne.apply("Order: Client"));
   }



   /*******************************************************************************
    ** An optional label must not erase the field's JSON key.
    *******************************************************************************/
   @Test
   void testMissingLabelUsesFieldName()
   {
      JsonExportStreamer streamer = new JsonExportStreamer();
      assertEquals("ownedCount", streamer.getLabelForJson(new QFieldMetaData("ownedCount", QFieldType.INTEGER)));
   }



}
