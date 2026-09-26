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

package com.kingsrook.qqq.backend.module.rdbms.jdbc;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import static com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager.getJdbcUrl;


/*******************************************************************************
 ** Simple connection provider - no pooling, just opens a new connection for
 ** every request.
 *******************************************************************************/
public class SimpleConnectionProvider implements ConnectionProviderInterface
{
   private RDBMSBackendMetaData backend;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void init(RDBMSBackendMetaData backend)
   {
      this.backend = backend;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Connection getConnection() throws SQLException
   {
      String jdbcURL = getJdbcUrl(backend);
      Connection connection = DriverManager.getConnection(jdbcURL, backend.getUsername(), backend.getPassword());

      if(CollectionUtils.nullSafeHasContents(backend.getQueriesForNewConnections()))
      {
         runQueriesForNewConnections(connection);
      }

      return (connection);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void runQueriesForNewConnections(Connection connection) throws SQLException
   {
      for(String sql : backend.getQueriesForNewConnections())
      {
         Statement statement = connection.createStatement();
         statement.execute(sql);
      }
   }

}
