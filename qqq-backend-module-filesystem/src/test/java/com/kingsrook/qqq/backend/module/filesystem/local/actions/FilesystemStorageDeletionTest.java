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

package com.kingsrook.qqq.backend.module.filesystem.local.actions;


import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Local object cleanup in a portable temporary directory.
 *******************************************************************************/
class FilesystemStorageDeletionTest extends BaseTest
{
   @TempDir
   Path directory;



   /*******************************************************************************
    ** Deletion uses the same contained path and tolerates an absent object.
    *******************************************************************************/
   @Test
   void testDeleteObject() throws Exception
   {
      ((FilesystemBackendMetaData) QContext.getQInstance().getBackend(TestUtils.defineLocalFilesystemBackend().getName())).setBasePath(directory.toString());
      StorageInput input = new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("reports/partial.csv");
      StorageAction action = new StorageAction();
      try(OutputStream output = action.createOutputStream(input))
      {
         output.write("partial".getBytes(StandardCharsets.UTF_8));
      }
      action.deleteObject(input);
      action.deleteObject(input);
      assertThrows(QException.class, () -> action.getInputStream(input));
      assertThrows(QBadRequestException.class, () -> action.deleteObject(new StorageInput(TestUtils.TABLE_NAME_BLOB_LOCAL_FS).withReference("../outside.txt")));
   }
}
