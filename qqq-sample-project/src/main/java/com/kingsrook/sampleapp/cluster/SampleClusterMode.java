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

package com.kingsrook.sampleapp.cluster;


import com.kingsrook.qqq.backend.core.cluster.ClusterBusMetaData;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.state.StateProviderMetaData;
import com.kingsrook.qqq.backend.core.state.TableStateProvider;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.esb.cluster.EsbClusterBus;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.kingsrook.sampleapp.processes.cluster.SampleClusterProcesses;


/*******************************************************************************
 ** Opt-in multi-node ("cluster") mode for the sample, for the HA acceptance
 ** test (SampleMultiNodeIT) and for trying HA locally. Off by default: the
 ** single-node sample is unchanged.
 **
 ** Turn it on with SAMPLE_CLUSTER_MODE=true (or -Dqqq.sample.cluster.mode=true).
 ** It then adds:
 ** - the qqq_state table and StateProviderMetaData (shared process and async
 **   state; QQQ_STATE_ENCRYPTION_KEY is required, as in production);
 ** - ClusterBusMetaData with EsbClusterBus (cache invalidation over the sample's
 **   Artemis broker);
 ** - /health and /ready probes;
 ** - the demo processes in SampleClusterProcesses.
 **
 ** Nodes share one database through SAMPLE_JDBC_URL (qqq.sample.jdbcUrl), for
 ** example an H2 TCP server: jdbc:h2:tcp://127.0.0.1:9092/mem:qqq;MODE=MySQL;DB_CLOSE_DELAY=-1.
 ** Only one node primes it (qqq.sample.cluster.primeDatabase, default true).
 ** A node that should use another node's broker sets -Dqqq.sample.esb.embedded=false
 ** and -Dqqq.sample.esb.port.
 *******************************************************************************/
public final class SampleClusterMode
{
   public static final String STATE_TABLE_NAME = StateProviderMetaData.DEFAULT_TABLE_NAME;



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private SampleClusterMode()
   {
   }



   /*******************************************************************************
    ** Whether cluster mode is on (SAMPLE_CLUSTER_MODE / qqq.sample.cluster.mode).
    *******************************************************************************/
   public static boolean isEnabled()
   {
      return (new QMetaDataVariableInterpreter().getBooleanFromPropertyOrEnvironment("qqq.sample.cluster.mode", "SAMPLE_CLUSTER_MODE", false));
   }



   /*******************************************************************************
    ** The shared database url, in cluster mode only (null otherwise).
    *******************************************************************************/
   public static String getSharedJdbcUrl()
   {
      if(!isEnabled())
      {
         return (null);
      }
      String url = new QMetaDataVariableInterpreter().getStringFromPropertyOrEnvironment("qqq.sample.jdbcUrl", "SAMPLE_JDBC_URL", null);
      return (url == null || url.isBlank() ? null : url.trim());
   }



   /*******************************************************************************
    ** Whether this node primes the shared database at startup.
    *******************************************************************************/
   public static boolean shouldPrimeDatabase()
   {
      return (!isEnabled() || new QMetaDataVariableInterpreter().getBooleanFromPropertyOrEnvironment("qqq.sample.cluster.primeDatabase", "SAMPLE_CLUSTER_PRIME_DATABASE", true));
   }



   /*******************************************************************************
    ** Add the cluster configuration to the instance.
    *******************************************************************************/
   public static void apply(QInstance instance) throws QException
   {
      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();

      //////////////////////////////////////////////////////////////////////
      // shared process / async state, in the qqq_state table (DDL in      //
      // prime-cluster-database.sql); the key comes from the environment  //
      //////////////////////////////////////////////////////////////////////
      instance.addTable(TableStateProvider.defineTable(STATE_TABLE_NAME, SampleMetaDataProvider.RDBMS_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName(STATE_TABLE_NAME)));
      instance.withSupplementalMetaData(new StateProviderMetaData()
         .withTableName(STATE_TABLE_NAME)
         .withHeartbeatSeconds(interpreter.getIntegerFromPropertyOrEnvironment("qqq.sample.cluster.heartbeatSeconds", "SAMPLE_CLUSTER_HEARTBEAT_SECONDS", 10))
         .withLostAfterSeconds(interpreter.getIntegerFromPropertyOrEnvironment("qqq.sample.cluster.lostAfterSeconds", "SAMPLE_CLUSTER_LOST_AFTER_SECONDS", 60)));

      ///////////////////////////////////////////////////////////////
      // cache invalidation and session ends reach every node over //
      // the ESB topic qqq.cluster.control                         //
      ///////////////////////////////////////////////////////////////
      instance.withSupplementalMetaData(new ClusterBusMetaData().withBus(new QCodeReference(EsbClusterBus.class)));

      ////////////////////////////////////////////////////////////
      // liveness and readiness probes (/ready is 503 draining) //
      ////////////////////////////////////////////////////////////
      new SampleClusterHealthProducer().produce(instance);

      SampleClusterProcesses.defineAll(instance);
   }
}
