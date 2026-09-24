/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A cached connection for a different database must never receive sample resets.
 *******************************************************************************/
class SampleBootstrapTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testResetRefusesAnotherDatabaseWithTheSameBackendName() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      RDBMSBackendMetaData other = SampleMetaDataProvider.defineRdbmsBackend().withDatabaseName("other_sample_bootstrap_test");
      try(Connection connection = ConnectionManager.getConnection(other);
         Statement statement = connection.createStatement())
      {
         statement.execute("CREATE TABLE person (id INTEGER)");
         statement.execute("INSERT INTO person (id) VALUES (123)");
         IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql"));
         assertTrue(error.getMessage().contains("owned in-memory H2"));
         try(ResultSet result = statement.executeQuery("SELECT id FROM person"))
         {
            assertTrue(result.next());
            assertEquals(123, result.getInt(1));
         }
      }
      finally
      {
         ConnectionManager.resetConnectionProviders();
      }
   }
}
