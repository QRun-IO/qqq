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

package com.kingsrook.qqq.backend.module.filesystem.sftp.model.metadata;


import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.AbstractFilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.sftp.SFTPBackendModule;


/*******************************************************************************
 ** SFTP backend meta data.
 *******************************************************************************/
public class SFTPBackendMetaData extends AbstractFilesystemBackendMetaData
{
   private String  username;
   private String  password;
   private String  hostName;
   private byte[]  privateKey;
   private Integer port;



   /*******************************************************************************
    ** Default Constructor.
    *******************************************************************************/
   public SFTPBackendMetaData()
   {
      super();
      setBackendType(SFTPBackendModule.class);
   }



   /*******************************************************************************
    ** Fluent setter for basePath
    **
    *******************************************************************************/
   public SFTPBackendMetaData withBasePath(String basePath)
   {
      setBasePath(basePath);
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public SFTPBackendMetaData withName(String name)
   {
      setName(name);
      return this;
   }



   /*******************************************************************************
    ** Getter for username
    *******************************************************************************/
   public String getUsername()
   {
      return (this.username);
   }



   /*******************************************************************************
    ** Setter for username
    *******************************************************************************/
   public void setUsername(String username)
   {
      this.username = username;
   }



   /*******************************************************************************
    ** Fluent setter for username
    *******************************************************************************/
   public SFTPBackendMetaData withUsername(String username)
   {
      this.username = username;
      return (this);
   }



   /*******************************************************************************
    ** Getter for password
    *******************************************************************************/
   public String getPassword()
   {
      return (this.password);
   }



   /*******************************************************************************
    ** Setter for password
    *******************************************************************************/
   public void setPassword(String password)
   {
      this.password = password;
   }



   /*******************************************************************************
    ** Fluent setter for password
    *******************************************************************************/
   public SFTPBackendMetaData withPassword(String password)
   {
      this.password = password;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hostName
    *******************************************************************************/
   public String getHostName()
   {
      return (this.hostName);
   }



   /*******************************************************************************
    ** Setter for hostName
    *******************************************************************************/
   public void setHostName(String hostName)
   {
      this.hostName = hostName;
   }



   /*******************************************************************************
    ** Fluent setter for hostName
    *******************************************************************************/
   public SFTPBackendMetaData withHostName(String hostName)
   {
      this.hostName = hostName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for port
    *******************************************************************************/
   public Integer getPort()
   {
      return (this.port);
   }



   /*******************************************************************************
    ** Setter for port
    *******************************************************************************/
   public void setPort(Integer port)
   {
      this.port = port;
   }



   /*******************************************************************************
    ** Fluent setter for port
    *******************************************************************************/
   public SFTPBackendMetaData withPort(Integer port)
   {
      this.port = port;
      return (this);
   }



   /*******************************************************************************
    ** Getter for privateKey
    *******************************************************************************/
   public byte[] getPrivateKey()
   {
      return (this.privateKey);
   }



   /*******************************************************************************
    ** Setter for privateKey
    *******************************************************************************/
   public void setPrivateKey(byte[] privateKey)
   {
      this.privateKey = privateKey;
   }



   /*******************************************************************************
    ** Fluent setter for privateKey
    *******************************************************************************/
   public SFTPBackendMetaData withPrivateKey(byte[] privateKey)
   {
      this.privateKey = privateKey;
      return (this);
   }

}
