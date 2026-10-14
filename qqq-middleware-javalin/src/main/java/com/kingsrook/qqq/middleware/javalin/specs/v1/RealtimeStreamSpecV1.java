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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.realtime.RealtimeExecutor;
import com.kingsrook.qqq.middleware.javalin.realtime.RealtimeHub;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.BasicOperation;
import com.kingsrook.qqq.middleware.javalin.specs.BasicResponse;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeChannelRevokedEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeHelloEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeMetaDataChangedEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeRecordChangedEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeRecordDeletedEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeResyncEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeSessionEndedEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RealtimeTableChangedEventV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.utils.TagsV1;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.qqq.openapi.model.In;
import com.kingsrook.qqq.openapi.model.Parameter;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.sse.SseClient;


/*******************************************************************************
 ** Authenticated v1 SSE stream. One virtual writer per stream; hub callbacks only enqueue.
 *******************************************************************************/
public class RealtimeStreamSpecV1 extends AbstractEndpointSpec<RealtimeExecutor.Input, RealtimeExecutor.Output, RealtimeExecutor> implements AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(RealtimeStreamSpecV1.class);
   private RealtimeHub hub;



   /*******************************************************************************
    ** Declare the streaming contract without an extension-point dependency.
    *******************************************************************************/
   @Override
   public BasicOperation defineBasicOperation()
   {
      return (new BasicOperation().withPath("/realtime/stream").withHttpMethod(HttpMethod.GET).withTag(TagsV1.GENERAL)
         .withShortSummary("Open the realtime event stream")
         .withLongDescription("SSE invalidation hints. Each event has id <connectionId>:<sequence>, an event name and JSON data. "
            + "hello identifies the connection; comment pings maintain it. Last-Event-ID replays retained events on the same pod and session, otherwise resync asks clients to refetch. "
            + "Core event payloads use the Realtime*EventV1 components. Qbits publish through RealtimeBus with their own event names, such as presence.changed and comment.added, described by RealtimeEventV1. "
            + "Subscriptions require a separate POST; session-ended closes the stream and channel-revoked removes a subscription."));
   }



   /*******************************************************************************
    ** A reconnect cursor never grants ownership of another session connection.
    *******************************************************************************/
   @Override
   public List<Parameter> defineRequestParameters()
   {
      return (List.of(new Parameter().withName("Last-Event-ID").withIn(In.HEADER).withRequired(false)
         .withSchema(new Schema().withType(Type.STRING)).withDescription("Last received <connectionId>:<sequence> cursor.")));
   }



   /*******************************************************************************
    ** The 200 response is explicitly a stream, not a JSON response.
    *******************************************************************************/
   @Override
   public BasicResponse defineBasicSuccessResponse()
   {
      return (new BasicResponse("text/event-stream", HttpStatus.OK, "Realtime named events and comment heartbeats.", "RealtimeEventV1", null));
   }



   /*******************************************************************************
    ** Every named core event is a separate schema component.
    *******************************************************************************/
   @Override
   public Map<String, Schema> defineComponentSchemas()
   {
      Map<String, Schema> schemas = new LinkedHashMap<>();
      schemas.put("RealtimeHelloEventV1", new RealtimeHelloEventV1().toSchema());
      schemas.put("RealtimeRecordChangedEventV1", new RealtimeRecordChangedEventV1().toSchema());
      schemas.put("RealtimeRecordDeletedEventV1", new RealtimeRecordDeletedEventV1().toSchema());
      schemas.put("RealtimeTableChangedEventV1", new RealtimeTableChangedEventV1().toSchema());
      schemas.put("RealtimeMetaDataChangedEventV1", new RealtimeMetaDataChangedEventV1().toSchema());
      schemas.put("RealtimeResyncEventV1", new RealtimeResyncEventV1().toSchema());
      schemas.put("RealtimeSessionEndedEventV1", new RealtimeSessionEndedEventV1().toSchema());
      schemas.put("RealtimeChannelRevokedEventV1", new RealtimeChannelRevokedEventV1().toSchema());
      schemas.put("RealtimeEventV1", new RealtimeEventV1().toSchema());
      return (schemas);
   }



   /*******************************************************************************
    ** Initialize lazily so OpenAPI generation does not start resources.
    *******************************************************************************/
   public synchronized RealtimeHub getHub()
   {
      if(hub == null)
      {
         hub = new RealtimeHub(qInstance);
      }
      return (hub);
   }



   /*******************************************************************************
    ** A metadata hot swap retires connections bound to the prior instance.
    *******************************************************************************/
   @Override
   public synchronized void setQInstance(QInstance instance)
   {
      if(qInstance != instance)
      {
         close();
      }
      super.setQInstance(instance);
   }



   /*******************************************************************************
    ** Authentication has already established QContext before this runs.
    *******************************************************************************/
   @Override
   public RealtimeExecutor.Input buildInput(Context context)
   {
      RealtimeExecutor.Input input = new RealtimeExecutor.Input();
      input.hub = getHub();
      return (input);
   }



   /*******************************************************************************
    ** Use Javalin SSE lifetime handling and one virtual writer for ordered frames.
    *******************************************************************************/
   @Override
   public void handleOutput(Context context, RealtimeExecutor.Output output)
   {
      RealtimeHub.Attachment attachment = output.hub.connect(QContext.getQSession(), context.header("Last-Event-ID"));
      context.contentType("text/event-stream");
      context.header("Cache-Control", "no-cache");
      context.header("X-Accel-Buffering", "no");
      context.res().setCharacterEncoding("UTF-8");
      context.async(config -> config.timeout = 0, () ->
      {
         SseClient client = new SseClient(context);
         client.keepAlive();
         client.onClose(() -> output.hub.disconnect(attachment));
         Thread.ofVirtual().name("qqq-realtime-stream").start(() ->
         {
            try
            {
               while(!client.terminated())
               {
                  RealtimeHub.Frame frame = attachment.poll(1000);
                  if(frame == null)
                  {
                     continue;
                  }
                  if("".equals(frame.event()))
                  {
                     break;
                  }
                  if(frame.event() == null)
                  {
                     client.sendComment("ping");
                  }
                  else
                  {
                     client.sendEvent(frame.event(), JsonUtils.toJson(frame.data()), frame.id());
                  }
               }
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
            }
            catch(Exception e)
            {
               LOG.debug("Realtime stream disconnected", e);
            }
            finally
            {
               client.close();
            }
         });
      });
   }



   /*******************************************************************************
    ** Server shutdown releases the hub even if no clients remain.
    *******************************************************************************/
   @Override
   public synchronized void close()
   {
      if(hub != null)
      {
         hub.close();
         hub = null;
      }
   }
}

