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


package com.kingsrook.qqq.backend.core.jobs;


import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Regression coverage for DurableJobsMetaDataValidation.
 *******************************************************************************/
class DurableJobsMetaDataValidationTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** Enabling durable jobs without their tables names jobs_job in validation errors.
    *******************************************************************************/
   @Test
   void enabledWithoutTablesFailsValidation() throws Exception
   {
      QInstance missingTables = TestUtils.defineInstance().withSupplementalMetaData(new DurableJobsMetaData());
      assertThatThrownBy(() -> new QInstanceValidator().validate(missingTables))
         .isInstanceOf(QInstanceValidationException.class).hasMessageContaining("jobs_job");
   }



   /*******************************************************************************
    ** A configured storage table must exist.
    *******************************************************************************/
   @Test
   void missingStorageTableFailsValidation() throws Exception
   {
      settings.withStorageTableName("missingJobStorage");
      instance.setHasBeenValidated(null);
      assertThatThrownBy(() -> new QInstanceValidator().validate(instance))
         .isInstanceOf(QInstanceValidationException.class).hasMessageContaining("missingJobStorage");
   }



   /*******************************************************************************
    ** An always-durable process cannot explicitly disable eligibility.
    *******************************************************************************/
   @Test
   void alwaysDurableCannotBeIneligible() throws Exception
   {
      DurableProcessMetaData.of(instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS)).withAlwaysDurable(true).withEligible(false);
      instance.setHasBeenValidated(null);
      assertThatThrownBy(() -> new QInstanceValidator().validate(instance))
         .isInstanceOf(QInstanceValidationException.class).hasMessageContaining("alwaysDurable").hasMessageContaining("not eligible");
   }



   /*******************************************************************************
    ** Parallel chunks require the process to declare chunkParallelSafe.
    *******************************************************************************/
   @Test
   void parallelChunksRequireSafetyDeclaration() throws Exception
   {
      DurableProcessMetaData.of(instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS)).withMaxParallelChunks(2);
      instance.setHasBeenValidated(null);
      assertThatThrownBy(() -> new QInstanceValidator().validate(instance))
         .isInstanceOf(QInstanceValidationException.class).hasMessageContaining("chunkParallelSafe");
   }



   /*******************************************************************************
    ** The complete memory fixture passes instance validation.
    *******************************************************************************/
   @Test
   void validSetupPassesValidation() throws Exception
   {
      instance.setHasBeenValidated(null);
      assertThatCode(() -> new QInstanceValidator().validate(instance)).doesNotThrowAnyException();
   }



   /*******************************************************************************
    ** Explicit eligible metadata permits durable execution.
    *******************************************************************************/
   @Test
   void explicitEligibility() throws Exception
   {
      assertThat(DurableJobs.isEligible(instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS))).isTrue();
   }



   /*******************************************************************************
    ** The instance can opt a plain process into durable execution by name.
    *******************************************************************************/
   @Test
   void namedEligibility() throws Exception
   {
      QProcessMetaData process = new QProcessMetaData().withName("named");
      instance.addProcess(process);
      settings.withEligibleProcessName("named");
      assertThat(DurableJobs.isEligible(process)).isTrue();
   }



   /*******************************************************************************
    ** The core person bulk-insert process is eligible without an explicit override.
    *******************************************************************************/
   @Test
   void bulkInsertIsEligibleByDefault() throws Exception
   {
      new QInstanceEnricher(instance).enrich();
      QProcessMetaData process = instance.getProcess(TestUtils.TABLE_NAME_PERSON_MEMORY + ".bulkInsert");
      assertThat(process).isNotNull();
      assertThat(DurableJobs.isEligible(process)).isTrue();
   }



   /*******************************************************************************
    ** A plain process is ineligible unless explicitly enabled.
    *******************************************************************************/
   @Test
   void plainProcessIsNotEligible() throws Exception
   {
      QProcessMetaData process = new QProcessMetaData().withName("plain");
      instance.addProcess(process);
      assertThat(DurableJobs.isEligible(process)).isFalse();
   }



   /*******************************************************************************
    ** Launching an ineligible process returns a user-facing error.
    *******************************************************************************/
   @Test
   void ineligibleLaunchIsRejected() throws Exception
   {
      instance.addProcess(new QProcessMetaData().withName("plain"));
      assertThatThrownBy(() -> DurableJobs.launch(new LaunchRequest("plain"))).isInstanceOf(QUserFacingException.class);
   }



   /*******************************************************************************
    ** Launching an unknown process returns not found.
    *******************************************************************************/
   @Test
   void unknownLaunchIsNotFound() throws Exception
   {
      assertThatThrownBy(() -> DurableJobs.launch(new LaunchRequest("unknown"))).isInstanceOf(QNotFoundException.class);
   }
}

