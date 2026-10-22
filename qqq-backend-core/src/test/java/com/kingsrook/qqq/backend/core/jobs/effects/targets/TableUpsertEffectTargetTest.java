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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectRequest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectResult;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectType;
import com.kingsrook.qqq.backend.core.jobs.effects.ReconcileResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Natural-key upserts into the separate person memory table.
 *******************************************************************************/
class TableUpsertEffectTargetTest extends BaseTest
{
   /*******************************************************************************
    ** Re-delivery updates values while preserving the row and primary key.
    *******************************************************************************/
   @Test
   void repeatedDeliveryUpdatesOneRow() throws Exception
   {
      TableUpsertEffectTarget target  = new TableUpsertEffectTarget();
      EffectRequest           request = request("first@example.com");
      assertThat(target.reconcile(request).outcome()).isEqualTo(ReconcileResult.Outcome.NOT_FOUND);
      EffectResult first = target.deliver(request);
      assertThat(first.outcome()).isEqualTo(EffectResult.Outcome.SENT);
      assertThat(first.providerRef()).isNotBlank();
      assertThat(target.deliver(request("updated@example.com"))).isEqualTo(first);
      List<QRecord> records = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getRecords();
      assertThat(records).hasSize(1);
      assertThat(records.get(0).getValueString("email")).isEqualTo("updated@example.com");
      assertThat(target.reconcile(request)).isEqualTo(ReconcileResult.found(first.providerRef()));
      assertThat(QContext.getQSession().getUser().getIdReference()).isEqualTo(DEFAULT_USER_ID);
   }



   /*******************************************************************************
    ** Validation errors on both insert and update are definite rejections.
    *******************************************************************************/
   @Test
   void recordErrorsAreFailed() throws Exception
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY).getField("email").setIsRequired(true);
      TableUpsertEffectTarget target = new TableUpsertEffectTarget();
      assertThat(target.deliver(request(null)).outcome()).isEqualTo(EffectResult.Outcome.FAILED);
      assertThat(target.deliver(request("valid@example.com")).outcome()).isEqualTo(EffectResult.Outcome.SENT);
      assertThat(target.deliver(request(null)).outcome()).isEqualTo(EffectResult.Outcome.FAILED);
      assertThat(new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getRecords()).hasSize(1);
   }



   /*******************************************************************************
    ** An incomplete key cannot cause an unfiltered update.
    *******************************************************************************/
   @Test
   void missingNaturalKeyIsUnknown() throws Exception
   {
      EffectRequest request = new EffectRequest("key", "upsert", JobEffectType.IDEMPOTENT_EXTERNAL, 1, 0, "row",
         Map.of("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY, "keyFields", new ArrayList<>(List.of("firstName")),
            "values", new HashMap<String, Serializable>()), null, 1);
      assertThat(new TableUpsertEffectTarget().deliver(request).outcome()).isEqualTo(EffectResult.Outcome.UNKNOWN);
      assertThat(new TableUpsertEffectTarget().reconcile(request).outcome()).isEqualTo(ReconcileResult.Outcome.UNKNOWN);
      assertThat(new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY)).getRecords()).isEmpty();
   }



   /*******************************************************************************
    ** A composite natural key stays fixed while the email value changes.
    *******************************************************************************/
   private EffectRequest request(String email)
   {
      HashMap<String, Serializable> values = new HashMap<>();
      values.put("firstName", "Effect");
      values.put("lastName", "Test");
      values.put("email", email);
      return (new EffectRequest("1:0:row:upsert", "upsert", JobEffectType.IDEMPOTENT_EXTERNAL, 1, 0, "row",
         Map.of("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY, "keyFields", new ArrayList<>(List.of("firstName", "lastName")), "values", values), null, 1));
   }
}
