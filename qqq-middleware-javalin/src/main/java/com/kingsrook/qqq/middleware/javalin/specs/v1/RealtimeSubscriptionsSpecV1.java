/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.realtime.RealtimeExecutor;
import com.kingsrook.qqq.middleware.javalin.realtime.RealtimeHub;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.BasicErrorResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeSubscriptionRequestV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeSubscriptionResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.Content;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.RequestBody;
import com.kingsrook.qqq.openapi.model.Schema;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;


/*******************************************************************************
 ** Change channels on a connection belonging to the authenticated session.
 *******************************************************************************/
public class RealtimeSubscriptionsSpecV1 extends AbstractEndpointSpec<RealtimeExecutor.Input, RealtimeExecutor.Output, RealtimeExecutor>
{
   private final Supplier<RealtimeHub> hub;



   /*******************************************************************************
    ** Share the stream spec hub, scoped to this middleware server.
    *******************************************************************************/
   public RealtimeSubscriptionsSpecV1(Supplier<RealtimeHub> hub)
   {
      this.hub = hub;
   }



   /*******************************************************************************
    ** The standalone spec constructor supports schema tooling.
    *******************************************************************************/
   public RealtimeSubscriptionsSpecV1()
   {
      this(() ->
      {
         throw new IllegalStateException("Bind the subscriptions spec to a stream hub");
      });
   }



   /*******************************************************************************
    ** Channel grammar and refusal semantics are part of the public contract.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/realtime/subscriptions").withHttpMethod(HttpMethod.POST).withTag(TagsV1.GENERAL)
         .withShortSummary("Change realtime subscriptions")
         .withLongDescription("Add or remove record:{table}:{pk}, table:{table}, user:me, or instance channels. "
            + "Records require a permission-checked GetAction and tables require READ. Refusals give no reason. "
            + "The connection must belong to the authenticated session; otherwise 404. Active user channels are returned as user:me. Channel limits apply."));
   }



   /*******************************************************************************
    ** Typed JSON request body.
    *******************************************************************************/
   @Override
   public RequestBody defineRequestBody()
   {
      return (new RequestBody().withContent(Map.of("application/json", new Content().withSchema(new RealtimeSubscriptionRequestV1().toSchema()))));
   }



   /*******************************************************************************
    ** Active channels and refused requested channels.
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("Current subscriptions and refused additions.", "RealtimeSubscriptionResponseV1"));
   }



   /*******************************************************************************
    ** Publish both sides of the subscription contract.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      return (Map.of("BasicErrorResponseV1", new BasicErrorResponseV1().toSchema(), "RealtimeSubscriptionRequestV1", new RealtimeSubscriptionRequestV1().toSchema(),
         "RealtimeSubscriptionResponseV1", new RealtimeSubscriptionResponseV1().toSchema()));
   }



   /*******************************************************************************
    ** Reject malformed bodies before they reach permission checks.
    *******************************************************************************/
   @Override
   public RealtimeExecutor.Input buildInput(Context context) throws QBadRequestException
   {
      try
      {
         JSONObject body = new JSONObject(context.body());
         RealtimeExecutor.Input input = new RealtimeExecutor.Input();
         input.hub = hub.get();
         input.connectionId = body.getString("connectionId");
         input.add = channels(body, "add");
         input.remove = channels(body, "remove");
         return (input);
      }
      catch(org.json.JSONException e)
      {
         throw (new QBadRequestException("Expected connectionId and arrays of channel strings"));
      }
   }



   /*******************************************************************************
    ** Require string arrays, with a bounded request size.
    *******************************************************************************/
   private List<String> channels(JSONObject body, String key) throws QBadRequestException
   {
      if(!body.has(key))
      {
         return (List.of());
      }
      JSONArray array = body.getJSONArray(key);
      if(array.length() > 10000)
      {
         throw (new QBadRequestException("Too many channels"));
      }
      List<String> result = new ArrayList<>();
      for(int i = 0; i < array.length(); i++)
      {
         result.add(array.getString(i));
      }
      return (result);
   }



   /*******************************************************************************
    ** Only the typed wire result is serialized.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, RealtimeExecutor.Output output)
   {
      context.contentType("application/json").result(JsonUtils.toJson(output.subscriptions));
   }


   /*******************************************************************************
    ** Do not distinguish unknown connections from another session's connection.
    *******************************************************************************/
   @Override
   public List<BasicResponse> defineAdditionalBasicResponses()
   {
      return (List.of(new BasicResponse(HttpStatus.NOT_FOUND, "Connection not found.", "BasicErrorResponseV1"),
         new BasicResponse(HttpStatus.BAD_REQUEST, "Malformed subscription request.", "BasicErrorResponseV1")));
   }
}

