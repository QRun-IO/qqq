/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.filesystem.sftp.utils;


import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.common.SftpException;
import org.jetbrains.annotations.NotNull;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class SFTPOutputStream extends PipedOutputStream
{
   private static final QLogger LOG = QLogger.getLogger(SFTPOutputStream.class);

   private final SftpClient sftpClient;
   private final String     path;

   private final PipedInputStream pipedInputStream;
   private final Future<?>        putFuture;

   private AtomicBoolean              started      = new AtomicBoolean(false);
   private AtomicReference<Exception> putException = new AtomicReference<>(null);



   /***************************************************************************
    **
    ***************************************************************************/
   public SFTPOutputStream(SftpClient sftpClient, String path) throws IOException
   {
      pipedInputStream = new PipedInputStream(this, 32 * 1024);

      this.sftpClient = sftpClient;
      this.path = path;

      putFuture = Executors.newSingleThreadExecutor().submit(() ->
      {
         try
         {
            started.set(true);
            LOG.debug("Starting sftp put", logPair("path", path));
            sftpClient.put(pipedInputStream, path);
         }
         catch(Exception e)
         {
            putException.set(e);
            LOG.error("Error putting SFTP output stream", e);

            try
            {
               pipedInputStream.close();
            }
            catch(IOException ex)
            {
               LOG.error("Secondary error closing pipedInputStream after sftp put error", e);
            }

            throw new RuntimeException(e);
         }
      });
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void write(@NotNull byte[] b) throws IOException
   {
      try
      {
         super.write(b);
      }
      catch(IOException e)
      {
         if(putException.get() != null)
         {
            if(putException.get() instanceof SftpException sftpException)
            {
               throw new IOException("Error performing SFTP put for path [" + path + "]: " + sftpException.getMessage());
            }
            throw new IOException("Error performing SFTP put", putException.get());
         }

         throw new IOException("Error writing to SFTP output stream", e);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void close() throws IOException
   {
      try
      {
         /////////////////////////////////////////////////////////////////////////////////////////////
         // don't try to close anything until we know that the sftpClient.put call's thread         //
         // has tried to start (otherwise, race condition could cause us to close things too early) //
         /////////////////////////////////////////////////////////////////////////////////////////////
         int sleepLoops = 0;
         while(!started.get() && sleepLoops++ <= 30)
         {
            SleepUtils.sleep(1, TimeUnit.SECONDS);
         }

         ///////////////////////////////////////////////////////////////////////////////////////
         // closing the pipedOutputStream (super) causes things to flush and complete the put //
         ///////////////////////////////////////////////////////////////////////////////////////
         super.close();

         ////////////////////////////////
         // wait for the put to finish //
         ////////////////////////////////
         putFuture.get(60 - sleepLoops, TimeUnit.SECONDS);

         ///////////////////////////////////////////////////////////////////////////////
         // in case the put-future never did start, throw explicitly mentioning that. //
         ///////////////////////////////////////////////////////////////////////////////
         if(sleepLoops >= 30)
         {
            throw (new Exception("future to can sftpClient.put() was never started."));
         }
      }
      catch(ExecutionException ee)
      {
         throw new IOException("Error performing SFTP put", ee);
      }
      catch(Exception e)
      {
         if(putException.get() != null)
         {
            throw new IOException("Error performing SFTP put", putException.get());
         }

         throw new IOException("Error closing SFTP output stream", e);
      }
      finally
      {
         try
         {
            sftpClient.close();
         }
         catch(IOException e)
         {
            LOG.error("Error closing SFTP client", e);
         }
      }
   }

}
