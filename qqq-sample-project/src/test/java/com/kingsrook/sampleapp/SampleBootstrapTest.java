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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A different database with the same backend name must never receive sample resets.
 *******************************************************************************/
class SampleBootstrapTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testResetPreservesAnotherDatabaseWithTheSameBackendName() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      RDBMSBackendMetaData other = SampleMetaDataProvider.defineRdbmsBackend().withDatabaseName("other_sample_bootstrap_test");
      try(Connection connection = ConnectionManager.getConnection(other);
         Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE person (id INTEGER)");
         statement.execute("INSERT INTO person (id) VALUES (123)");
         SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");

         try(Connection sampleConnection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
            Statement sampleStatement = sampleConnection.createStatement();
            ResultSet sampleResult = sampleStatement.executeQuery("SELECT first_name FROM person WHERE id = 1"))
         {
            assertEquals("jdbc:h2:mem:test_database", sampleConnection.getMetaData().getURL());
            assertTrue(sampleResult.next());
            assertEquals("Avery", sampleResult.getString(1));
            assertFalse(sampleResult.next());
         }

         try(ResultSet result = statement.executeQuery("SELECT id FROM person"))
         {
            assertTrue(result.next());
            assertEquals(123, result.getInt(1));
            assertFalse(result.next());
         }
      }
      finally
      {
         ConnectionManager.resetConnectionProviders();
      }
   }
}
