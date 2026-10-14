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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.helpcontent.HelpContentMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
    ** HTTP tests for application help coverage.
    *******************************************************************************/
class HelpCoverageSpecV1Test extends SpecTestBase
{
   /*******************************************************************************
    ** Endpoint under test.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new HelpCoverageSpecV1());
   }



   /*******************************************************************************
    ** API version.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** Help fixtures use the same metadata and memory backend as real requests.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = TestUtils.defineInstance();
      new HelpContentMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      instance.getTable("person").withHelpContent("help", new QHelpContent().withContentAsMarkdown("**Email** documentation").withSummary("Contact guide"));
      instance.getTable("person").getField("email").withHelpContent(new QHelpContent("Private documentation").withPermissionName("private.help"));
      return (instance);
   }



   /*******************************************************************************
    ** Coverage filters, totals and author permission failures over HTTP.
    *******************************************************************************/
   @Test
   void coverage()
   {
      var response = Unirest.get(getBaseUrlAndPath() + "/help/coverage?kind=table").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      var body = JsonUtils.toJSONObject(response.getBody());
      assertThat(body.getJSONArray("entries").toString()).contains("table:person;slot:help");
      assertEquals(1, body.getJSONArray("totals").getJSONObject(0).getInt("withHelp"));
      response = Unirest.get(getBaseUrlAndPath() + "/help/coverage?kind=table&missingOnly=true").asString();
      assertThat(JsonUtils.toJSONObject(response.getBody()).getJSONArray("entries").toString()).doesNotContain("table:person;slot:help");
      assertEquals(400, Unirest.get(getBaseUrlAndPath() + "/help/coverage?missingOnly=bad").asString().getStatus());
      response = Unirest.get(getBaseUrlAndPath() + "/help/coverage?kind=unknown").asString();
      body = JsonUtils.toJSONObject(response.getBody());
      assertEquals(0, body.getJSONArray("entries").length());
      assertEquals(0, body.getJSONArray("totals").length());
      serverQInstance.getTable("helpContent").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      assertEquals(403, Unirest.get(getBaseUrlAndPath() + "/help/coverage").asString().getStatus());
   }
}
