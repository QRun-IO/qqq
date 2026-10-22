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


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 **
 *******************************************************************************/
public class SFTPDeleteActionTest extends BaseSFTPTest
{
   private String filesBasename = "delete-test-";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach() throws Exception
   {
      rmrfInContainer(REMOTE_DIR + "/" + filesBasename + "*");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSuccessfulDeleteMultiple() throws QException
   {
      int initialCount = new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_SFTP_FILE)).getCount();

      String filename1 = filesBasename + "A.txt";
      String filename2 = filesBasename + "B.txt";
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SFTP_FILE).withRecords(List.of(
         new QRecord().withValue("fileName", filename1).withValue("contents", "bytes"),
         new QRecord().withValue("fileName", filename2).withValue("contents", "bytes"))));
      assertEquals(initialCount + 2, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_SFTP_FILE)).getCount());

      DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_SFTP_FILE).withPrimaryKeys(List.of(filename1, filename2)));
      assertEquals(2, deleteOutput.getDeletedRecordCount());
      assertEquals(0, deleteOutput.getRecordsWithErrors().size());
      assertEquals(initialCount, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_SFTP_FILE)).getCount());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testFailedDelete() throws Exception
   {
      int initialCount = new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_SFTP_FILE)).getCount();

      String filename1 = filesBasename + "C.txt";
      String filename2 = filesBasename + "D.txt";
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_SFTP_FILE).withRecords(List.of(
         new QRecord().withValue("fileName", filename1).withValue("contents", "bytes"),
         new QRecord().withValue("fileName", filename2).withValue("contents", "bytes"))));
      assertEquals(initialCount + 2, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_SFTP_FILE)).getCount());

      sftpContainer.execInContainer("chmod", "000", REMOTE_DIR);
      DeleteOutput deleteOutput = new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_SFTP_FILE).withPrimaryKeys(List.of(filename1, filename2)));
      sftpContainer.execInContainer("chmod", "777", REMOTE_DIR);

      assertEquals(0, deleteOutput.getDeletedRecordCount());
      assertEquals(2, deleteOutput.getRecordsWithErrors().size());
      assertThat(deleteOutput.getRecordsWithErrors().get(0).getErrorsAsString()).contains("Error deleting file: Permission denied");
      assertThat(deleteOutput.getRecordsWithErrors().get(1).getErrorsAsString()).contains("Error deleting file: Permission denied");
      assertEquals(initialCount + 2, new CountAction().execute(new CountInput(TestUtils.TABLE_NAME_SFTP_FILE)).getCount());
   }

}