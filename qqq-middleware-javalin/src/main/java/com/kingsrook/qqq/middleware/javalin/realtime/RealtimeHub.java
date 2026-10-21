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

package com.kingsrook.qqq.middleware.javalin.realtime;


import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.actions.collaboration.RuntimeMetaDataOverlay;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBus;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMetaData;


/*******************************************************************************
 ** One server connection registry. Replay stays session-bound and is re-filtered at delivery.
 *******************************************************************************/
public class RealtimeHub implements AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(RealtimeHub.class);
   private static final int MAX_BUFFERED_EVENTS = 10000;
   private final QInstance instance;
   private final RealtimeMetaData metadata;
   private final Clock clock;
   private final Map<String, Connection> connections = new LinkedHashMap<>();
   private final AutoCloseable subscription;
   private final ScheduledExecutorService timer;



   /*******************************************************************************
    ** Start one bus subscription and a daemon maintenance timer.
    *******************************************************************************/
   public RealtimeHub(QInstance instance)
   {
      this(instance, Clock.systemUTC());
   }



   /*******************************************************************************
    ** A clock seam makes retention and permission rechecks deterministic in tests.
    *******************************************************************************/
   public RealtimeHub(QInstance instance, Clock clock)
   {
      this.instance = instance;
      this.clock = clock;
      this.metadata = RealtimeMetaData.of(instance);
      subscription = RealtimeBus.get(instance).subscribe(this::receive);
      timer = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().name("qqq-realtime-maintenance").factory());
      timer.scheduleWithFixedDelay(this::maintenance, 1, 1, TimeUnit.SECONDS);
   }



   /*******************************************************************************
    ** Attach a transport, resuming only an owned connection with a retained event id.
    *******************************************************************************/
   public synchronized Attachment connect(QSession session, String lastEventId)
   {
      Connection connection = null;
      long sequence = -1;
      if(lastEventId != null)
      {
         int separator = lastEventId.lastIndexOf(':');
         if(separator > 0)
         {
            connection = connections.get(lastEventId.substring(0, separator));
            try
            {
               sequence = Long.parseLong(lastEventId.substring(separator + 1));
            }
            catch(NumberFormatException ignored)
            {
               sequence = -1;
            }
         }
      }
      if(connection != null && !Objects.equals(connection.session.getUuid(), session.getUuid()))
      {
         connection = null;
      }
      boolean resumed = false;
      List<Frame> replay = List.of();
      if(connection != null)
      {
         trim(connection);
         long requestedSequence = sequence;
         resumed = connection.replay.stream().anyMatch(frame -> frame.sequence() == requestedSequence);
         if(resumed)
         {
            replay = connection.replay.stream().filter(frame -> frame.sequence() > requestedSequence).toList();
         }
      }
      if(connection == null)
      {
         List<Connection> owned = connections.values().stream().filter(value -> Objects.equals(value.session.getUuid(), session.getUuid())).toList();
         if(owned.size() >= metadata.getMaxConnectionsPerSession())
         {
            end(owned.get(0));
         }
         connection = new Connection(session, clock.instant());
         connections.put(connection.id, connection);
      }
      if(connection.attachment != null)
      {
         connection.attachment.finish();
      }
      connection.session = session;
      connection.detachedAt = null;
      Attachment attachment = new Attachment(connection.id);
      connection.attachment = attachment;
      Connection target = connection;
      Map<String, Serializable> hello = Map.of("connectionId", connection.id, "heartbeatSeconds", metadata.getHeartbeatSeconds(),
         "metaDataGeneration", RuntimeMetaDataOverlay.getGeneration(instance));
      if(resumed)
      {
         attachment.offer(new Frame(sequence, connection.id + ":" + sequence, null, "hello", hello, clock.instant()));
      }
      else
      {
         emit(connection, null, "hello", hello);
      }
      List<String> revoked = target.channels.stream().filter(channel -> !allowed(target, channel)).toList();
      target.channels.removeAll(revoked);
      for(Frame frame : replay)
      {
         Map<String, Serializable> data = filtered(target, frame.channel(), frame.event(), frame.data());
         if(data != null && (frame.channel() == null || (target.channels.contains(frame.channel()))))
         {
            attachment.offer(new Frame(frame.sequence(), frame.id(), frame.channel(), frame.event(), data, frame.at()));
         }
      }
      revoked.forEach(channel -> emit(target, null, "channel-revoked", Map.of("channel", channel)));
      if(lastEventId != null && !resumed)
      {
         emit(connection, null, "resync", Map.of());
      }
      return (attachment);
   }



   /*******************************************************************************
    ** Ownership is intentionally indistinguishable from a missing connection.
    *******************************************************************************/
   public synchronized Map<String, List<String>> subscriptions(QSession session, String connectionId, List<String> add, List<String> remove) throws QNotFoundException
   {
      Connection connection = connections.get(connectionId);
      if(connection == null || !Objects.equals(connection.session.getUuid(), session.getUuid()))
      {
         throw (new QNotFoundException("Connection not found"));
      }
      connection.session = session;
      for(String channel : remove)
      {
         connection.channels.remove(normalize(connection, channel));
      }
      List<String> refused = new ArrayList<>();
      for(String channel : add)
      {
         String normalized = normalize(connection, channel);
         if(normalized == null || !allowed(connection, normalized)
            || (!connection.channels.contains(normalized) && connection.channels.size() >= metadata.getMaxChannelsPerConnection()))
         {
            refused.add(channel);
         }
         else
         {
            connection.channels.add(normalized);
         }
      }
      return (Map.of("active", connection.channels.stream().map(channel -> channel.equals("user:" + connection.userId) ? "user:me" : channel).toList(), "refused", refused));
   }



   /*******************************************************************************
    ** Only the user:me alias can request a user channel.
    *******************************************************************************/
   private String normalize(Connection connection, String channel)
   {
      if(channel == null)
      {
         return (null);
      }
      if(channel.equals("user:me"))
      {
         return (connection.userId == null ? null : "user:" + connection.userId);
      }
      return (channel.startsWith("user:") ? null : channel);
   }



   /*******************************************************************************
    ** Use normal core record reads and table permissions, refusing all failures.
    *******************************************************************************/
   private boolean allowed(Connection connection, String channel)
   {
      boolean[] allowed = {false};
      try
      {
         QContext.withTemporaryContext(new CapturedContext(instance, connection.session), () ->
         {
            if("instance".equals(channel) || (connection.userId != null && ("user:" + connection.userId).equals(channel)))
            {
               allowed[0] = true;
               return;
            }
            String[] parts = channel.split(":", 3);
            if(parts.length < 2 || instance.getTable(parts[1]) == null)
            {
               return;
            }
            GetInput input = new GetInput(parts[1]).withInputSource(QInputSource.USER);
            if(!PermissionsHelper.hasTablePermission(input, parts[1], TablePermissionSubType.READ))
            {
               return;
            }
            if(parts.length == 2 && "table".equals(parts[0]))
            {
               allowed[0] = true;
            }
            else if(parts.length == 3 && "record".equals(parts[0]) && !parts[2].isEmpty())
            {
               allowed[0] = new GetAction().execute(input.withPrimaryKey(parts[2])).getRecord() != null;
            }
         });
      }
      catch(Exception e)
      {
         LOG.debug("Realtime channel refused", e);
      }
      return (allowed[0]);
   }



   /*******************************************************************************
    ** Field names are filtered with the same personalized metadata as the frontend.
    *******************************************************************************/
   private Map<String, Serializable> filtered(Connection connection, String channel, String event, Map<String, Serializable> data)
   {
      Map<String, Serializable> result = new LinkedHashMap<>(data);
      if(event.startsWith("record."))
      {
         try
         {
            QContext.withTemporaryContext(new CapturedContext(instance, connection.session), () ->
            {
               QTableMetaData table = TableMetaDataPersonalizerAction.execute(new GetInput(String.valueOf(data.get("table"))).withInputSource(QInputSource.USER), true);
               Map<String, QFieldMetaData> visibleFields = new LinkedHashMap<>(table.getFields());
               if(table.getVirtualFields() != null)
               {
                  visibleFields.putAll(table.getVirtualFields());
               }
               if(data.get("fields") instanceof List<?> fields)
               {
                  result.put("fields", new ArrayList<>(fields.stream().filter(String.class::isInstance).map(String.class::cast)
                     .filter(name -> visibleFields.containsKey(name) && !visibleFields.get(name).getIsHidden()).toList()));
               }
            });
         }
         catch(Exception e)
         {
            LOG.debug("Realtime record hint could not be filtered", e);
            return (null);
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Bus callbacks never perform socket writes; each stream has a bounded queue.
    *******************************************************************************/
   private synchronized void receive(RealtimeMessage message)
   {
      if("session-ended".equals(message.getEvent()) && message.getChannel().startsWith("session:"))
      {
         endSession(message.getChannel().substring("session:".length()));
         return;
      }
      for(Connection connection : List.copyOf(connections.values()))
      {
         if(connection.channels.contains(message.getChannel()))
         {
            emit(connection, message.getChannel(), message.getEvent(), message.getData());
         }
      }
   }



   /*******************************************************************************
    ** Allocate a connection-local id and retain the filtered hint for reconnects.
    *******************************************************************************/
   private void emit(Connection connection, String channel, String event, Map<String, Serializable> data)
   {
      Map<String, Serializable> filtered = filtered(connection, channel, event, data);
      if(filtered == null)
      {
         return;
      }
      long sequence = ++connection.sequence;
      Frame frame = new Frame(sequence, connection.id + ":" + sequence, channel, event, filtered, clock.instant());
      connection.replay.addLast(frame);
      trim(connection);
      if(connection.attachment != null && !connection.attachment.offer(frame))
      {
         connection.attachment.finish();
         connection.attachment = null;
         connection.detachedAt = clock.instant();
      }
   }



   /*******************************************************************************
    ** Bound replay by both age and count; an evicted cursor requires resync.
    *******************************************************************************/
   private void trim(Connection connection)
   {
      Instant cutoff = clock.instant().minusSeconds(metadata.getReplaySeconds());
      while(!connection.replay.isEmpty() && (connection.replay.peekFirst().at().isBefore(cutoff) || connection.replay.size() > MAX_BUFFERED_EVENTS))
      {
         connection.replay.removeFirst();
      }
   }



   /*******************************************************************************
    ** Transport closure retains subscriptions for the replay window.
    *******************************************************************************/
   public synchronized void disconnect(Attachment attachment)
   {
      Connection connection = connections.get(attachment.connectionId);
      if(connection != null && connection.attachment == attachment)
      {
         connection.attachment = null;
         connection.detachedAt = clock.instant();
      }
      attachment.finish();
   }



   /*******************************************************************************
    ** Validate expiry at each heartbeat and periodically recheck every channel.
    *******************************************************************************/
   public synchronized void maintenance()
   {
      Instant now = clock.instant();
      for(Connection connection : List.copyOf(connections.values()))
      {
         try
         {
            trim(connection);
            if(connection.detachedAt != null && !connection.detachedAt.plusSeconds(metadata.getReplaySeconds()).isAfter(now))
            {
               connections.remove(connection.id);
               continue;
            }
            if(!connection.lastHeartbeat.plusSeconds(metadata.getHeartbeatSeconds()).isAfter(now))
            {
               try
               {
                  QContext.withTemporaryContext(new CapturedContext(instance, connection.session), () -> ActionHelper.validateSession(null));
               }
               catch(Exception e)
               {
                  end(connection);
                  continue;
               }
               connection.lastHeartbeat = now;
               if(connection.attachment != null)
               {
                  connection.attachment.offer(new Frame(0, null, null, null, Map.of(), now));
               }
            }
            if(!connection.lastRecheck.plusSeconds(metadata.getRecheckMinutes() * 60L).isAfter(now))
            {
               connection.lastRecheck = now;
               for(String channel : List.copyOf(connection.channels))
               {
                  if(!allowed(connection, channel))
                  {
                     connection.channels.remove(channel);
                     emit(connection, null, "channel-revoked", Map.of("channel", channel));
                  }
               }
            }
         }
         catch(Exception e)
         {
            LOG.warn("Realtime maintenance failed; closing connection", e);
            end(connection);
         }
      }
   }



   /*******************************************************************************
    ** Logout closes every stream belonging to this session.
    *******************************************************************************/
   public synchronized void endSession(String sessionUuid)
   {
      connections.values().stream().filter(connection -> Objects.equals(connection.session.getUuid(), sessionUuid)).toList().forEach(this::end);
   }



   /*******************************************************************************
    ** Queue the terminal event before the transport close marker.
    *******************************************************************************/
   private void end(Connection connection)
   {
      emit(connection, null, "session-ended", Map.of());
      if(connection.attachment != null)
      {
         connection.attachment.finish();
      }
      connections.remove(connection.id);
   }



   /*******************************************************************************
    ** Unsubscribe and release all streams and the maintenance timer.
    *******************************************************************************/
   @Override
   public synchronized void close()
   {
      timer.shutdownNow();
      List.copyOf(connections.values()).forEach(this::end);
      try
      {
         subscription.close();
      }
      catch(Exception e)
      {
         LOG.warn("Could not unsubscribe realtime hub", e);
      }
   }



   /*******************************************************************************
    ** Wire frame, with a null event representing a comment ping.
    *******************************************************************************/
   public record Frame(long sequence, String id, String channel, String event, Map<String, Serializable> data, Instant at)
   {
   }



   /*******************************************************************************
    ** A bounded transport queue; empty event is the internal close marker.
    *******************************************************************************/
   public static class Attachment
   {
      private final String connectionId;
      private final BlockingQueue<Frame> queue = new LinkedBlockingQueue<>(MAX_BUFFERED_EVENTS);



      /*******************************************************************************
       ** Bind this transport to a logical connection.
       *******************************************************************************/
      private Attachment(String connectionId)
      {
         this.connectionId = connectionId;
      }



      /*******************************************************************************
       ** Read the next wire frame without polling in a busy loop.
       *******************************************************************************/
      public Frame poll(long timeoutMillis) throws InterruptedException
      {
         return (queue.poll(timeoutMillis, TimeUnit.MILLISECONDS));
      }



      /*******************************************************************************
       ** Connection identifier used by the subscription endpoint.
       *******************************************************************************/
      public String getConnectionId()
      {
         return (connectionId);
      }



      /*******************************************************************************
       ** Never block a bus publisher behind a slow client.
       *******************************************************************************/
      private boolean offer(Frame frame)
      {
         return (queue.offer(frame));
      }



      /*******************************************************************************
       ** Preserve queued final messages unless the transport has fallen behind.
       *******************************************************************************/
      private void finish()
      {
         Frame terminal = new Frame(0, null, null, "", Map.of(), Instant.EPOCH);
         if(!queue.offer(terminal))
         {
            queue.clear();
            queue.offer(terminal);
         }
      }
   }



   /*******************************************************************************
    ** All logical connection state is guarded by the hub monitor.
    *******************************************************************************/
   private static class Connection
   {
      private final String id = UUID.randomUUID().toString();
      private final String userId;
      private QSession session;
      private final Set<String> channels = new LinkedHashSet<>();
      private final Deque<Frame> replay = new ArrayDeque<>();
      private long sequence;
      private Instant detachedAt;
      private Instant lastHeartbeat;
      private Instant lastRecheck;
      private Attachment attachment;



      /*******************************************************************************
       ** Capture session ownership and maintenance deadlines.
       *******************************************************************************/
      private Connection(QSession session, Instant now)
      {
         this.session = session;
         userId = session.getUser() == null ? null : session.getUser().getIdReference();
         lastHeartbeat = now;
         lastRecheck = now;
      }
   }
}

