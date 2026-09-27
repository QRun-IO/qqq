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

package com.kingsrook.qqq.esb.runtime;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Pause, resume, or restart a trigger's consumers on every node (spec sections
 * 6 and 8).  Each call sends { "action", "triggerName" } to the non-durable
 * control topic (qqq.esb.control) on the trigger's provider; every running
 * QEsbRuntime listening there - this node's included - applies it to its own
 * runner (see EsbControlChannel).  The call returns once the message is sent,
 * not once the nodes have applied it.
 *
 * The trigger is looked up in the QContext's instance, by its name
 * (processName.destinationName); an unknown trigger is a QException, as is a
 * message that can't be sent (e.g., with the broker down).
 *
 * Pause state isn't persisted: a node that starts (or restarts) later uses the
 * trigger's startPaused.
 *******************************************************************************/
public class EsbTriggerControl
{
   private static final QLogger LOG = QLogger.getLogger(EsbTriggerControl.class);



   /*******************************************************************************
    ** Constructor - not used; everything is static.
    *******************************************************************************/
   private EsbTriggerControl()
   {
   }



   /*******************************************************************************
    ** Pause the trigger on every node: consumers close, and messages wait on the
    ** broker.
    *******************************************************************************/
   public static void pause(String triggerName) throws QException
   {
      send(EsbControlChannel.Action.PAUSE, triggerName);
   }



   /*******************************************************************************
    ** Resume the trigger on every node.
    *******************************************************************************/
   public static void resume(String triggerName) throws QException
   {
      send(EsbControlChannel.Action.RESUME, triggerName);
   }



   /*******************************************************************************
    ** Restart the trigger's consumers on every node: each builds a new session
    ** and consumer (a paused trigger stays paused).
    *******************************************************************************/
   public static void restart(String triggerName) throws QException
   {
      send(EsbControlChannel.Action.RESTART, triggerName);
   }



   /*******************************************************************************
    ** Send the action to the control topic on the trigger's provider.
    *******************************************************************************/
   private static void send(EsbControlChannel.Action action, String triggerName) throws QException
   {
      String providerName = getProviderName(triggerName);
      EsbControlChannel.send(providerName, action, triggerName);
      LOG.info("Sent an ESB control message", logPair("action", action), logPair("triggerName", triggerName), logPair("providerName", providerName));
   }



   /*******************************************************************************
    ** The provider of the named trigger's destination, in the QContext's
    ** instance - or a QException if there's no such trigger.
    *******************************************************************************/
   private static String getProviderName(String triggerName) throws QException
   {
      QInstance           qInstance           = QContext.getQInstance();
      EsbInstanceMetaData esbInstanceMetaData = qInstance == null ? null : QSupplementalInstanceMetaData.of(qInstance, EsbInstanceMetaData.NAME);
      if(triggerName != null && esbInstanceMetaData != null)
      {
         for(QProcessMetaData process : CollectionUtils.nonNullMap(qInstance.getProcesses()).values())
         {
            EsbProcessMetaData esbProcessMetaData = EsbProcessMetaData.of(process);
            if(esbProcessMetaData == null)
            {
               continue;
            }

            for(EsbTrigger trigger : CollectionUtils.nonNullList(esbProcessMetaData.getTriggers()))
            {
               QEsbDestinationMetaData destination = esbInstanceMetaData.getDestination(trigger.getDestinationName());
               if(triggerName.equals(trigger.getName(process.getName())) && destination != null)
               {
                  return (destination.getProviderName());
               }
            }
         }
      }

      throw (new QException("Unknown ESB trigger: " + triggerName));
   }

}
