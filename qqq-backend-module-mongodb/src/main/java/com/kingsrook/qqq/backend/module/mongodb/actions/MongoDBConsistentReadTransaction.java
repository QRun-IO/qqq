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

package com.kingsrook.qqq.backend.module.mongodb.actions;


import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.module.mongodb.model.metadata.MongoDBBackendMetaData;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;


/*******************************************************************************
 ** MongoDB transaction opened by MongoDBBackendModule.openConsistentReadTransaction -
 ** wrapping a session that has snapshot read concern (if the server supports
 ** it).  Never starts a mongo transaction (snapshot sessions can't have one),
 ** and so is for reads only.
 *******************************************************************************/
public class MongoDBConsistentReadTransaction extends MongoDBTransaction
{
   private final ConsistencyGuarantee consistencyGuarantee;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public MongoDBConsistentReadTransaction(MongoDBBackendMetaData backend, MongoClient mongoClient, ClientSession clientSession, ConsistencyGuarantee consistencyGuarantee)
   {
      super(backend, mongoClient, clientSession, false);
      this.consistencyGuarantee = consistencyGuarantee;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ConsistencyGuarantee getConsistencyGuarantee()
   {
      return (consistencyGuarantee);
   }

}
