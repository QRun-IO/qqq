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
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
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
class MongoDBCountActionTest extends BaseTest
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

      CountInput countInput = new CountInput();
      countInput.setTableName(TestUtils.TABLE_NAME_PERSON);
      assertEquals(4, new CountAction().execute(countInput).getCount());

      countInput.setFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "Tylers")));
      assertEquals(2, new CountAction().execute(countInput).getCount());

      countInput.setFilter(new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.EQUALS, "assdf")));
      assertEquals(0, new CountAction().execute(countInput).getCount());
   }

}