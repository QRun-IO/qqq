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

package com.kingsrook.qqq.backend.module.filesystem.sftp.actions;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for FilesystemStorageAction 
 *******************************************************************************/
public class SFTPStorageActionTest extends BaseSFTPTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSmall() throws Exception
   {
      String data = "Hellooo, Storage.";
      runTest(data);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testPermissionError() throws Exception
   {
      String remotePath = REMOTE_DIR + "/fromStorageAction.txt";
      copyFileToContainer("files/testfile.txt", remotePath);
      assertEquals(0, sftpContainer.execInContainer("chmod", "666", remotePath).getExitCode());
      String before = sftpContainer.execInContainer("cat", remotePath).getStdout();
      try
      {
         revokeUploadFilesDirWritePermission();
         QException error = assertThrows(QException.class, () -> runTest("oops!"));
         SftpException denial = assertInstanceOf(SftpException.class, ExceptionUtils.getRootException(error));
         assertEquals(SftpConstants.SSH_FX_PERMISSION_DENIED, denial.getStatus());
         assertEquals(before, sftpContainer.execInContainer("cat", remotePath).getStdout());
      }
      finally
      {
         grantUploadFilesDirWritePermission();
      }
      runTest("allowed again");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testLarge() throws Exception
   {
      String data = StringUtils.join("!", Collections.nCopies(5_000_000, "Hello"));
      runTest(data);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void runTest(String data) throws QException, IOException
   {
      StorageInput storageInput = new StorageInput(TestUtils.TABLE_NAME_SFTP_FILE).withReference("fromStorageAction.txt");

      StorageAction storageAction = new StorageAction();
      OutputStream  outputStream  = storageAction.createOutputStream(storageInput);
      outputStream.write(data.getBytes(StandardCharsets.UTF_8));
      outputStream.close();

      InputStream           inputStream           = storageAction.getInputStream(storageInput);
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      inputStream.transferTo(byteArrayOutputStream);

      assertEquals(data.length(), byteArrayOutputStream.toString(StandardCharsets.UTF_8).length());
      assertEquals(data, byteArrayOutputStream.toString(StandardCharsets.UTF_8));
   }

}
