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
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.collaboration.RuntimeMetaDataOverlay;
import com.kingsrook.qqq.backend.core.actions.collaboration.RuntimeMetaDataProviderInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeTableMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.fields.JsonPathStorage;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class RuntimeFieldMetaDataSpecV1Test extends SpecTestBase
{
   /*******************************************************************************
    ** Test the existing versioned table endpoint.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new TableMetaDataSpecV1());
   }



   /*******************************************************************************
    ** Version under test.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** An already running server immediately exposes additions and removals.
    *******************************************************************************/
   @Test
   void runtimeFieldsAppearWithoutRestart() throws Exception
   {
      assertFalse(metadata().getJSONObject("fields").has("runtimeNote"));
      RecordFeaturesMetaData.ofOrWithNew(serverQInstance).withRuntimeProviders(List.of(new QCodeReference(Provider.class)));
      RuntimeMetaDataOverlay.refresh(serverQInstance);
      JSONObject added = metadata();
      assertTrue(added.getJSONObject("fields").has("runtimeNote"));
      assertEquals("Runtime Note", added.getJSONObject("fields").getJSONObject("runtimeNote").getString("label"));
      assertEquals(1, added.getLong("metaDataGeneration"));
      RecordFeaturesMetaData.ofOrWithNew(serverQInstance).withRuntimeProviders(List.of());
      RuntimeMetaDataOverlay.refresh(serverQInstance);
      JSONObject removed = metadata();
      assertFalse(removed.getJSONObject("fields").has("runtimeNote"));
      assertEquals(2, removed.getLong("metaDataGeneration"));
   }



   /*******************************************************************************
    ** Fetch fresh metadata over HTTP.
    *******************************************************************************/
   private JSONObject metadata()
   {
      var response = Unirest.get(getBaseUrlAndPath() + "/metaData/table/person").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      return (new JSONObject(response.getBody()));
   }



   /*******************************************************************************
    ** Supply one runtime field.
    *******************************************************************************/
   public static class Provider implements RuntimeMetaDataProviderInterface
   {
      /***************************************************************************
       ** Contribution for the server fixture.
       ***************************************************************************/
      @Override
      public RuntimeMetaDataContribution getContribution(QInstance instance)
      {
         return (new RuntimeMetaDataContribution().withTables(Map.of("person", new RuntimeTableMetaDataContribution()
            .withFields(List.of(new QFieldMetaData("runtimeNote", QFieldType.STRING)
               .withBackendStorage(new JsonPathStorage().withColumnName("qqq_custom_fields").withKey("runtime_note")))))));
      }
   }
}

