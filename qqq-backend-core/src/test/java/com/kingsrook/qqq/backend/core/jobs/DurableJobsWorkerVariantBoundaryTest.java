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


import java.util.Base64;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsUtil;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** A worker restores a job's owner from the job record, not the launching request,
 ** so the launching request's backend variant (which database of a multi-variant
 ** backend a table lives in) is not carried to the worker. That must fail closed:
 ** the worker session has no variant, and asking a variant backend for one is an
 ** error - it can never resolve to another variant or database. (A host that wants
 ** variant-aware jobs supplies the variant through its own automated-session
 ** customizer.) Nothing secret is in the job record: only the user id and scope.
 *******************************************************************************/
class DurableJobsWorkerVariantBoundaryTest extends DurableJobsTestBase
{
   private static final String KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());



   /*******************************************************************************
    ** A backend whose tables live per tenant variant.
    *******************************************************************************/
   private QBackendMetaData variantBackend()
   {
      return (new QBackendMetaData().withName("variantBackend").withBackendType(MemoryBackendModule.class).withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig().withVariantTypeKey("tenant").withOptionsTableName("tenants")));
   }



   /*******************************************************************************
    ** The launching request resolves its variant; the restored worker session has
    ** none and the lookup fails instead of choosing a database.
    *******************************************************************************/
   private void assertWorkerHasNoVariant(Integer jobId) throws Exception
   {
      QBackendMetaData backend = variantBackend();
      assertThat(BackendVariantsUtil.getVariantId(backend)).isEqualTo("A");

      QContext.init(instance, new QSystemUserSession(instance));
      QSession worker = JobSessions.forJob(instance, store.getJob(jobId));
      assertThat(worker.getBackendVariants() == null ? java.util.Map.of() : worker.getBackendVariants()).isEmpty();
      assertThat(store.getJob(jobId).getValueString("credentialScopeJson")).doesNotContain("\"A\"").doesNotContain("tenant");

      QContext.withTemporaryContext(new CapturedContext(instance, worker), () ->
         assertThatThrownBy(() -> BackendVariantsUtil.getVariantId(backend)).isInstanceOf(QUserFacingException.class).hasMessageContaining("Could not find Backend Variant information"));
   }



   /*******************************************************************************
    ** Without a grant (the module's automated session for the owner).
    *******************************************************************************/
   @Test
   void automatedWorkerSessionHasNoVariantAndFailsClosed() throws Exception
   {
      QContext.getQSession().setBackendVariants(new java.util.HashMap<>(java.util.Map.of("tenant", "A")));
      Integer jobId = launch();
      assertWorkerHasNoVariant(jobId);
   }



   /*******************************************************************************
    ** With a signed grant (the owner's own session rebuilt from a live sign-in).
    *******************************************************************************/
   @Test
   void grantedWorkerSessionHasNoVariantAndFailsClosed() throws Exception
   {
      settings.withJobGrantKey(KEY);
      instance.getAuthentication().withModuleCodeReference(new QCodeReference(JobGrantsTest.SignInAuthentication.class));
      JobGrantsTest.SIGNED_IN.put(QContext.getQSession().getUuid(), QContext.getQSession().getUser().getIdReference());
      QContext.getQSession().setBackendVariants(new java.util.HashMap<>(java.util.Map.of("tenant", "A")));
      try
      {
         Integer jobId = launch();
         assertThat(store.getJob(jobId).getValueString("credentialScopeJson")).contains(JobGrants.SCOPE_GRANT);
         assertWorkerHasNoVariant(jobId);
      }
      finally
      {
         JobGrantsTest.SIGNED_IN.clear();
      }
   }
}
