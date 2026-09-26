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

package com.kingsrook.qqq.backend.core.actions.tables.listeners;


import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Runs the instance's record change listeners.  Every listener failure (in
 ** loading it, in appliesTo, or in onRecordsChanged) is caught and logged, so
 ** a listener can never fail the write that it is hearing about.  That includes
 ** a LinkageError (e.g., a NoClassDefFoundError when a listener uses an
 ** optional library, such as a message broker's client, that isn't on the
 ** classpath), as well as any Exception.
 **
 ** Each applying listener is given the same event object, in registration
 ** order.
 *******************************************************************************/
public class RecordChangeListenerHelper
{
   private static final QLogger LOG = QLogger.getLogger(RecordChangeListenerHelper.class);



   /*******************************************************************************
    ** Whether any of the instance's listeners applies to this table and change
    ** type.  Actions check this before fetching old records or building an event,
    ** so an instance without applying listeners pays for neither.
    *******************************************************************************/
   public static boolean anyApply(QInstance qInstance, String tableName, RecordChangeType type)
   {
      if(qInstance == null || CollectionUtils.nullSafeIsEmpty(qInstance.getRecordChangeListeners()))
      {
         return (false);
      }

      for(QCodeReference codeReference : qInstance.getRecordChangeListeners())
      {
         if(loadIfApplies(codeReference, tableName, type) != null)
         {
            return (true);
         }
      }

      return (false);
   }



   /*******************************************************************************
    ** Give the event to each listener that applies to its table and type, in
    ** registration order.  Does nothing if the event has no records.
    *******************************************************************************/
   public static void fire(QInstance qInstance, RecordChangeEvent event)
   {
      if(qInstance == null || event == null || CollectionUtils.nullSafeIsEmpty(event.getRecords()) || CollectionUtils.nullSafeIsEmpty(qInstance.getRecordChangeListeners()))
      {
         return;
      }

      for(QCodeReference codeReference : qInstance.getRecordChangeListeners())
      {
         RecordChangeListenerInterface listener = loadIfApplies(codeReference, event.getTableName(), event.getType());
         if(listener == null)
         {
            continue;
         }

         try
         {
            listener.onRecordsChanged(event);
         }
         catch(Exception | LinkageError e)
         {
            LOG.warn("Error in record change listener", e, logPair("listener", codeReference.getName()), logPair("tableName", event.getTableName()), logPair("type", event.getType()), logPair("recordCount", event.getRecords().size()));
         }
      }
   }



   /*******************************************************************************
    ** The listener for a code reference, if it loads and applies; else null.
    *******************************************************************************/
   private static RecordChangeListenerInterface loadIfApplies(QCodeReference codeReference, String tableName, RecordChangeType type)
   {
      try
      {
         RecordChangeListenerInterface listener = QCodeLoader.getAdHoc(RecordChangeListenerInterface.class, codeReference);
         if(listener != null && listener.appliesTo(tableName, type))
         {
            return (listener);
         }
      }
      catch(Exception | LinkageError e)
      {
         LOG.warn("Error loading record change listener", e, logPair("listener", codeReference == null ? null : codeReference.getName()), logPair("tableName", tableName), logPair("type", type));
      }

      return (null);
   }

}
