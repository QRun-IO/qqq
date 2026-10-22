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


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for MongoDBQueryAction 
 *******************************************************************************/
class MongoDBDeleteActionTest extends BaseTest
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
      collection.insertMany(List.of(
         Document.parse("""
            {"firstName": "Darin", "lastName": "Kelkhoff"}"""),
         Document.parse("""
            {"firstName": "Tylers", "lastName": "Sample"}"""),
         Document.parse("""
            {"firstName": "Tylers", "lastName": "Simple"}"""),
         Document.parse("""
            {"firstName": "Thom", "lastName": "Chutterloin"}""")
      ));
      assertEquals(4, collection.countDocuments());

      //////////////////////////////////////////
      // do a delete by id (look it up first) //
      //////////////////////////////////////////
      {
         QueryInput queryInput = new QueryInput();
         queryInput.setTableName(TestUtils.TABLE_NAME_PERSON);
         queryInput.setFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Darin")));
         QueryOutput queryOutput = new QueryAction().execute(queryInput);
         String      id0         = queryOutput.getRecords().get(0).getValueString("id");

         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(TestUtils.TABLE_NAME_PERSON);
         deleteInput.setPrimaryKeys(List.of(id0));
         assertEquals(1, new DeleteAction().execute(deleteInput).getDeletedRecordCount());
      }
      assertEquals(3, collection.countDocuments());

      ///////////////////////////
      // do a delete by filter //
      ///////////////////////////
      {
         DeleteInput deleteInput = new DeleteInput();
         deleteInput.setTableName(TestUtils.TABLE_NAME_PERSON);
         deleteInput.setQueryFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Tylers")));
         assertEquals(2, new DeleteAction().execute(deleteInput).getDeletedRecordCount());
      }
      assertEquals(1, collection.countDocuments());
   }

}