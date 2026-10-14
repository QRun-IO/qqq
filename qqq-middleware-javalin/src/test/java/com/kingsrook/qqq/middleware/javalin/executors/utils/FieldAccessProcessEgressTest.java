/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessDecision;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessResolverMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldAccessTier;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.FieldMaskStyle;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Process result ownership, including nested values and missing table metadata.
 *******************************************************************************/
class FieldAccessProcessEgressTest
{
   /*******************************************************************************
    ** Field names intentionally overlap; the record table determines their policy.
    *******************************************************************************/
   @BeforeEach
   void setup()
   {
      QInstance instance = TestUtils.defineInstance();
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("user")));
      for(String name : List.of("public", "private"))
      {
         instance.addTable(new QTableMetaData().withName(name).withBackendName(TestUtils.BACKEND_NAME_MEMORY).withPrimaryKeyField("id")
            .withField(new QFieldMetaData("id", QFieldType.INTEGER)).withField(new QFieldMetaData("secret", QFieldType.STRING)));
      }
      instance.addProcess(new QProcessMetaData().withName("mixed").withTableName("public"));
      instance.addProcess(new QProcessMetaData().withName("unknown"));
      FieldAccessResolverMetaData.ofOrWithNew(instance).withResolverInstance((table, field) ->
         new FieldAccessDecision(table.getName().equals("private") ? FieldAccessTier.MASKED : FieldAccessTier.FULL, FieldMaskStyle.FULL, "test"));
   }



   /*******************************************************************************
    ** Keep this isolated from unrelated middleware tests.
    *******************************************************************************/
   @AfterEach
   void cleanup()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** One mixed list must not be masked against the process's public table.
    *******************************************************************************/
   @Test
   void eachRecordUsesOwnTable()
   {
      QRecord restricted = new QRecord().withTableName("private").withValue("id", 1).withValue("secret", "SENTINEL");
      List<QRecord> result = ProcessExecutorUtils.recordsForCaller("mixed", List.of(restricted,
         new QRecord().withValue("id", 2).withValue("secret", "public")));
      assertEquals("***", result.get(0).getValue("secret"));
      assertEquals("public", result.get(1).getValue("secret"));
      assertEquals("SENTINEL", restricted.getValue("secret"));
   }



   /*******************************************************************************
    ** Missing or invalid ownership withholds display values, labels and associations.
    *******************************************************************************/
   @Test
   void unknownOwnershipRetainsOnlyId()
   {
      for(String table : new String[] { null, "missing" })
      {
         QRecord record = new QRecord().withTableName(table).withValue("id", 1).withValue("secret", "SENTINEL")
            .withDisplayValue("secret", "SENTINEL").withRecordLabel("SENTINEL");
         QRecord result = ProcessExecutorUtils.recordsForCaller("unknown", List.of(record)).get(0);
         assertEquals(Map.of("id", 1), result.getValues());
         assertTrue(result.getDisplayValues().isEmpty());
         assertEquals(null, result.getRecordLabel());
         assertEquals("SENTINEL", record.getValue("secret"));
      }
   }



   /*******************************************************************************
    ** Maps nested in process values are also an egress path for QRecords.
    *******************************************************************************/
   @Test
   void nestedMapRecordsAreMasked()
   {
      LinkedHashMap<String, Serializable> nested = new LinkedHashMap<>();
      nested.put("rows", new ArrayList<>(List.of(new QRecord().withTableName("private").withValue("id", 1).withValue("secret", "SENTINEL"))));
      Map<String, Serializable> result = ProcessExecutorUtils.valuesForCaller("mixed", Map.of("result", nested));
      Map<?, ?> copied = (Map<?, ?>) result.get("result");
      assertEquals("***", ((QRecord) ((List<?>) copied.get("rows")).get(0)).getValue("secret"));
   }
}
