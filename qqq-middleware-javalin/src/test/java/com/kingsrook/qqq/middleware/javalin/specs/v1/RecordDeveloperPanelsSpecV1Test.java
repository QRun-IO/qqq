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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelAction;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlock;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelProviderInterface;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelsMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for {@link RecordDeveloperPanelsSpecV1}.
 *******************************************************************************/
class RecordDeveloperPanelsSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new RecordDeveloperPanelsSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    ** register a test provider in the server's instance.
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = super.defineQInstance();
      DeveloperPanelsMetaData.ofOrWithNew(qInstance).withProvider(new QCodeReference(TestProvider.class));
      return (qInstance);
   }



   /*******************************************************************************
    ** The built-in record security panel and the registered provider's panel,
    ** in order, with their blocks.
    *******************************************************************************/
   @Test
   void test()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/1/developer/panels").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONArray panels = JsonUtils.toJSONObject(response.getBody()).getJSONArray("panels");
      assertEquals(2, panels.length());

      JSONObject security = panels.getJSONObject(0);
      assertEquals("recordSecurity", security.getString("id"));
      assertEquals("Record Security", security.getString("title"));
      assertEquals("TEXT", security.getJSONArray("blocks").getJSONObject(0).getString("type"));

      JSONObject test = panels.getJSONObject(1);
      assertEquals("test", test.getString("id"));
      assertEquals("Test Panel", test.getString("title"));
      assertEquals(500, test.getInt("order"));
      JSONObject keyValues = test.getJSONArray("blocks").getJSONObject(0);
      assertEquals("KEY_VALUES", keyValues.getString("type"));
      assertEquals("1", keyValues.getJSONArray("items").getJSONObject(0).getString("value"));
      JSONObject actions = test.getJSONArray("blocks").getJSONObject(1);
      assertEquals("ACTIONS", actions.getString("type"));
      assertEquals(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE, actions.getJSONArray("actions").getJSONObject(0).getString("processName"));
   }



   /*******************************************************************************
    ** A missing record is a 404.
    *******************************************************************************/
   @Test
   void testNotFound()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/1701/developer/panels").asString();
      assertEquals(404, response.getStatus(), response.getBody());
   }



   /*******************************************************************************
    ** A provider showing the record's primary key and a process action.
    *******************************************************************************/
   public static class TestProvider implements DeveloperPanelProviderInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getId()
      {
         return ("test");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public String getTitle()
      {
         return ("Test Panel");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public boolean isAvailable(DeveloperPanelContext context)
      {
         return ("person".equals(context.getTableName()));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public DeveloperPanel build(DeveloperPanelContext context)
      {
         return (new DeveloperPanel()
            .withBlocks(List.of(
               DeveloperPanelBlock.keyValues("Record").withItem("id", context.getPrimaryKey()),
               DeveloperPanelBlock.actions("Actions").withAction(new DeveloperPanelAction().withLabel("Greet").withProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE)))));
      }
   }

}
