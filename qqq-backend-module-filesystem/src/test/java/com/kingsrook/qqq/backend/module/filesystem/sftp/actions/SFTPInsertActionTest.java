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

package com.kingsrook.qqq.backend.module.filesystem.sftp.actions;


import java.io.IOException;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.FilesystemRecordBackendDetailFields;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 **
 *******************************************************************************/
public class SFTPInsertActionTest extends BaseSFTPTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCardinalityOne() throws QException, IOException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_SFTP_FILE);
      insertInput.setRecords(List.of(
         new QRecord().withValue("fileName", "file2.txt").withValue("contents", "Hi, Bob.")
      ));

      SFTPInsertAction insertAction = new SFTPInsertAction();

      InsertOutput insertOutput = insertAction.execute(insertInput);
      assertThat(insertOutput.getRecords())
         .allMatch(record -> record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH).contains(BaseSFTPTest.BACKEND_FOLDER));

      QRecord record   = insertOutput.getRecords().get(0);
      String  fullPath = record.getBackendDetailString(FilesystemRecordBackendDetailFields.FULL_PATH);
      assertThat(record.getErrors()).isNullOrEmpty();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCardinalityOnePermissionError() throws Exception
   {
      try
      {
         revokeUploadFilesDirWritePermission();

         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(TestUtils.TABLE_NAME_SFTP_FILE);
         insertInput.setRecords(List.of(
            new QRecord().withValue("fileName", "file2.txt").withValue("contents", "Hi, Bob.")
         ));

         SFTPInsertAction insertAction = new SFTPInsertAction();

         InsertOutput insertOutput = insertAction.execute(insertInput);

         QRecord record = insertOutput.getRecords().get(0);
         assertThat(record.getErrors()).isNotEmpty();
         assertThat(record.getErrors().get(0).getMessage()).contains("Error writing file: Permission denied");
      }
      finally
      {
         grantUploadFilesDirWritePermission();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCardinalityMany() throws QException, IOException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_SFTP);
      insertInput.setRecords(List.of(
         new QRecord().withValue("id", "1").withValue("firstName", "Bob")
      ));

      SFTPInsertAction insertAction = new SFTPInsertAction();

      assertThatThrownBy(() -> insertAction.execute(insertInput))
         .hasRootCauseInstanceOf(NotImplementedException.class);
   }
}