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


package com.kingsrook.qqq.backend.core.jobs.effects.targets;


import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectKeys;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectRequest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectResult;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectType;
import com.kingsrook.qqq.backend.core.jobs.effects.ReconcileResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Storage effects use the BaseTest memory instance and never touch disk.
 *******************************************************************************/
class StorageEffectTargetTest extends BaseTest
{
   /*******************************************************************************
    ** A duplicate PUT has one object, the same reference and the latest UTF-8 content.
    *******************************************************************************/
   @Test
   void repeatedDeliveryOverwritesSameObject() throws Exception
   {
      StorageEffectTarget target = new StorageEffectTarget();
      EffectRequest       first  = request("content", "first");
      EffectRequest       second = request("content", "Updated café");
      EffectResult        result = target.deliver(first);
      assertThat(result.outcome()).isEqualTo(EffectResult.Outcome.SENT);
      assertThat(target.deliver(second)).isEqualTo(result);
      assertThat(result.providerRef()).isEqualTo(EffectKeys.objectName("exports", first.idempotencyKey(), "txt"));
      assertThat(new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_MEMORY_STORAGE)).getRecords()).hasSize(1);
      try(InputStream stream = new StorageAction().getInputStream(new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(result.providerRef())))
      {
         assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("Updated café");
      }
      assertThat(target.reconcile(second)).isEqualTo(ReconcileResult.found(result.providerRef()));
   }



   /*******************************************************************************
    ** A missing deterministic object can safely be delivered, including binary payloads.
    *******************************************************************************/
   @Test
   void reconcileMissingAndBase64Content() throws Exception
   {
      byte[]              bytes   = new byte[] { 0, 1, -1, 42 };
      StorageEffectTarget target  = new StorageEffectTarget();
      EffectRequest       request = request("contentBase64", Base64.getEncoder().encodeToString(bytes));
      assertThat(target.reconcile(request).outcome()).isEqualTo(ReconcileResult.Outcome.NOT_FOUND);
      EffectResult result = target.deliver(request);
      assertThat(result.outcome()).isEqualTo(EffectResult.Outcome.SENT);
      try(InputStream stream = new StorageAction().getInputStream(new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(result.providerRef())))
      {
         assertThat(stream.readAllBytes()).isEqualTo(bytes);
      }
   }



   /*******************************************************************************
    ** Invalid storage configuration is ambiguous, never proof that an object is absent.
    *******************************************************************************/
   @Test
   void storageErrorsAreUnknown()
   {
      EffectRequest request = new EffectRequest("key", "store", JobEffectType.IDEMPOTENT_EXTERNAL, 1, 0, "row",
         Map.of("storageTableName", "missingTable", "content", "text"), null, 1);
      assertThat(new StorageEffectTarget().deliver(request).outcome()).isEqualTo(EffectResult.Outcome.UNKNOWN);
      assertThat(new StorageEffectTarget().reconcile(request).outcome()).isEqualTo(ReconcileResult.Outcome.UNKNOWN);
   }



   /*******************************************************************************
    ** Both attempts use the same stable job/row identity.
    *******************************************************************************/
   private EffectRequest request(String contentKey, String content)
   {
      Map<String, Serializable> payload = Map.of("storageTableName", TestUtils.TABLE_NAME_MEMORY_STORAGE,
         "prefix", "exports", "extension", "txt", contentKey, content);
      return (new EffectRequest("1:0:row:store", "store", JobEffectType.IDEMPOTENT_EXTERNAL, 1, 0, "row", payload, null, 1));
   }
}
