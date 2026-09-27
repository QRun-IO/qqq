/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.filesystem.TestUtils;
import com.kingsrook.qqq.backend.module.filesystem.sftp.BaseSFTPTest;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


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
      try
      {
         revokeUploadFilesDirWritePermission();
         String data = "oops!";
         assertThatThrownBy(() -> runTest(data))
            .isInstanceOf(IOException.class)
            .hasStackTraceContaining("Permission denied");
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
