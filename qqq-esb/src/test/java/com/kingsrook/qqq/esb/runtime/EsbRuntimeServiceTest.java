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

package com.kingsrook.qqq.esb.runtime;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.instances.QRuntimeServiceInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbTrigger;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbRuntimeService - the runtime service (registered by
 ** EsbInstanceMetaData.enrich) through which an application launcher starts
 ** and stops the node's QEsbRuntime - and for QEsbRuntime.start's need for a
 ** validated instance.
 *******************************************************************************/
class EsbRuntimeServiceTest extends EsbRuntimeTestBase
{

   /*******************************************************************************
    ** Enrichment registers the service once, however many times it runs.
    *******************************************************************************/
   @Test
   void enrichRegistersTheRuntimeServiceOnce()
   {
      QInstance qInstance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      assertThat(getRuntimeServiceReferences(qInstance)).hasSize(1);

      EsbInstanceMetaData.of(qInstance).enrich(qInstance);
      assertThat(getRuntimeServiceReferences(qInstance)).hasSize(1);
   }



   /*******************************************************************************
    ** The registered service, loaded as a launcher loads it, starts and stops the
    ** node's runtime (QEsbRuntime.getInstance).
    *******************************************************************************/
   @Test
   void registeredServiceStartsAndStopsTheNodesRuntime() throws Exception
   {
      QInstance                qInstance = defineInstanceWithTrigger(new EsbTrigger().withDestinationName(QUEUE_NAME));
      QRuntimeServiceInterface service   = QCodeLoader.getAdHoc(QRuntimeServiceInterface.class, getRuntimeServiceReferences(qInstance).get(0));
      assertThat(service).isInstanceOf(EsbRuntimeService.class);
      assertThat(service.getName()).isEqualTo(EsbRuntimeService.NAME);

      try
      {
         service.start(qInstance);
         waitFor("runtime service control listener", QEsbRuntime.getInstance()::isRunning);
         waitForState(QEsbRuntime.getInstance(), QUEUE_TRIGGER_NAME, EsbTriggerState.RUNNING);
      }
      finally
      {
         service.stop();
      }

      assertThat(QEsbRuntime.getInstance().isRunning()).isFalse();
      assertThat(QEsbRuntime.getInstance().getRunner(QUEUE_TRIGGER_NAME).getState()).isEqualTo(EsbTriggerState.STOPPED);
   }



   /*******************************************************************************
    ** start needs a validated instance (its threads would otherwise each
    ** validate it, at once): an unvalidated or null instance is refused, and the
    ** runtime doesn't start.
    *******************************************************************************/
   @Test
   void startNeedsAValidatedInstance()
   {
      QEsbRuntime runtime = new QEsbRuntime();

      assertThatThrownBy(() -> runtime.start(defineInstanceWithDestinations(PROVIDER_NAME))).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("validated");
      assertThatThrownBy(() -> runtime.start(null)).isInstanceOf(IllegalArgumentException.class);
      assertThat(runtime.isRunning()).isFalse();
      assertThat(runtime.getRunners()).isEmpty();
   }



   /*******************************************************************************
    ** The instance's runtime-service references to EsbRuntimeService.
    *******************************************************************************/
   private static List<QCodeReference> getRuntimeServiceReferences(QInstance qInstance)
   {
      return (CollectionUtils.nonNullList(qInstance.getRuntimeServices()).stream()
         .filter(codeReference -> EsbRuntimeService.class.getName().equals(codeReference.getName()))
         .toList());
   }

}
