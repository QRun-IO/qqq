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

package com.kingsrook.qqq.backend.module.filesystem.local.actions;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 **
 *******************************************************************************/
public class FilesystemInsertActionTest extends FilesystemActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCardinalityOne() throws QException, IOException
   {
      QInstance   qInstance   = TestUtils.defineInstance();
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_BLOB_LOCAL_FS);
      insertInput.setRecords(List.of(
         new QRecord().withValue("fileName", "file1.txt").withValue("contents", "Hello, World")
      ));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      assertThat(insertOutput.getRecords())
         .allMatch(record -> record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH).contains(TestUtils.BASE_PATH))
         .allMatch(record -> record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH).contains("blobs"));

      assertEquals("Hello, World", FileUtils.readFileToString(new File(insertOutput.getRecords().get(0).getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH)), StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCardinalityMany() throws QException, IOException
   {
      QInstance   qInstance   = TestUtils.defineInstance();
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_LOCAL_FS_JSON);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", "1").withValue("firstName", "Bob")
      ));
      assertThatThrownBy(() -> new InsertAction().execute(insertInput))
         .hasRootCauseInstanceOf(NotImplementedException.class);
   }

}