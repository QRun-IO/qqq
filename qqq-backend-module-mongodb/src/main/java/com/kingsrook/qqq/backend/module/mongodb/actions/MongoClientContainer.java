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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;


/*******************************************************************************
 ** Wrapper around a MongoClient, ClientSession, and a boolean to help signal
 ** where it was opened (e.g., so you know if you need to close it yourself, or
 ** if it came from someone else (e.g., via an input transaction)).
 *******************************************************************************/
public class MongoClientContainer
{
   private MongoClient   mongoClient;
   private ClientSession mongoSession;
   private boolean       needToClose;



   /*******************************************************************************
    **
    *******************************************************************************/
   public MongoClientContainer(MongoClient mongoClient, ClientSession mongoSession, boolean needToClose)
   {
      this.mongoClient = mongoClient;
      this.mongoSession = mongoSession;
      this.needToClose = needToClose;
   }



   /*******************************************************************************
    ** Getter for mongoClient
    *******************************************************************************/
   public MongoClient getMongoClient()
   {
      return (this.mongoClient);
   }



   /*******************************************************************************
    ** Setter for mongoClient
    *******************************************************************************/
   public void setMongoClient(MongoClient mongoClient)
   {
      this.mongoClient = mongoClient;
   }



   /*******************************************************************************
    ** Fluent setter for mongoClient
    *******************************************************************************/
   public MongoClientContainer withMongoClient(MongoClient mongoClient)
   {
      this.mongoClient = mongoClient;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mongoSession
    *******************************************************************************/
   public ClientSession getMongoSession()
   {
      return (this.mongoSession);
   }



   /*******************************************************************************
    ** Setter for mongoSession
    *******************************************************************************/
   public void setMongoSession(ClientSession mongoSession)
   {
      this.mongoSession = mongoSession;
   }



   /*******************************************************************************
    ** Fluent setter for mongoSession
    *******************************************************************************/
   public MongoClientContainer withMongoSession(ClientSession mongoSession)
   {
      this.mongoSession = mongoSession;
      return (this);
   }



   /*******************************************************************************
    ** Getter for needToClose
    *******************************************************************************/
   public boolean getNeedToClose()
   {
      return (this.needToClose);
   }



   /*******************************************************************************
    ** Setter for needToClose
    *******************************************************************************/
   public void setNeedToClose(boolean needToClose)
   {
      this.needToClose = needToClose;
   }



   /*******************************************************************************
    ** Fluent setter for needToClose
    *******************************************************************************/
   public MongoClientContainer withNeedToClose(boolean needToClose)
   {
      this.needToClose = needToClose;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void closeIfNeeded()
   {
      if(needToClose)
      {
         mongoSession.close();
         mongoClient.close();
      }
   }
}
