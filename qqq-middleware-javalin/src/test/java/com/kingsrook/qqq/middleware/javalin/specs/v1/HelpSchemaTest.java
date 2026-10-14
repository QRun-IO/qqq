/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.MetaDataResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.WidgetMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.AppMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FieldMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.FrontendStep;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.HelpContentV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.HelpCoverageEntryV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.HelpSearchResultV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.ProcessMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.ProcessMetaDataLight;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.ReportMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableMetaDataLight;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableSection;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.VirtualFieldMetaData;
import com.kingsrook.qqq.openapi.model.Schema;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Assert help schemas describe actual payloads, including inherited properties.
 *******************************************************************************/
class HelpSchemaTest
{
   /*******************************************************************************
    ** Lists and slot maps always reference the public help component.
    *******************************************************************************/
   @Test
   void typedListsAndMaps()
   {
      String ref = "#/components/schemas/HelpContentV1";
      for(ToSchema wrapper : List.of(new FieldMetaData(), new FrontendStep(), new TableSection(), new VirtualFieldMetaData()))
      {
         assertEquals(ref, wrapper.toSchema().getProperties().get("helpContents").getItems().getRef(), wrapper.getClass().getSimpleName());
      }
      for(ToSchema wrapper : List.of(new AppMetaData(), new TableMetaData(), new TableMetaDataLight(), new ProcessMetaData(), new ProcessMetaDataLight(), new ReportMetaData(), new MetaDataResponseV1(), new WidgetMetaData()))
      {
         String property = wrapper instanceof WidgetMetaData ? "helpContent" : "helpContents";
         Schema values = (Schema) wrapper.toSchema().getProperties().get(property).getAdditionalProperties();
         assertEquals(ref, values.getItems().getRef(), wrapper.getClass().getSimpleName());
      }
      assertThat(new HelpContentV1().toSchema().getProperties()).containsOnlyKeys("content", "contentAsHtml", "summary", "format", "roles");
      assertThat(new HelpContentV1().toSchema().getProperties().get("format").getEnumValues()).containsExactly("TEXT", "HTML", "MARKDOWN");
      assertThat(new HelpSearchResultV1().toSchema().getProperties()).containsKeys("kind", "key", "summary", "text", "contentAsHtml", "href");
      assertThat(new HelpCoverageEntryV1().toSchema().getProperties()).containsKeys("kind", "key", "hidden", "hasSummary", "hasContent", "roles");
   }
}
