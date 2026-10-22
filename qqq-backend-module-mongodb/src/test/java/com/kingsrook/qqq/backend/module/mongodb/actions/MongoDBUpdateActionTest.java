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


import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertManyResult;
import org.bson.BsonValue;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for MongoDBUpdateAction
 *******************************************************************************/
class MongoDBUpdateActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      ////////////////////////////////////////
      // directly insert some mongo records //
      ////////////////////////////////////////
      MongoDatabase             database   = getMongoClient().getDatabase(TestUtils.MONGO_DATABASE);
      MongoCollection<Document> collection = database.getCollection(TestUtils.TABLE_NAME_PERSON);
      InsertManyResult insertManyResult = collection.insertMany(List.of(
         Document.parse("""
            {"metaData": {"createDate": "2023-01-09T03:03:03.123Z", "modifyDate": "2023-01-09T04:04:04.123Z"}, "firstName": "Tylers", "lastName": "Sample"}""")
      ));
      BsonValue insertedId = insertManyResult.getInsertedIds().values().iterator().next();

      ////////////////////////////////////
      // update using qqq update action //
      ////////////////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", insertedId.asObjectId().getValue().toString()).withValue("firstName", "Tyler").withValue("lastName", "Sample")
      ));
      UpdateOutput updateOutput = new UpdateAction().execute(updateInput);

      /////////////////////////////////////////////////
      // directly query mongo for the updated record //
      /////////////////////////////////////////////////
      Document document = collection.find(new Document("firstName", "Tyler")).first();
      assertNotNull(document);
      assertEquals("Tyler", document.get("firstName"));
      assertNotEquals(Instant.parse("2023-01-09T04:04:04.123Z"), ((Document) document.get("metaData")).get("modifyDate"));
   }

}