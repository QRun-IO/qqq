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

package com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata;


import com.kingsrook.qqq.backend.core.adapters.QInstanceAdapter;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for S3BackendMetaData
 *******************************************************************************/
@Disabled("This concept doesn't seem right any more.  We will want/need custom JSON/YAML serialization, so, let us disable this test, at least for now, and maybe permanently")
class S3BackendMetaDataTest
{


   /*******************************************************************************
    ** Test that an instance can be serialized as expected
    *******************************************************************************/
   @Test
   public void testSerializingToJson() throws QException
   {
      TestUtils.resetTestInstanceCounter();
      QInstance qInstance = TestUtils.defineInstance();
      String    json      = new QInstanceAdapter().qInstanceToJsonIncludingBackend(qInstance);
      System.out.println(JsonUtils.prettyPrint(json));
      System.out.println(json);
      String expectToContain = """
         {"s3":{"bucketName":"localstack-test-bucket","basePath":"test-files","backendType":"s3","name":"s3","usesVariants":false}""";
      assertTrue(json.contains(expectToContain));
   }



   /*******************************************************************************
    ** Test that an instance can be deserialized as expected
    *******************************************************************************/
   @Test
   public void testDeserializingFromJson() throws Exception
   {
      QInstanceAdapter qInstanceAdapter = new QInstanceAdapter();

      QInstance qInstance = TestUtils.defineInstance();
      String    json      = qInstanceAdapter.qInstanceToJsonIncludingBackend(qInstance);

      QInstance deserialized = qInstanceAdapter.jsonToQInstanceIncludingBackends(json);
      assertThat(deserialized.getBackends()).usingRecursiveComparison()
         // TODO seeing occassional flaps on this field - where it can be null 1 out of 10 runs... unclear why.
         .ignoringFields("mock.backendType")
         .isEqualTo(qInstance.getBackends());
   }
}
