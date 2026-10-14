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


import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.delivery.DeliverOutputAction;
import com.kingsrook.qqq.backend.core.delivery.DeliveryAttachment;
import com.kingsrook.qqq.backend.core.delivery.DeliveryDestinationContext;
import com.kingsrook.qqq.backend.core.delivery.DeliveryDestinationInterface;
import com.kingsrook.qqq.backend.core.delivery.DeliveryDestinationsMetaData;
import com.kingsrook.qqq.backend.core.delivery.DeliveryMetaDataProvider;
import com.kingsrook.qqq.backend.core.delivery.DeliveryRequest;
import com.kingsrook.qqq.backend.core.delivery.DeliveryResult;
import com.kingsrook.qqq.backend.core.delivery.DeliveryTargetOption;
import com.kingsrook.qqq.backend.core.delivery.DeliveryTargetStep;
import com.kingsrook.qqq.backend.core.delivery.DownloadDeliveryDestination;
import com.kingsrook.qqq.backend.core.delivery.QDeliveryDestinationMetaData;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.invoke.UiApiInProcessSessions;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Real HTTP contracts and owner isolation for all three delivery operations.
 *******************************************************************************/
class DeliverySpecV1Test extends SpecTestBase
{
   /*******************************************************************************
    ** Primary route.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new DeliveryDestinationsSpecV1());
   }



   /*******************************************************************************
    ** The other delivery routes share the standard authenticated pipeline.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new DeliveryTargetsSpecV1(), new DeliveryOutputSpecV1()));
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
    ** Memory storage and one invisible destination.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = TestUtils.defineInstance();
      new DeliveryMetaDataProvider().defineAll(instance, TestUtils.BACKEND_NAME_MEMORY, null);
      DeliveryDestinationsMetaData.of(instance).withDestination(DownloadDeliveryDestination.metadata().withName("HIDDEN").withPermissionName("hidden"));
      DeliveryDestinationsMetaData.of(instance).withDestination(new QDeliveryDestinationMetaData().withName("TENANT").withLabel("Tenant destination")
         .withCodeReference(new QCodeReference(TenantDestination.class)));
      instance.addTable(new QTableMetaData().withName("deliveryStorage").withBackendName(TestUtils.BACKEND_NAME_MEMORY).withPrimaryKeyField("reference")
         .withField(new QFieldMetaData("reference", QFieldType.STRING)).withField(new QFieldMetaData("contents", QFieldType.BLOB)));
      new QInstanceEnricher(instance).enrich();
      return (instance);
   }



   /*******************************************************************************
    ** Listing strips private metadata and targets distinguish no hidden names.
    *******************************************************************************/
   @Test
   void destinationsAndTargets() throws Exception
   {
      String token = token("A", false);
      try
      {
         HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/delivery/destinations").header(UiApiInProcessSessions.HEADER, token).asString();
         assertEquals(200, response.getStatus(), response.getBody());
         JSONObject body = new JSONObject(response.getBody());
         assertEquals(3, body.getJSONArray("destinations").length());
         assertThat(response.getBody()).doesNotContain("codeReference", "emailProviderName", "HIDDEN");
         response = Unirest.post(getBaseUrlAndPath() + "/delivery/destinations/DOWNLOAD/targets").header(UiApiInProcessSessions.HEADER, token)
            .header("Content-Type", "application/json").body("{\"chosen\":{}}").asString();
         assertEquals(200, response.getStatus(), response.getBody());
         assertEquals(true, new JSONObject(response.getBody()).getBoolean("complete"));
         for(String name : List.of("HIDDEN", "missing"))
         {
            assertEquals(404, Unirest.post(getBaseUrlAndPath() + "/delivery/destinations/" + name + "/targets")
               .header(UiApiInProcessSessions.HEADER, token).header("Content-Type", "application/json").body("{\"chosen\":{}}").asString().getStatus());
         }
         assertEquals(400, Unirest.post(getBaseUrlAndPath() + "/delivery/destinations/DOWNLOAD/targets")
            .header(UiApiInProcessSessions.HEADER, token).header("Content-Type", "application/json").body("[]").asString().getStatus());
      }
      finally
      {
         UiApiInProcessSessions.revoke(token);
      }
   }



   /*******************************************************************************
    ** A two-tenant picker receives each real HTTP caller's session, including on crafted selections.
    *******************************************************************************/
   @Test
   void tenantPickerIsolation() throws Exception
   {
      for(String user : List.of("A", "B"))
      {
         String token = token(user, false);
         try
         {
            HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/delivery/destinations/TENANT/targets")
               .header(UiApiInProcessSessions.HEADER, token).header("Content-Type", "application/json")
               .body("{\"chosen\":{\"tenant\":\"A\"}}").asString();
            assertEquals(200, response.getStatus(), response.getBody());
            var options = new JSONObject(response.getBody()).getJSONArray("options");
            assertEquals(1, options.length());
            assertEquals(user, options.getJSONObject(0).getString("value"));
         }
         finally
         {
            UiApiInProcessSessions.revoke(token);
         }
      }
   }



   /*******************************************************************************
    ** Stream an actual stored object, denying other users and allowing administrators.
    *******************************************************************************/
   @Test
   void outputOwnerAndAdmin() throws Exception
   {
      QSession owner = session("A", false);
      QContext.init(serverQInstance, owner);
      try(OutputStream stream = new StorageAction().createOutputStream(new StorageInput("deliveryStorage").withReference("output")))
      {
         stream.write("private output".getBytes(StandardCharsets.UTF_8));
      }
      String path = new DeliverOutputAction().execute(new DeliveryRequest().withOwnerUserId("A").withDestinationName("DOWNLOAD")
         .withIdempotencyKey("http-output").withAttachments(List.of(new DeliveryAttachment().withFileName("output.txt").withSize(14L)
            .withStorageTableName("deliveryStorage").withStorageReference("output")))).getExternalReference();
      String url = "http://localhost:" + PORT + path;
      for(String user : List.of("A", "B", "admin"))
      {
         String token = token(user, "admin".equals(user));
         try
         {
            HttpResponse<String> response = Unirest.get(url).header(UiApiInProcessSessions.HEADER, token).asString();
            assertEquals("B".equals(user) ? 404 : 200, response.getStatus(), response.getBody());
            if(!"B".equals(user))
            {
               assertEquals("private output", response.getBody());
               assertEquals("private, no-store", response.getHeaders().getFirst("Cache-Control"));
            }
         }
         finally
         {
            UiApiInProcessSessions.revoke(token);
         }
      }
   }



   /*******************************************************************************
    ** Trusted in-process authentication still traverses the real HTTP spec pipeline.
    *******************************************************************************/
   private String token(String user, boolean admin)
   {
      return (UiApiInProcessSessions.register(session(user, admin), Duration.ofMinutes(1)));
   }



   /*******************************************************************************
    ** Build isolated users with explicit administrative grants.
    *******************************************************************************/
   private QSession session(String user, boolean admin)
   {
      QSession session = new QSession().withUser(new QUser().withIdReference(user));
      if(admin)
      {
         session.withPermission(DeliveryMetaDataProvider.ADMIN_PERMISSION);
      }
      return (session);
   }



   /*******************************************************************************
    ** A fake provider whose available tenant comes exclusively from the authenticated session.
    *******************************************************************************/
   public static class TenantDestination implements DeliveryDestinationInterface
   {
      /*******************************************************************************
       ** Client-supplied tenant choices cannot expand the caller's available options.
       *******************************************************************************/
      @Override
      public DeliveryTargetStep describeTargets(DeliveryDestinationContext context, Map<String, Serializable> chosen)
      {
         String user = QContext.getQSession().getUser().getIdReference();
         return (new DeliveryTargetStep().withName("tenant").withOptions(List.of(new DeliveryTargetOption().withValue(user).withLabel(user))));
      }



      /*******************************************************************************
       ** The picker tests never send output.
       *******************************************************************************/
      @Override
      public DeliveryResult deliver(DeliveryRequest request)
      {
         throw new UnsupportedOperationException("Picker-only fixture");
      }
   }
}
