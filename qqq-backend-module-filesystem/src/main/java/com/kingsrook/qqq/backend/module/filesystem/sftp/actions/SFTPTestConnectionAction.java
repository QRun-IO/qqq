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

package com.kingsrook.qqq.backend.module.filesystem.sftp.actions;


import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.apache.sshd.sftp.client.SftpClient;


/*******************************************************************************
 ** action for testing credentials for an SFTP backend connection
 *******************************************************************************/
public class SFTPTestConnectionAction extends AbstractSFTPAction
{

   /***************************************************************************
    **
    ***************************************************************************/
   public SFTPTestConnectionTestOutput testConnection(SFTPTestConnectionTestInput input)
   {
      try(SftpClient sftpClient = super.makeConnection(input.getUsername(), input.getHostName(), input.getPort(), input.getPassword(), input.getPrivateKey()))
      {
         SFTPTestConnectionTestOutput output = new SFTPTestConnectionTestOutput().withIsConnectionSuccess(true);

         if(StringUtils.hasContent(input.basePath))
         {
            try
            {
               Iterable<SftpClient.DirEntry> dirEntries = sftpClient.readDir(input.basePath);

               /////////////////////////////////////////////////////////////////////////
               // it seems like only the .iterator call throws if bad directory here. //
               /////////////////////////////////////////////////////////////////////////
               dirEntries.iterator();
               output.setIsListBasePathSuccess(true);
            }
            catch(Exception e)
            {
               output.setIsListBasePathSuccess(false);
               output.setListBasePathErrorMessage(e.getMessage());
            }
         }

         return output;
      }
      catch(Exception e)
      {
         return new SFTPTestConnectionTestOutput().withIsConnectionSuccess(false).withConnectionErrorMessage(e.getMessage());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class SFTPTestConnectionTestInput
   {
      private String  username;
      private String  hostName;
      private Integer port;
      private String  password;
      private String  basePath;
      private byte[]  privateKey;



      /*******************************************************************************
       ** Getter for username
       **
       *******************************************************************************/
      public String getUsername()
      {
         return username;
      }



      /*******************************************************************************
       ** Setter for username
       **
       *******************************************************************************/
      public void setUsername(String username)
      {
         this.username = username;
      }



      /*******************************************************************************
       ** Fluent setter for username
       **
       *******************************************************************************/
      public SFTPTestConnectionTestInput withUsername(String username)
      {
         this.username = username;
         return (this);
      }



      /*******************************************************************************
       ** Getter for hostName
       **
       *******************************************************************************/
      public String getHostName()
      {
         return hostName;
      }



      /*******************************************************************************
       ** Setter for hostName
       **
       *******************************************************************************/
      public void setHostName(String hostName)
      {
         this.hostName = hostName;
      }



      /*******************************************************************************
       ** Fluent setter for hostName
       **
       *******************************************************************************/
      public SFTPTestConnectionTestInput withHostName(String hostName)
      {
         this.hostName = hostName;
         return (this);
      }



      /*******************************************************************************
       ** Getter for port
       **
       *******************************************************************************/
      public Integer getPort()
      {
         return port;
      }



      /*******************************************************************************
       ** Setter for port
       **
       *******************************************************************************/
      public void setPort(Integer port)
      {
         this.port = port;
      }



      /*******************************************************************************
       ** Fluent setter for port
       **
       *******************************************************************************/
      public SFTPTestConnectionTestInput withPort(Integer port)
      {
         this.port = port;
         return (this);
      }



      /*******************************************************************************
       ** Getter for password
       **
       *******************************************************************************/
      public String getPassword()
      {
         return password;
      }



      /*******************************************************************************
       ** Setter for password
       **
       *******************************************************************************/
      public void setPassword(String password)
      {
         this.password = password;
      }



      /*******************************************************************************
       ** Fluent setter for password
       **
       *******************************************************************************/
      public SFTPTestConnectionTestInput withPassword(String password)
      {
         this.password = password;
         return (this);
      }



      /*******************************************************************************
       ** Getter for basePath
       **
       *******************************************************************************/
      public String getBasePath()
      {
         return basePath;
      }



      /*******************************************************************************
       ** Setter for basePath
       **
       *******************************************************************************/
      public void setBasePath(String basePath)
      {
         this.basePath = basePath;
      }



      /*******************************************************************************
       ** Fluent setter for basePath
       **
       *******************************************************************************/
      public SFTPTestConnectionTestInput withBasePath(String basePath)
      {
         this.basePath = basePath;
         return (this);
      }



      /*******************************************************************************
       ** Getter for privateKey
       **
       *******************************************************************************/
      public byte[] getPrivateKey()
      {
         return privateKey;
      }



      /*******************************************************************************
       ** Setter for privateKey
       **
       *******************************************************************************/
      public void setPrivateKey(byte[] privateKey)
      {
         this.privateKey = privateKey;
      }



      /*******************************************************************************
       ** Fluent setter for privateKey
       **
       *******************************************************************************/
      public SFTPTestConnectionTestInput withPrivateKey(byte[] privateKey)
      {
         this.privateKey = privateKey;
         return (this);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class SFTPTestConnectionTestOutput
   {
      private Boolean isConnectionSuccess;
      private String  connectionErrorMessage;

      private Boolean isListBasePathSuccess;
      private String  listBasePathErrorMessage;



      /*******************************************************************************
       ** Getter for isSuccess
       **
       *******************************************************************************/
      public Boolean getIsConnectionSuccess()
      {
         return isConnectionSuccess;
      }



      /*******************************************************************************
       ** Setter for isSuccess
       **
       *******************************************************************************/
      public void setIsConnectionSuccess(Boolean isSuccess)
      {
         this.isConnectionSuccess = isSuccess;
      }



      /*******************************************************************************
       ** Fluent setter for isSuccess
       **
       *******************************************************************************/
      public SFTPTestConnectionTestOutput withIsConnectionSuccess(Boolean isSuccess)
      {
         this.isConnectionSuccess = isSuccess;
         return (this);
      }



      /*******************************************************************************
       ** Getter for connectionErrorMessage
       **
       *******************************************************************************/
      public String getConnectionErrorMessage()
      {
         return connectionErrorMessage;
      }



      /*******************************************************************************
       ** Setter for connectionErrorMessage
       **
       *******************************************************************************/
      public void setConnectionErrorMessage(String connectionErrorMessage)
      {
         this.connectionErrorMessage = connectionErrorMessage;
      }



      /*******************************************************************************
       ** Fluent setter for connectionErrorMessage
       **
       *******************************************************************************/
      public SFTPTestConnectionTestOutput withConnectionErrorMessage(String connectionErrorMessage)
      {
         this.connectionErrorMessage = connectionErrorMessage;
         return (this);
      }



      /*******************************************************************************
       ** Getter for listBasePathErrorMessage
       *******************************************************************************/
      public String getListBasePathErrorMessage()
      {
         return (this.listBasePathErrorMessage);
      }



      /*******************************************************************************
       ** Setter for listBasePathErrorMessage
       *******************************************************************************/
      public void setListBasePathErrorMessage(String listBasePathErrorMessage)
      {
         this.listBasePathErrorMessage = listBasePathErrorMessage;
      }



      /*******************************************************************************
       ** Fluent setter for listBasePathErrorMessage
       *******************************************************************************/
      public SFTPTestConnectionTestOutput withListBasePathErrorMessage(String listBasePathErrorMessage)
      {
         this.listBasePathErrorMessage = listBasePathErrorMessage;
         return (this);
      }



      /*******************************************************************************
       ** Getter for isListBasePathSuccess
       **
       *******************************************************************************/
      public Boolean getIsListBasePathSuccess()
      {
         return isListBasePathSuccess;
      }



      /*******************************************************************************
       ** Setter for isListBasePathSuccess
       **
       *******************************************************************************/
      public void setIsListBasePathSuccess(Boolean isListBasePathSuccess)
      {
         this.isListBasePathSuccess = isListBasePathSuccess;
      }



      /*******************************************************************************
       ** Fluent setter for isListBasePathSuccess
       **
       *******************************************************************************/
      public SFTPTestConnectionTestOutput withIsListBasePathSuccess(Boolean isListBasePathSuccess)
      {
         this.isListBasePathSuccess = isListBasePathSuccess;
         return (this);
      }

   }

}
