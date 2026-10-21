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

package com.kingsrook.qqq.esb.api;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanel;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelBlock;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelContext;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelKeyValue;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelLink;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelProviderInterface;
import com.kingsrook.qqq.backend.core.model.developer.DeveloperPanelTone;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.envelope.EsbEventCodec;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbTableEvent;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Developer panel for a record of a table that publishes change events to the
 ** ESB: the CloudEvents type, source and subject of its events, where they are
 ** published, the triggers (processes) subscribed to them with their dead
 ** letter counts, and a sample event for this record.  Available when the ESB
 ** is configured and the table has ESB publications; the subscribers are only
 ** those on processes the user may run (as on GET /qqq/v1/esb/table/{table}).
 *******************************************************************************/
public class EsbDeveloperPanelProvider implements DeveloperPanelProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(EsbDeveloperPanelProvider.class);

   public static final String ID = "esb";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getId()
   {
      return (ID);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getTitle()
   {
      return ("ESB Events");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getOrder()
   {
      return (300);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getIcon()
   {
      return ("hub");
   }



   /*******************************************************************************
    ** Available when the ESB is configured and the table publishes to it.
    *******************************************************************************/
   @Override
   public boolean isAvailable(DeveloperPanelContext context)
   {
      QInstance qInstance = QContext.getQInstance();
      return (context.getTable() != null
         && QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME) != null
         && EsbTableMetaData.of(context.getTable()) != null
         && CollectionUtils.nullSafeHasContents(EsbTableMetaData.of(context.getTable()).getPublications()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   @SuppressWarnings("unchecked")
   public DeveloperPanel build(DeveloperPanelContext context) throws QException
   {
      String              tableName    = context.getTableName();
      EsbInstanceMetaData esbInstance  = QSupplementalInstanceMetaData.of(QContext.getQInstance(), EsbInstanceMetaData.NAME);
      Map<String, Object> status       = new EsbStatusBuilder().buildTable(tableName);
      Set<String>         publishedEvents = new LinkedHashSet<>();

      DeveloperPanelBlock publications = DeveloperPanelBlock.table("Published to", List.of("Destination", "Type", "Provider", "Events", "Queued"));
      for(Map<String, Object> publication : (List<Map<String, Object>>) status.getOrDefault("publications", List.of()))
      {
         Map<String, Object> destination = asMap(publication.get("destination"));
         List<String>        events      = ((Collection<?>) publication.getOrDefault("events", List.of())).stream().map(String::valueOf).toList();
         publishedEvents.addAll(events);
         publications.withRow(List.of(
            text(destination.get("name")),
            text(destination.get("type")),
            text(destination.get("provider")),
            String.join(", ", events),
            text(asMap(destination.get("queueInfo")).get("messageCount"))));
      }

      String              source  = "qqq://" + esbInstance.getInstanceName() + "/table/" + tableName;
      DeveloperPanelBlock summary = DeveloperPanelBlock.keyValues("CloudEvents attributes");
      summary.withItem(new DeveloperPanelKeyValue().withLabel("type").withValue(String.join(", ", publishedEvents.stream().map(event -> eventType(tableName, event)).toList())).withMonospace(true));
      summary.withItem(new DeveloperPanelKeyValue().withLabel("source").withValue(source).withMonospace(true));
      summary.withItem(new DeveloperPanelKeyValue().withLabel("subject").withValue(text(context.getPrimaryKey())).withMonospace(true));
      summary.withItem(new DeveloperPanelKeyValue().withLabel("datacontenttype").withValue(EsbEventCodec.DATA_CONTENT_TYPE).withMonospace(true));

      List<Map<String, Object>> subscriberList = (List<Map<String, Object>>) status.getOrDefault("subscribers", List.of());
      DeveloperPanelBlock       subscribers    = DeveloperPanelBlock.table("Subscribers (triggers)", List.of("Trigger", "Process", "Mode", "State", "Max attempts", "Dead letters"));
      for(Map<String, Object> subscriber : subscriberList)
      {
         subscribers.withRow(List.of(
            text(subscriber.get("name")),
            text(Objects.requireNonNullElse(subscriber.get("processLabel"), subscriber.get("processName"))),
            text(subscriber.get("mode")),
            text(subscriber.get("state")),
            text(subscriber.get("maxAttempts")),
            text(asMap(subscriber.get("deadLetter")).get("messageCount"))));
      }

      long deadLetters = subscriberList.stream().map(subscriber -> asMap(subscriber.get("deadLetter")).get("messageCount"))
         .filter(Number.class::isInstance).mapToLong(count -> ((Number) count).longValue()).sum();

      DeveloperPanel panel = new DeveloperPanel()
         .withDescription("This table publishes its record changes to the ESB as CloudEvents.")
         .withStatusLabel(deadLetters > 0 ? deadLetters + " dead letters" : "Publishing")
         .withStatusTone(deadLetters > 0 ? DeveloperPanelTone.WARNING : DeveloperPanelTone.SUCCESS)
         .withBlock(summary)
         .withBlock(publications);

      if(subscriberList.isEmpty())
      {
         panel.withBlock(DeveloperPanelBlock.text("No triggers that you can see subscribe to these destinations.", DeveloperPanelTone.INFO));
      }
      else
      {
         panel.withBlock(subscribers);
      }

      String sample = sampleEvent(esbInstance, tableName, publishedEvents, context);
      if(sample != null)
      {
         panel.withBlock(DeveloperPanelBlock.code("Sample event for this record (id and time are generated per event)", "json", sample));
      }

      return (panel.withBlock(DeveloperPanelBlock.links("ESB tools")
         .withLink(new DeveloperPanelLink().withLabel("Table developer view (messages, dead letters, operate)").withHref("/app/" + tableName + "/dev"))
         .withLink(new DeveloperPanelLink().withLabel("ESB overview").withHref("/app/esb"))));
   }



   /*******************************************************************************
    ** Build a sample CloudEvent for the record: an update if the table publishes
    ** updates, else its first published event.  Null (logged) if it can't be built.
    *******************************************************************************/
   private String sampleEvent(EsbInstanceMetaData esbInstance, String tableName, Set<String> publishedEvents, DeveloperPanelContext context)
   {
      try
      {
         String           event      = publishedEvents.contains(EsbTableEvent.UPDATE.name()) ? EsbTableEvent.UPDATE.name() : publishedEvents.stream().findFirst().orElse(EsbTableEvent.UPDATE.name());
         RecordChangeType changeType = RecordChangeType.valueOf(event);
         return (EsbEventCodec.toJson(EsbEventFactory.forRecordChange(esbInstance.getInstanceName(), tableName, changeType, context.getRecord(), changeType == RecordChangeType.INSERT ? null : context.getRecord())));
      }
      catch(Exception e)
      {
         LOG.info("Could not build a sample ESB event for a developer panel", e, logPair("table", tableName));
         return (null);
      }
   }



   /*******************************************************************************
    ** The CloudEvents type of a table event, e.g., qqq.table.person.updated.
    *******************************************************************************/
   static String eventType(String tableName, String event)
   {
      String suffix = switch(event)
      {
         case "INSERT" -> "inserted";
         case "DELETE" -> "deleted";
         default -> "updated";
      };
      return ("qqq.table." + tableName + "." + suffix);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static Map<String, Object> asMap(Object value)
   {
      return (value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String text(Object value)
   {
      return (value == null ? "" : String.valueOf(value));
   }

}
