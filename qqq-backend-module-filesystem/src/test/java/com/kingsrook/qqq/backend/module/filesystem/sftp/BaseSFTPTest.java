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

package com.kingsrook.qqq.backend.module.filesystem.sftp;


import com.kingsrook.qqq.backend.module.filesystem.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;


/*******************************************************************************
 ** Base class for tests that want to be able to work with sftp testcontainer
 *******************************************************************************/
public class BaseSFTPTest extends BaseTest
{
   public static final int    PORT      = 22;
   public static final String USERNAME  = "testuser";
   public static final String PASSWORD  = "testpass";
   public static final String HOST_NAME = "localhost";

   public static final String BACKEND_FOLDER = "upload";
   public static final String TABLE_FOLDER   = "files";
   public static final String REMOTE_DIR     = "/home/" + USERNAME + "/" + BACKEND_FOLDER + "/" + TABLE_FOLDER;

   protected static GenericContainer<?> sftpContainer;
   private static   Integer             currentPort;



   /***************************************************************************
    **
    ***************************************************************************/
   @BeforeAll
   static void setUp() throws Exception
   {
      sftpContainer = new GenericContainer<>("atmoz/sftp:latest")
         .withExposedPorts(PORT)
         .withCommand(USERNAME + ":" + PASSWORD + ":1001");

      sftpContainer.start();

      for(int i = 0; i < 5; i++)
      {
         copyFileToContainer("files/testfile.txt", REMOTE_DIR + "/testfile-" + i + ".txt");
      }

      grantUploadFilesDirWritePermission();

      ///////////////////////////////////////////////////
      // add our test-only public key to the container //
      ///////////////////////////////////////////////////
      String sshDir = "/home/" + USERNAME + "/.ssh";
      sftpContainer.execInContainer("mkdir", sshDir);
      sftpContainer.execInContainer("chmod", "700", sshDir);
      sftpContainer.execInContainer("chown", USERNAME, sshDir);
      copyFileToContainer("test-only-key.pub", sshDir + "/authorized_keys");
      sftpContainer.execInContainer("chmod", "600", sshDir + "/authorized_keys");
      sftpContainer.execInContainer("chown", USERNAME, sshDir + "/authorized_keys");

      currentPort = sftpContainer.getMappedPort(22);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected static void copyFileToContainer(String sourceFileClasspathResourceName, String fullRemotePath)
   {
      sftpContainer.copyFileToContainer(MountableFile.forClasspathResource(sourceFileClasspathResourceName), fullRemotePath);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected static void rmrfInContainer(String fullRemotePath) throws Exception
   {
      sftpContainer.execInContainer("rm", "-rf", fullRemotePath);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @AfterAll
   static void tearDown()
   {
      if(sftpContainer != null)
      {
         sftpContainer.stop();
      }
   }



   /*******************************************************************************
    ** Getter for currentPort
    **
    *******************************************************************************/
   public static Integer getCurrentPort()
   {
      return currentPort;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected static void revokeUploadFilesDirWritePermission() throws Exception
   {
      setUploadFilesDirPermission("444");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   protected static void grantUploadFilesDirWritePermission() throws Exception
   {
      setUploadFilesDirPermission("777");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void setUploadFilesDirPermission(String mode) throws Exception
   {
      sftpContainer.execInContainer("chmod", mode, "/home/testuser/upload/files");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected void mkdirInSftpContainerUnderHomeTestuser(String path) throws Exception
   {
      sftpContainer.execInContainer("mkdir", "-p", "/home/testuser/" + path);
   }
}
