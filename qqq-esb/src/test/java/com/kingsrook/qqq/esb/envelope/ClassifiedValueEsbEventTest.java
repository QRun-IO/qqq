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

package com.kingsrook.qqq.esb.envelope;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Default and explicit classified-value payload policy.
 *******************************************************************************/
class ClassifiedValueEsbEventTest extends com.kingsrook.qqq.esb.EsbTestBase
{
   /*******************************************************************************
    ** Cleanup.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Classified fields omitted for all mutations.
    *******************************************************************************/
   @Test
   void classifiedFieldsOmittedForAllMutations()
   {
      QTableMetaData table = new QTableMetaData().withName("patient").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("phi", QFieldType.STRING).withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI)))
         .withField(new QFieldMetaData("pii", QFieldType.STRING).withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PII)))
         .withField(new QFieldMetaData("public", QFieldType.STRING));
      QInstance instance = QContext.getQInstance();
      table.setBackendName(BACKEND_NAME);
      instance.addTable(table);
      QContext.init(instance, new QSession());
      QRecord record = new QRecord().withValue("id", 1).withValue("phi", "CANARY-PHI-secret").withValue("pii", "CANARY-PHI-secret").withValue("public", "visible");
      QRecord old = new QRecord().withValue("id", 1).withValue("phi", "CANARY-PHI-old").withValue("pii", "CANARY-PHI-old");
      for(RecordChangeType type : RecordChangeType.values())
      {
         EsbEvent safe = EsbEventFactory.forRecordChange("test", "patient", type, record, old);
         assertThat(JsonUtils.toJson(safe)).doesNotContain("CANARY", "secret");
         EsbEvent included = EsbEventFactory.forRecordChange("test", "patient", type, record, old, true);
         assertThat(JsonUtils.toJson(included)).contains("CANARY");
         if(type == RecordChangeType.UPDATE)
         {
            assertThat(safe.getData().get("changedFields")).isEqualTo(List.of("phi", "pii", "public"));
            assertThat(safe.getData().get("record")).isEqualTo(Map.of("id", 1, "public", "visible"));
         }
      }
      assertThat(record.getValue("phi")).isEqualTo("CANARY-PHI-secret");
   }
}
