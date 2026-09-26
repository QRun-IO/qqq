/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.api;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.connection.EsbConnectionManager;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapter;
import com.kingsrook.qqq.esb.management.EsbBrokerAdapters;
import com.kingsrook.qqq.esb.management.EsbBrokerCapabilities;
import com.kingsrook.qqq.esb.management.EsbBrokerNames;
import com.kingsrook.qqq.esb.management.EsbBrowsedMessage;
import com.kingsrook.qqq.esb.management.EsbMessageBrowser;
import com.kingsrook.qqq.esb.management.EsbQueueInfo;
import com.kingsrook.qqq.esb.metadata.EsbAppMetaDataProducer;
import com.kingsrook.qqq.esb.model.EsbDestinationType;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessPublication;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.esb.model.EsbTablePublication;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import com.kingsrook.qqq.esb.runtime.EsbTriggerRunner;
import com.kingsrook.qqq.esb.runtime.EsbTriggerState;
import com.kingsrook.qqq.esb.runtime.QEsbRuntime;
import com.kingsrook.qqq.esb.stats.EsbCounterSnapshot;
import com.kingsrook.qqq.esb.stats.EsbStats;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Builds the bodies of the ESB endpoints (see EsbRouteProvider), as maps in
 * exactly the shape of the endpoint contract, from the QContext's instance and
 * session: ESB meta-data, this node's counters (EsbStats) and trigger states
 * (QEsbRuntime), and broker data from the management API when a provider has
 * a managementUrl.
 *
 * Checks permissions (throwing QPermissionDeniedException), and throws
 * QNotFoundException for an unknown table, process, trigger or destination, or
 * a table or process without ESB meta-data.  Never includes a provider's urls
 * or credentials.
 *
 * One builder per request: it keeps the broker adapters and queue infos it
 * looked up, so each queue is asked about once.
 *******************************************************************************/
public class EsbStatusBuilder
{
   private static final QLogger LOG = QLogger.getLogger(EsbStatusBuilder.class);

   public static final Integer DEFAULT_LIMIT = 50;

   ////////////////////////////////////////////////////////////////////
   // one more than a page is browsed, to tell whether there are more //
   ////////////////////////////////////////////////////////////////////
   public static final Integer MAX_LIMIT = EsbMessageBrowser.MAX_LIMIT - 1;

   private static final String OPERATE_PERMISSION = EsbAppMetaDataProducer.OPERATE_PERMISSION_BASE_NAME + ".hasAccess";
   private static final String DELETE_PERMISSION  = EsbAppMetaDataProducer.DELETE_PERMISSION_BASE_NAME + ".hasAccess";

   private final QInstance           qInstance;
   private final EsbInstanceMetaData esbInstanceMetaData;
   private final AbstractActionInput actionInput = new AbstractActionInput();

   private final Map<String, Optional<EsbBrokerAdapter>> adapters   = new HashMap<>();
   private final Map<String, Optional<EsbQueueInfo>>     queueInfos = new HashMap<>();

   private QEsbRuntime runtime = QEsbRuntime.getInstance();



   /*******************************************************************************
    ** Constructor - for the QContext's instance (and, for permissions, session).
    *******************************************************************************/
   public EsbStatusBuilder()
   {
      this.qInstance = QContext.getQInstance();
      this.esbInstanceMetaData = QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
   }



   /*******************************************************************************
    ** GET /table/{table}: the table's publications, the triggers subscribed to
    ** their destinations (on processes the user can access), and permissions.
    ** Needs READ on the table.
    *******************************************************************************/
   public Map<String, Object> buildTable(String tableName) throws QException
   {
      QTableMetaData table = qInstance.getTable(tableName);
      if(table == null)
      {
         throw (new QNotFoundException("Table " + tableName + " was not found."));
      }

      if(!PermissionsHelper.hasTablePermission(actionInput, tableName, TablePermissionSubType.READ))
      {
         throw (new QPermissionDeniedException("Permission denied."));
      }

      EsbTableMetaData esbTableMetaData = EsbTableMetaData.of(table);
      if(esbTableMetaData == null || esbInstanceMetaData == null)
      {
         throw (new QNotFoundException("Table " + tableName + " has no ESB meta-data."));
      }

      List<Map<String, Object>> publications     = new ArrayList<>();
      Set<String>               destinationNames = new LinkedHashSet<>();
      for(EsbTablePublication publication : CollectionUtils.nonNullList(esbTableMetaData.getPublications()))
      {
         QEsbDestinationMetaData destination = esbInstanceMetaData.getDestination(publication.getDestinationName());
         if(destination != null)
         {
            destinationNames.add(destination.getName());
            publications.add(buildPublication(destination, enumNames(publication.getEvents())));
         }
      }

      List<Map<String, Object>> subscribers = new ArrayList<>();
      for(TriggerReference triggerReference : findTriggers())
      {
         if(destinationNames.contains(triggerReference.destination().getName()) && PermissionsHelper.hasProcessPermission(actionInput, triggerReference.process().getName()))
         {
            subscribers.add(buildTrigger(triggerReference));
         }
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("table", tableName);
      result.put("publications", publications);
      result.put("subscribers", subscribers);
      result.put("permissions", buildPermissions());
      return (result);
   }



   /*******************************************************************************
    ** GET /process/{process}: the process's publications and triggers, and
    ** permissions.  Needs access to the process.
    *******************************************************************************/
   public Map<String, Object> buildProcess(String processName) throws QException
   {
      QProcessMetaData process = qInstance.getProcess(processName);
      if(process == null)
      {
         throw (new QNotFoundException("Process " + processName + " was not found."));
      }

      PermissionsHelper.checkProcessPermissionThrowing(actionInput, processName);

      EsbProcessMetaData esbProcessMetaData = EsbProcessMetaData.of(process);
      if(esbProcessMetaData == null || esbInstanceMetaData == null)
      {
         throw (new QNotFoundException("Process " + processName + " has no ESB meta-data."));
      }

      List<Map<String, Object>> publications = new ArrayList<>();
      for(EsbProcessPublication publication : CollectionUtils.nonNullList(esbProcessMetaData.getPublications()))
      {
         QEsbDestinationMetaData destination = esbInstanceMetaData.getDestination(publication.getDestinationName());
         if(destination != null)
         {
            publications.add(buildPublication(destination, enumNames(publication.getEvents())));
         }
      }

      List<Map<String, Object>> triggers = new ArrayList<>();
      for(TriggerReference triggerReference : findTriggers())
      {
         if(triggerReference.process() == process)
         {
            triggers.add(buildTrigger(triggerReference));
         }
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("process", processName);
      result.put("publications", publications);
      result.put("triggers", triggers);
      result.put("permissions", buildPermissions());
      return (result);
   }



   /*******************************************************************************
    ** GET /overview: the providers (name, type, connected, managementEnabled -
    ** never urls or credentials), every destination with its publishers and its
    ** triggers (on processes the user can access), and permissions.  Needs
    ** access to the ESB app (esbView).
    *******************************************************************************/
   public Map<String, Object> buildOverview() throws QException
   {
      PermissionsHelper.checkAppPermissionThrowing(actionInput, EsbAppMetaDataProducer.NAME);
      if(esbInstanceMetaData == null)
      {
         throw (new QNotFoundException("The instance has no ESB meta-data."));
      }

      List<Map<String, Object>> providers    = new ArrayList<>();
      List<Map<String, Object>> destinations = new ArrayList<>();
      for(QEsbProviderMetaData provider : CollectionUtils.nonNullMap(esbInstanceMetaData.getProviders()).values())
      {
         Map<String, Object> providerView = new LinkedHashMap<>();
         providerView.put("name", provider.getName());
         providerView.put("type", enumName(provider.getType()));
         providerView.put("connected", EsbConnectionManager.getInstance().isConnected(provider.getName()));
         providerView.put("managementEnabled", StringUtils.hasContent(provider.getManagementUrl()));
         providers.add(providerView);
      }

      List<TriggerReference> triggerReferences = findTriggers();
      for(QEsbDestinationMetaData destination : CollectionUtils.nonNullMap(esbInstanceMetaData.getDestinations()).values())
      {
         List<Map<String, Object>> triggers = new ArrayList<>();
         for(TriggerReference triggerReference : triggerReferences)
         {
            if(triggerReference.destination() == destination && PermissionsHelper.hasProcessPermission(actionInput, triggerReference.process().getName()))
            {
               triggers.add(buildTrigger(triggerReference));
            }
         }

         Map<String, Object> destinationView = buildDestination(destination);
         destinationView.put("publishers", buildPublishers(destination));
         destinationView.put("triggers", triggers);
         destinations.add(destinationView);
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("providers", providers);
      result.put("destinations", destinations);
      result.put("permissions", buildPermissions());
      return (result);
   }



   /*******************************************************************************
    ** GET /deadLetters/{trigger}: a page of the trigger's dead-letter queue.
    ** Needs access to the trigger's process.
    *******************************************************************************/
   public Map<String, Object> browseDeadLetters(String triggerName, Integer offset, Integer limit) throws QException
   {
      TriggerReference triggerReference = findTrigger(triggerName);
      if(triggerReference == null)
      {
         throw (new QNotFoundException("Trigger " + triggerName + " was not found."));
      }

      PermissionsHelper.checkProcessPermissionThrowing(actionInput, triggerReference.process().getName());

      String deadLetterQueue = triggerReference.trigger().getEffectiveDeadLetterDestinationName(triggerReference.process().getName(), triggerReference.destination());
      return (browse(triggerReference.destination().getProviderName(), deadLetterQueue, offset, limit));
   }



   /*******************************************************************************
    ** GET /messages/{destination}: a page of a queue's messages - or, for a
    ** topic, of one of its triggers' subscription queues (named by triggerName;
    ** required for a topic, ignored for a queue).  Needs esbView, or READ on a
    ** table that publishes to the destination, or access to a process that it
    ** triggers.  A topic subscription also requires access to its selected
    ** trigger's process.
    *******************************************************************************/
   public Map<String, Object> browseMessages(String destinationName, String triggerName, Integer offset, Integer limit) throws QException
   {
      QEsbDestinationMetaData destination = esbInstanceMetaData == null ? null : esbInstanceMetaData.getDestination(destinationName);
      if(destination == null)
      {
         throw (new QNotFoundException("Destination " + destinationName + " was not found."));
      }

      checkMessagesPermission(destination);

      String brokerQueueName = destination.getEffectiveDestinationName();
      if(destination.getType() == EsbDestinationType.TOPIC)
      {
         if(!StringUtils.hasContent(triggerName))
         {
            throw (new QBadRequestException("A topic's messages are kept on each of its triggers' subscriptions: name one of the triggers of " + destinationName + " in the trigger parameter."));
         }

         TriggerReference triggerReference = findTrigger(triggerName);
         if(triggerReference == null || triggerReference.destination() != destination)
         {
            throw (new QNotFoundException("Trigger " + triggerName + " was not found on " + destinationName + "."));
         }

         PermissionsHelper.checkProcessPermissionThrowing(actionInput, triggerReference.process().getName());
         brokerQueueName = getSubscriptionQueueName(triggerReference);
      }

      return (browse(destination.getProviderName(), brokerQueueName, offset, limit));
   }



   /*******************************************************************************
    ** Fluent setter for the runtime whose trigger states are reported (by
    ** default, QEsbRuntime.getInstance()).
    *******************************************************************************/
   public EsbStatusBuilder withRuntime(QEsbRuntime runtime)
   {
      this.runtime = runtime;
      return (this);
   }



   /*******************************************************************************
    ** esbView, or READ on a table publishing to the destination, or access to a
    ** process it triggers.
    *******************************************************************************/
   private void checkMessagesPermission(QEsbDestinationMetaData destination) throws QPermissionDeniedException
   {
      if(PermissionsHelper.hasAppPermission(actionInput, EsbAppMetaDataProducer.NAME))
      {
         return;
      }

      for(QTableMetaData table : CollectionUtils.nonNullMap(qInstance.getTables()).values())
      {
         EsbTableMetaData esbTableMetaData = EsbTableMetaData.of(table);
         boolean publishes = esbTableMetaData != null && CollectionUtils.nonNullList(esbTableMetaData.getPublications()).stream()
            .anyMatch(publication -> destination.getName().equals(publication.getDestinationName()));
         if(publishes && PermissionsHelper.hasTablePermission(actionInput, table.getName(), TablePermissionSubType.READ))
         {
            return;
         }
      }

      for(TriggerReference triggerReference : findTriggers())
      {
         if(triggerReference.destination() == destination && PermissionsHelper.hasProcessPermission(actionInput, triggerReference.process().getName()))
         {
            return;
         }
      }

      throw (new QPermissionDeniedException("Permission denied."));
   }



   /*******************************************************************************
    ** A page of messages: { messages, hasMore }.
    *******************************************************************************/
   private Map<String, Object> browse(String providerName, String brokerQueueName, Integer offset, Integer limit) throws QException
   {
      int effectiveOffset = offset == null ? 0 : offset;
      int effectiveLimit  = limit == null ? DEFAULT_LIMIT : limit;
      if(effectiveOffset < 0)
      {
         throw (new QBadRequestException("The offset may not be negative."));
      }

      if(effectiveLimit < 1 || effectiveLimit > MAX_LIMIT)
      {
         throw (new QBadRequestException("The limit must be from 1 to " + MAX_LIMIT + "."));
      }

      List<EsbBrowsedMessage> browsedMessages = EsbMessageBrowser.browse(providerName, brokerQueueName, effectiveOffset, effectiveLimit + 1);

      List<Map<String, Object>> messages = new ArrayList<>();
      for(EsbBrowsedMessage browsedMessage : browsedMessages.subList(0, Math.min(effectiveLimit, browsedMessages.size())))
      {
         Map<String, Object> message = new LinkedHashMap<>();
         message.put("messageId", browsedMessage.getMessageId());
         message.put("timestamp", browsedMessage.getTimestamp() == null ? null : browsedMessage.getTimestamp().toString());
         message.put("deliveryCount", browsedMessage.getDeliveryCount());
         message.put("event", browsedMessage.getEvent() == null ? null : toMap(EsbEventCodec.toJson(browsedMessage.getEvent())));
         message.put("rawBody", browsedMessage.getRawBody());
         message.put("properties", CollectionUtils.nonNullMap(browsedMessage.getProperties()));
         messages.add(message);
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("messages", messages);
      result.put("hasMore", browsedMessages.size() > effectiveLimit);
      return (result);
   }



   /*******************************************************************************
    ** A publication: { destination, events }.
    *******************************************************************************/
   private Map<String, Object> buildPublication(QEsbDestinationMetaData destination, List<String> events)
   {
      Map<String, Object> publication = new LinkedHashMap<>();
      publication.put("destination", buildDestination(destination));
      publication.put("events", events);
      return (publication);
   }



   /*******************************************************************************
    ** The tables and processes publishing to a destination: { kind, name,
    ** events } each.
    *******************************************************************************/
   private List<Map<String, Object>> buildPublishers(QEsbDestinationMetaData destination)
   {
      List<Map<String, Object>> publishers = new ArrayList<>();
      for(QTableMetaData table : CollectionUtils.nonNullMap(qInstance.getTables()).values())
      {
         EsbTableMetaData esbTableMetaData = EsbTableMetaData.of(table);
         for(EsbTablePublication publication : esbTableMetaData == null ? List.<EsbTablePublication>of() : CollectionUtils.nonNullList(esbTableMetaData.getPublications()))
         {
            if(destination.getName().equals(publication.getDestinationName()))
            {
               publishers.add(buildPublisher("TABLE", table.getName(), enumNames(publication.getEvents())));
            }
         }
      }

      for(QProcessMetaData process : CollectionUtils.nonNullMap(qInstance.getProcesses()).values())
      {
         EsbProcessMetaData esbProcessMetaData = EsbProcessMetaData.of(process);
         for(EsbProcessPublication publication : esbProcessMetaData == null ? List.<EsbProcessPublication>of() : CollectionUtils.nonNullList(esbProcessMetaData.getPublications()))
         {
            if(destination.getName().equals(publication.getDestinationName()))
            {
               publishers.add(buildPublisher("PROCESS", process.getName(), enumNames(publication.getEvents())));
            }
         }
      }

      return (publishers);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Object> buildPublisher(String kind, String name, List<String> events)
   {
      Map<String, Object> publisher = new LinkedHashMap<>();
      publisher.put("kind", kind);
      publisher.put("name", name);
      publisher.put("events", events);
      return (publisher);
   }



   /*******************************************************************************
    ** A destination: { name, type, provider, brokerName, counters, queueInfo,
    ** capabilities }.  queueInfo is only for queues (a topic's messages are on
    ** its triggers' subscriptions), and needs the provider's managementUrl.
    *******************************************************************************/
   private Map<String, Object> buildDestination(QEsbDestinationMetaData destination)
   {
      String brokerName = destination.getEffectiveDestinationName();

      Map<String, Object> queueInfo = null;
      if(destination.getType() == EsbDestinationType.QUEUE)
      {
         queueInfo = getQueueInfo(destination.getProviderName(), brokerName)
            .map(EsbStatusBuilder::buildQueueInfo)
            .orElse(null);
      }

      EsbBrokerCapabilities capabilities = getAdapter(destination.getProviderName())
         .map(EsbBrokerAdapter::capabilities)
         .orElse(EsbBrokerCapabilities.WITHOUT_MANAGEMENT);

      Map<String, Object> capabilitiesView = new LinkedHashMap<>();
      capabilitiesView.put("browse", capabilities.browse());
      capabilitiesView.put("pauseQueue", capabilities.pauseQueue());
      capabilitiesView.put("purge", capabilities.purge());
      capabilitiesView.put("deleteSelected", capabilities.deleteSelected());
      capabilitiesView.put("deleteOlderThan", capabilities.deleteOlderThan());
      capabilitiesView.put("move", capabilities.move());

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("name", destination.getName());
      result.put("type", enumName(destination.getType()));
      result.put("provider", destination.getProviderName());
      result.put("brokerName", brokerName);
      result.put("counters", buildCounters(EsbStats.getInstance().destination(destination.getName())));
      result.put("queueInfo", queueInfo);
      result.put("capabilities", capabilitiesView);
      return (result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Map<String, Object> buildQueueInfo(EsbQueueInfo queueInfo)
   {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("messageCount", queueInfo.messageCount());
      result.put("consumerCount", queueInfo.consumerCount());
      result.put("paused", queueInfo.paused());
      return (result);
   }



   /*******************************************************************************
    ** A trigger: { name, processName, processLabel, destination, mode,
    ** concurrency, maxAttempts, state (on this node), counters, deadLetter,
    ** subscription (topic triggers only; else null) }.
    *******************************************************************************/
   private Map<String, Object> buildTrigger(TriggerReference triggerReference)
   {
      QProcessMetaData        process     = triggerReference.process();
      EsbTrigger              trigger     = triggerReference.trigger();
      QEsbDestinationMetaData destination = triggerReference.destination();
      String                  triggerName = trigger.getName(process.getName());

      EsbTriggerRunner runner = runtime == null ? null : runtime.getRunner(triggerName);
      EsbTriggerState  state  = runner == null ? EsbTriggerState.STOPPED : runner.getState();

      String deadLetterQueue = trigger.getEffectiveDeadLetterDestinationName(process.getName(), destination);

      Map<String, Object> subscription = null;
      if(destination.getType() == EsbDestinationType.TOPIC)
      {
         subscription = buildBrokerQueue(destination.getProviderName(), getSubscriptionQueueName(triggerReference));
      }

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("name", triggerName);
      result.put("processName", process.getName());
      result.put("processLabel", Objects.requireNonNullElse(process.getLabel(), process.getName()));
      result.put("destination", buildDestination(destination));
      result.put("mode", enumName(trigger.getMode()));
      result.put("concurrency", trigger.getConcurrency());
      result.put("maxAttempts", trigger.getMaxAttempts());
      result.put("state", state.name());
      result.put("counters", buildCounters(EsbStats.getInstance().trigger(triggerName)));
      result.put("deadLetter", buildBrokerQueue(destination.getProviderName(), deadLetterQueue));
      result.put("subscription", subscription);
      return (result);
   }



   /*******************************************************************************
    ** A trigger's dead-letter queue or subscription queue: { brokerName,
    ** messageCount (null without broker data) }.
    *******************************************************************************/
   private Map<String, Object> buildBrokerQueue(String providerName, String brokerQueueName)
   {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("brokerName", brokerQueueName);
      result.put("messageCount", getQueueInfo(providerName, brokerQueueName).map(EsbQueueInfo::messageCount).orElse(null));
      return (result);
   }



   /*******************************************************************************
    ** Counter values for the API.  Error details can contain broker URLs and
    ** credentials, so only a generic error is exposed here.
    *******************************************************************************/
   private static Map<String, Object> buildCounters(EsbCounterSnapshot snapshot)
   {
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("published", snapshot.published());
      result.put("publishFailures", snapshot.publishFailures());
      result.put("consumed", snapshot.consumed());
      result.put("succeeded", snapshot.succeeded());
      result.put("failed", snapshot.failed());
      result.put("retried", snapshot.retried());
      result.put("deadLettered", snapshot.deadLettered());
      result.put("inFlight", snapshot.inFlight());
      result.put("lastActivity", snapshot.lastActivity() == null ? null : snapshot.lastActivity().toString());
      result.put("avgMs", snapshot.avgMs());
      result.put("maxMs", snapshot.maxMs());
      result.put("lastError", snapshot.lastError() == null ? null : "An ESB operation failed.");
      return (result);
   }



   /*******************************************************************************
    ** { canOperate, canDelete }: whether the session has esbOperate.hasAccess
    ** and esbDelete.hasAccess.
    *******************************************************************************/
   private static Map<String, Object> buildPermissions()
   {
      QSession session = QContext.getQSession();

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("canOperate", session != null && session.hasPermission(OPERATE_PERMISSION));
      result.put("canDelete", session != null && session.hasPermission(DELETE_PERMISSION));
      return (result);
   }



   /*******************************************************************************
    ** The broker-side name of a topic trigger's subscription queue.
    *******************************************************************************/
   private String getSubscriptionQueueName(TriggerReference triggerReference)
   {
      QEsbDestinationMetaData destination      = triggerReference.destination();
      QEsbProviderMetaData    provider         = esbInstanceMetaData.getProvider(destination.getProviderName());
      String                  subscriptionName = triggerReference.trigger().getEffectiveSubscriptionName(triggerReference.process().getName(), destination);
      return (EsbBrokerNames.subscriptionQueue(provider == null ? null : provider.getType(), destination.getEffectiveDestinationName(), subscriptionName));
   }



   /*******************************************************************************
    ** The provider's broker adapter (empty without a managementUrl).
    *******************************************************************************/
   private Optional<EsbBrokerAdapter> getAdapter(String providerName)
   {
      return (adapters.computeIfAbsent(providerName, EsbBrokerAdapters::forProvider));
   }



   /*******************************************************************************
    ** A queue's info from the provider's management API - empty without a
    ** managementUrl, if the queue doesn't exist, or if the API can't be reached
    ** (logged: the endpoints still answer, without broker data).
    *******************************************************************************/
   private Optional<EsbQueueInfo> getQueueInfo(String providerName, String brokerQueueName)
   {
      String key = providerName + "\u0000" + brokerQueueName;
      if(!queueInfos.containsKey(key))
      {
         Optional<EsbQueueInfo>     queueInfo = Optional.empty();
         Optional<EsbBrokerAdapter> adapter   = getAdapter(providerName);
         if(adapter.isPresent())
         {
            try
            {
               queueInfo = adapter.get().getQueueInfo(brokerQueueName);
            }
            catch(Exception e)
            {
               LOG.warn("Could not get queue info from an ESB provider's management API", e, logPair("providerName", providerName), logPair("brokerQueueName", brokerQueueName));
            }
         }
         queueInfos.put(key, queueInfo);
      }

      return (queueInfos.get(key));
   }



   /*******************************************************************************
    ** Every trigger in the instance whose destination is known (in process, then
    ** trigger, order).
    *******************************************************************************/
   private List<TriggerReference> findTriggers()
   {
      List<TriggerReference> triggerReferences = new ArrayList<>();
      if(esbInstanceMetaData == null)
      {
         return (triggerReferences);
      }

      for(QProcessMetaData process : CollectionUtils.nonNullMap(qInstance.getProcesses()).values())
      {
         EsbProcessMetaData esbProcessMetaData = EsbProcessMetaData.of(process);
         for(EsbTrigger trigger : esbProcessMetaData == null ? List.<EsbTrigger>of() : CollectionUtils.nonNullList(esbProcessMetaData.getTriggers()))
         {
            QEsbDestinationMetaData destination = esbInstanceMetaData.getDestination(trigger.getDestinationName());
            if(destination != null)
            {
               triggerReferences.add(new TriggerReference(process, trigger, destination));
            }
         }
      }

      return (triggerReferences);
   }



   /*******************************************************************************
    ** A trigger by its name (processName.destinationName) - or null.
    *******************************************************************************/
   private TriggerReference findTrigger(String triggerName)
   {
      return (findTriggers().stream()
         .filter(triggerReference -> triggerReference.trigger().getName(triggerReference.process().getName()).equals(triggerName))
         .findFirst()
         .orElse(null));
   }



   /*******************************************************************************
    ** A CloudEvent's JSON as a map (so it's serialized as the CloudEvent is).
    *******************************************************************************/
   private static Map<String, Object> toMap(String json) throws QException
   {
      try
      {
         return (JsonUtils.toObject(json, new TypeReference<LinkedHashMap<String, Object>>() {}));
      }
      catch(IOException e)
      {
         throw (new QException("Could not read an ESB event's JSON", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> enumNames(List<? extends Enum<?>> values)
   {
      return (CollectionUtils.nonNullList(values).stream().map(EsbStatusBuilder::enumName).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String enumName(Enum<?> value)
   {
      return (value == null ? null : value.name());
   }



   /*******************************************************************************
    * A trigger, with its process and (known) destination.
    *******************************************************************************/
   private record TriggerReference(QProcessMetaData process, EsbTrigger trigger, QEsbDestinationMetaData destination)
   {
   }

}
