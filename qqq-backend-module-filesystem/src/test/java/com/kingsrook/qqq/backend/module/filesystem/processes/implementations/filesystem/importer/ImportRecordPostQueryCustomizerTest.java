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

package com.kingsrook.qqq.backend.module.filesystem.processes.implementations.filesystem.importer;


import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ImportRecordPostQueryCustomizer 
 *******************************************************************************/
class ImportRecordPostQueryCustomizerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      Instant createDate = Instant.parse("2024-01-08T20:07:21Z");

      List<QRecord> output = new ImportRecordPostQueryCustomizer().apply(List.of(
         new QRecord()
            .withTableName("personImporterImportRecord")
            .withValue("importFileId", 1)
            .withValue("unmapped", 2)
            .withValue("unstructured", 3)
            .withValue("nosqlObject", MapBuilder.of(HashMap::new).with("foo", "bar").with("createDate", createDate).build())
      ));

      assertEquals(1, output.get(0).getValue("importFileId"));
      assertEquals(2, output.get(0).getValue("unmapped"));
      assertEquals(3, output.get(0).getValue("unstructured"));
      assertEquals(Map.of("foo", "bar", "createDate", createDate), output.get(0).getValue("nosqlObject"));

      ///////////////////////////////////////////////////////////////////////////////////////////
      // make sure all un-structured fields get put in the "values" field as a JSON string     //
      // compare as maps, beacuse JSONObject seems to care about the ordering, which, we don't //
      ///////////////////////////////////////////////////////////////////////////////////////////
      Map<String, Object> expectedMap = new JSONObject("""
         {
            "unmapped": 2,
            "unstructured": 3,
            "nosqlObject":
            {
               "foo": "bar",
               "createDate": "%s"
            }
         }
         """.formatted(createDate)).toMap();
      Map<String, Object> actualMap = new JSONObject(output.get(0).getValueString("values")).toMap();
      assertThat(actualMap).isEqualTo(expectedMap);
   }

}