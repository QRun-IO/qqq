/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.module.mongodb.actions;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.module.mongodb.BaseTest;
import com.kingsrook.qqq.backend.module.mongodb.TestUtils;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Existing _id field mapping accepts supplied values and native generated IDs.
 ** This insert contract does not certify arbitrary manual-key filter behavior.
 *******************************************************************************/
class InsertPrimaryKeyContractTest extends BaseTest
{
   /*******************************************************************************
    ** The existing null-key path delegates generation to the MongoDB driver.
    *******************************************************************************/
   @Test
   void testMissingPrimaryKeyReturnsGeneratedObjectId() throws Exception
   {
      QRecord result = insert(new QRecord().withValue("firstName", "Generated owner"));
      assertThat(result.getErrors()).isEmpty();
      assertNotNull(result.getValueString("id"));
      Document stored = collection().find().first();
      assertNotNull(stored);
      assertThat(stored.get("_id")).isInstanceOf(ObjectId.class);
      assertEquals(stored.getObjectId("_id").toHexString(), result.getValueString("id"));
      assertEquals("Generated owner", stored.getString("firstName"));
      assertEquals(1, collection().countDocuments());
   }



   /*******************************************************************************
    ** recordToDocument preserves a supplied string _id. Its successful native
    ** write must not be misreported as failure while converting the returned key.
    *******************************************************************************/
   @Test
   void testSuppliedStringPrimaryKeyIsReturnedAsItsActualIdentity()
   {
      assertAll(() ->
      {
         QRecord result = insert(new QRecord().withValue("id", "manual-key").withValue("firstName", "Manual owner"));
         assertThat(result.getErrors()).isEmpty();
         assertEquals("manual-key", result.getValueString("id"));
      }, () ->
      {
         Document stored = collection().find(new Document("_id", "manual-key")).first();
         assertNotNull(stored, "Native readback runs even when insert returned a post-write conversion error");
         assertEquals("manual-key", stored.get("_id"));
         assertEquals("Manual owner", stored.getString("firstName"));
         assertEquals(1, collection().countDocuments());
      });
   }



   /*******************************************************************************
    ** MongoDB's actual unique _id constraint preserves an existing manual owner.
    *******************************************************************************/
   @Test
   void testDuplicateSuppliedPrimaryKeyDoesNotOverwriteOwner()
   {
      collection().insertOne(new Document("_id", "manual-key").append("firstName", "Original owner"));
      List<Document> before = collection().find().into(new ArrayList<>());
      assertThrows(QException.class, () -> insert(new QRecord().withValue("id", "manual-key").withValue("firstName", "Overwrite attempt")));
      assertEquals(before, collection().find().into(new ArrayList<>()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insert(QRecord record) throws Exception
   {
      return new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(record)).getRecords().get(0);
   }



   /*******************************************************************************
    ** Read native documents without core or adapter query key conversion.
    *******************************************************************************/
   private MongoCollection<Document> collection()
   {
      return getMongoClient().getDatabase(TestUtils.MONGO_DATABASE).getCollection(TestUtils.TABLE_NAME_PERSON);
   }
}
