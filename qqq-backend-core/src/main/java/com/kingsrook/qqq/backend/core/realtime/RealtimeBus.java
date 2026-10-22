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

package com.kingsrook.qqq.backend.core.realtime;


import java.util.Map;
import java.util.WeakHashMap;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;


/*******************************************************************************
 ** Public qbit entry point: RealtimeBus.get(instance).publish(message). One bus per instance.
 *******************************************************************************/
public final class RealtimeBus
{
   private static final Map<QInstance, RealtimeBusInterface> BUSES = new WeakHashMap<>();



   /*******************************************************************************
    ** Load a configured implementation once; otherwise use the local bus.
    *******************************************************************************/
   public static synchronized RealtimeBusInterface get(QInstance instance)
   {
      return (BUSES.computeIfAbsent(instance, key ->
      {
         QCodeReference reference = RealtimeMetaData.of(instance).getBus();
         try
         {
            RealtimeBusInterface[] result = new RealtimeBusInterface[1];
            QContext.withTemporaryContext(new CapturedContext(instance, null), () ->
               result[0] = reference == null ? new InMemoryRealtimeBus() : QCodeLoader.getAdHoc(RealtimeBusInterface.class, reference));
            return (result[0]);
         }
         catch(Exception e)
         {
            throw (new QRuntimeException("Could not load realtime bus", e));
         }
      }));
   }



   /*******************************************************************************
    ** Release the instance bus at application shutdown.
    *******************************************************************************/
   public static synchronized void close(QInstance instance)
   {
      RealtimeBusInterface bus = BUSES.remove(instance);
      if(bus instanceof AutoCloseable closeable)
      {
         try
         {
            closeable.close();
         }
         catch(Exception e)
         {
            QLogger.getLogger(RealtimeBus.class).warn("Could not close realtime bus", e);
         }
      }
   }
}
