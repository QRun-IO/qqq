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

package com.kingsrook.qqq.backend.module.mongodb;


import java.lang.reflect.Field;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Verifies MongoDB fixture startup readiness and per-class ownership.
 ******************************************************************************/
class MongoFixtureLifecycleTest
{
   /***************************************************************************
    ** Mongo's initialization server emits the same ready log as its final
    ** server, but only the latter accepts connections on the mapped port.
    ***************************************************************************/
   @Test
   void startupWaitsForMappedMongoPort() throws Exception
   {
      BaseTest owner = new BaseTest();
      try
      {
         owner.beforeAll();
         owner.baseBeforeEach();
         try(MongoClient client = BaseTest.getMongoClient())
         {
            client.getDatabase(TestUtils.MONGO_DATABASE).runCommand(new Document("ping", 1));
         }
      }
      finally
      {
         QContext.clear();
         GenericContainer<?> container = container(owner);
         if(container != null)
         {
            container.stop();
         }
      }
   }



   /***************************************************************************
    ** Two owners overlap, as they can when test classes execute concurrently.
    ***************************************************************************/
   @Test
   void closingOneOwnerLeavesTheOtherMongoAvailable() throws Exception
   {
      BaseTest first = new BaseTest();
      BaseTest second = new BaseTest();
      GenericContainer<?> firstContainer = null;
      GenericContainer<?> secondContainer = null;
      try
      {
         first.beforeAll();
         firstContainer = container(first);
         second.beforeAll();
         secondContainer = container(second);
         first.afterAll();

         assertTrue(secondContainer.isRunning(), "closing the first fixture stopped the second container");
         second.baseBeforeEach();
         try(MongoClient client = BaseTest.getMongoClient())
         {
            client.getDatabase(TestUtils.MONGO_DATABASE).runCommand(new Document("ping", 1));
         }
      }
      finally
      {
         QContext.clear();
         if(secondContainer != null)
         {
            secondContainer.stop();
         }
         if(firstContainer != null)
         {
            firstContainer.stop();
         }
      }
   }



   /***************************************************************************
    ** Keep both handles for cleanup even if the old shared field is overwritten.
    ***************************************************************************/
   private GenericContainer<?> container(BaseTest owner) throws Exception
   {
      Field field = BaseTest.class.getDeclaredField("mongoDBContainer");
      field.setAccessible(true);
      return (GenericContainer<?>) field.get(owner);
   }
}
