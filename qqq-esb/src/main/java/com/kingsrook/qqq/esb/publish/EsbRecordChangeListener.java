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

package com.kingsrook.qqq.esb.publish;


import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.envelope.EsbEvent;
import com.kingsrook.qqq.esb.envelope.EsbEventFactory;
import com.kingsrook.qqq.esb.model.EsbTableEvent;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.esb.model.EsbTablePublication;


/*******************************************************************************
 * Publishes table record change events (spec sections 4 and 5) to the
 * destinations of the table's ESB publications (EsbTableMetaData) that list the
 * change's event.  EsbInstanceMetaData.enrich registers it on the instance.
 *
 * The events - one per record - are made when the action calls the listener,
 * from copies of the records' values (the records belong to the action), and
 * the same events go to each of those destinations.
 *
 * If the write ran in the caller's transaction, the events are sent from an
 * after-commit callback on it: after the commit, and never if it rolls back.
 * Otherwise (the write is already stored) they are sent right away.  Sending
 * never fails the write: EsbPublisher logs and counts failures.
 *
 * Holds no state (actions load a new instance for each event).
 *******************************************************************************/
public class EsbRecordChangeListener implements RecordChangeListenerInterface
{

   /*******************************************************************************
    ** Whether the table (in the QContext's instance) has a publication for this
    ** change - so actions don't fetch old records for changes nobody publishes.
    *******************************************************************************/
   @Override
   public boolean appliesTo(String tableName, RecordChangeType type)
   {
      return (!getDestinationNames(tableName, type).isEmpty());
   }



   /*******************************************************************************
    ** Make the events for the written records, and publish them to each
    ** destination - after the caller's transaction commits, if there is one.
    *******************************************************************************/
   @Override
   public void onRecordsChanged(RecordChangeEvent event)
   {
      Set<String> destinationNames = getDestinationNames(event.getTableName(), event.getType());
      if(destinationNames.isEmpty())
      {
         return;
      }

      List<EsbEvent> esbEvents = makeEvents(event);
      Runnable       publish   = () -> publish(destinationNames, esbEvents);

      if(event.getTransaction() != null)
      {
         event.getTransaction().addAfterCommitCallback(publish);
      }
      else
      {
         publish.run();
      }
   }



   /*******************************************************************************
    ** The destinations (in publication order, each once) of the table's
    ** publications that list this change's event.  Empty if there is no
    ** QInstance in context, no such table, or no ESB meta-data on it.
    *******************************************************************************/
   private static Set<String> getDestinationNames(String tableName, RecordChangeType type)
   {
      QInstance        qInstance        = QContext.getQInstance();
      QTableMetaData   table            = (qInstance == null || tableName == null) ? null : qInstance.getTable(tableName);
      EsbTableMetaData esbTableMetaData = (table == null) ? null : EsbTableMetaData.of(table);
      if(esbTableMetaData == null || type == null)
      {
         return (Set.of());
      }

      //////////////////////////////////////////////////////////////////
      // EsbTableEvent values are named the same as RecordChangeTypes //
      //////////////////////////////////////////////////////////////////
      EsbTableEvent tableEvent       = EsbTableEvent.valueOf(type.name());
      Set<String>   destinationNames = new LinkedHashSet<>();
      for(EsbTablePublication publication : CollectionUtils.nonNullList(esbTableMetaData.getPublications()))
      {
         if(CollectionUtils.nonNullList(publication.getEvents()).contains(tableEvent))
         {
            destinationNames.add(publication.getDestinationName());
         }
      }

      return (destinationNames);
   }



   /*******************************************************************************
    ** One event per written record, paired with its old record (index-aligned,
    ** for updates and deletes).
    *******************************************************************************/
   private static List<EsbEvent> makeEvents(RecordChangeEvent event)
   {
      String        instanceName = EsbPublisher.getInstanceNameFromContext();
      List<QRecord> records      = CollectionUtils.nonNullList(event.getRecords());
      List<QRecord> oldRecords   = event.getOldRecords();

      List<EsbEvent> esbEvents = new ArrayList<>(records.size());
      for(int i = 0; i < records.size(); i++)
      {
         QRecord oldRecord = (oldRecords != null && i < oldRecords.size()) ? oldRecords.get(i) : null;
         esbEvents.add(EsbEventFactory.forRecordChange(instanceName, event.getTableName(), event.getType(), records.get(i), oldRecord));
      }

      return (esbEvents);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void publish(Set<String> destinationNames, List<EsbEvent> esbEvents)
   {
      for(String destinationName : destinationNames)
      {
         EsbPublisher.getInstance().publish(destinationName, esbEvents);
      }
   }

}
