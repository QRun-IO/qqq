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


import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.s3.BaseS3Test;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for FilesystemStorageAction 
 *******************************************************************************/
public class S3StorageActionTest extends BaseS3Test
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test() throws Exception
   {
      String       data         = "Hellooo, Storage.";
      StorageInput storageInput = new StorageInput(TestUtils.TABLE_NAME_BLOB_S3).withReference("test.txt");

      /////////////////////////////////////////////////////////////////////////
      // work directly w/ s3 action class here, so we can set s3 utils in it //
      /////////////////////////////////////////////////////////////////////////
      S3StorageAction s3StorageAction = new S3StorageAction();
      s3StorageAction.setS3Utils(getS3Utils());
      OutputStream outputStream = s3StorageAction.createOutputStream(storageInput);
      outputStream.write(data.getBytes(StandardCharsets.UTF_8));
      outputStream.close();

      InputStream           inputStream           = s3StorageAction.getInputStream(storageInput);
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      inputStream.transferTo(byteArrayOutputStream);

      assertEquals(data, byteArrayOutputStream.toString(StandardCharsets.UTF_8));

   }

}