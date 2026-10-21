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

package com.kingsrook.qqq.backend.module.filesystem.s3.actions;


import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.s3.model.metadata.S3BackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.s3.utils.S3Utils;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Delete dispatch and error propagation without an external S3 service.
 *******************************************************************************/
class S3StorageDeletionTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDeleteObject() throws Exception
   {
      List<DeleteObjectRequest> requests = new ArrayList<>();
      SdkClientException failure = SdkClientException.create("denied");
      S3Client client = (S3Client) Proxy.newProxyInstance(S3Client.class.getClassLoader(), new Class<?>[] { S3Client.class }, (proxy, method, arguments) ->
      {
         if(!method.getName().equals("deleteObject"))
         {
            throw new AssertionError("Unexpected S3 call: " + method.getName());
         }
         if(!requests.isEmpty())
         {
            throw failure;
         }
         requests.add((DeleteObjectRequest) arguments[0]);
         return DeleteObjectResponse.builder().build();
      });
      S3Utils utils = new S3Utils();
      utils.setS3Client(client);
      S3StorageAction action = new S3StorageAction();
      action.setS3Utils(utils);
      StorageInput input = new StorageInput(TestUtils.TABLE_NAME_BLOB_S3).withReference("reports/partial.csv");
      action.deleteObject(input);
      assertThat(requests).hasSize(1);
      assertThat(requests.get(0).bucket()).isEqualTo(((S3BackendMetaData) input.getBackend()).getBucketName());
      assertThat(requests.get(0).key()).endsWith("reports/partial.csv");
      assertThat(assertThrows(QException.class, () -> action.deleteObject(input)).getCause()).isSameAs(failure);
   }
}
