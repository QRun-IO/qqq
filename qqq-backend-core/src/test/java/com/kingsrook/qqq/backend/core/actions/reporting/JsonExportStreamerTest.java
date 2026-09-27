/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
