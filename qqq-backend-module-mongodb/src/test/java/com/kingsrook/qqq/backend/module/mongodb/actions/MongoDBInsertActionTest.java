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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for MongoDBQueryAction 
 *******************************************************************************/
class MongoDBInsertActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "Darin")
            .withValue("unmappedField", 1701)
            .withValue("unmappedList", new ArrayList<>(List.of("A", "B", "C")))
            .withValue("unmappedObject", new HashMap<>(Map.of("A", 1, "C", true))),
         new QRecord().withValue("firstName", "Tim"),
         new QRecord().withValue("firstName", "Tyler")
      ));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      /////////////////////////////////////////
      // make sure id got put on all records //
      /////////////////////////////////////////
      for(QRecord record : insertOutput.getRecords())
      {
         assertNotNull(record.getValueString("id"));
      }

      ///////////////////////////////////////////////////
      // directly query mongo for the inserted records //
      ///////////////////////////////////////////////////
      MongoDatabase             database   = getMongoClient().getDatabase(TestUtils.MONGO_DATABASE);
      MongoCollection<Document> collection = database.getCollection(TestUtils.TABLE_NAME_PERSON);
      assertEquals(3, collection.countDocuments());
      for(Document document : collection.find())
      {
         /////////////////////////////////////////////////////////////
         // make sure values got set - including some nested values //
         /////////////////////////////////////////////////////////////
         assertNotNull(document.get("firstName"));
         assertNotNull(document.get("metaData"));
         assertThat(document.get("metaData")).isInstanceOf(Document.class);
         assertNotNull(((Document) document.get("metaData")).get("createDate"));
      }

      Document document = collection.find(new Document("firstName", "Darin")).first();
      assertNotNull(document);
      assertEquals(1701, document.get("unmappedField"));
      assertEquals(List.of("A", "B", "C"), document.get("unmappedList"));
      assertEquals(Map.of("A", 1, "C", true), document.get("unmappedObject"));
   }

}