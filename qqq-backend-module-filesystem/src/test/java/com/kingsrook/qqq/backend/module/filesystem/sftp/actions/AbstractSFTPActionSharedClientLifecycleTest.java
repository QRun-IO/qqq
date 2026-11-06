/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import org.apache.sshd.client.SshClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for the shared SshClient lifecycle in AbstractSFTPAction.
 **
 ** No SFTP server is involved: only the local client's start/stop state.
 *******************************************************************************/
class AbstractSFTPActionSharedClientLifecycleTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      AbstractSFTPAction.shutdownSharedClient();
   }



   /*******************************************************************************
    ** The shared client is started on first use and reused while running.
    *******************************************************************************/
   @Test
   void testSharedClientIsStartedAndReused()
   {
      SshClient first = AbstractSFTPAction.getSharedSshClient();
      assertTrue(first.isStarted(), "shared client should be started on first use");

      SshClient again = AbstractSFTPAction.getSharedSshClient();
      assertSame(first, again, "a running shared client should be reused");
   }



   /*******************************************************************************
    ** A stopped application must be able to start the shared SFTP client again.
    *******************************************************************************/
   @Test
   void testSharedClientCanBeStartedAgainAfterShutdown()
   {
      SshClient first = AbstractSFTPAction.getSharedSshClient();
      assertTrue(first.isStarted());

      AbstractSFTPAction.shutdownSharedClient();
      assertFalse(first.isStarted(), "shutdown should stop the shared client");

      SshClient second = AbstractSFTPAction.getSharedSshClient();
      assertTrue(second.isStarted(), "a second getInstance() after stop() must yield a started client");
      assertNotSame(first, second, "the stopped client must not be handed out again");
   }



   /*******************************************************************************
    ** Shutdown without a prior client must not initialize (start) one.
    *******************************************************************************/
   @Test
   void testShutdownDoesNotStartClient()
   {
      AbstractSFTPAction.shutdownSharedClient();
      AbstractSFTPAction.shutdownSharedClient();

      SshClient client = AbstractSFTPAction.getSharedSshClient();
      assertTrue(client.isStarted());
   }

}
