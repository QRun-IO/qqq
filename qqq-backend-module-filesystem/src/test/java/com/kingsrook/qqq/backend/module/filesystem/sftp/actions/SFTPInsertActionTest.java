/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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