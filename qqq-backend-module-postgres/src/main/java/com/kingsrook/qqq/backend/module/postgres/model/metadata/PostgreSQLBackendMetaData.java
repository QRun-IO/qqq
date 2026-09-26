/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.model.metadata;


import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.module.postgres.PostgreSQLBackendModule;
import com.kingsrook.qqq.backend.module.postgres.fieldfunctions.PostgreSQLRDBMSWeekdayOfDateFunction;
import com.kingsrook.qqq.backend.module.postgres.fieldfunctions.PostgreSQLRDBMSWeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.module.postgres.strategy.PostgreSQLRDBMSActionStrategy;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.strategy.RDBMSActionStrategyInterface;
import org.postgresql.Driver;


/*******************************************************************************
 ** Meta-data to provide details of a PostgreSQL backend (e.g., connection
 ** params).
 ** 
 ** This class encapsulates PostgreSQL-specific backend configuration including
 ** connection details (hostname, port, database name) and provides methods to
 ** construct JDBC connection strings and retrieve the appropriate JDBC driver.
 ******************************************************************************/
public class PostgreSQLBackendMetaData extends RDBMSBackendMetaData
{
   public static final int DEFAULT_POSTGRES_PORT = 5432;



   /***************************************************************************
    ** Default constructor. Initializes the backend metadata with PostgreSQL
    ** vendor settings and backend type.
    ***************************************************************************/
   public PostgreSQLBackendMetaData()
   {
      super();
      setVendor("postgres");
      setBackendType(PostgreSQLBackendModule.class);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void doRegisterFieldFunctionAdapters()
   {
      super.doRegisterFieldFunctionAdapters();
      registerBackendFieldFunctionAdapter(WeekdayOfDateFunction.IDENTIFIER, PostgreSQLRDBMSWeekdayOfDateFunction.class);
      registerBackendFieldFunctionAdapter(WeekdayOfDateTimeFunction.IDENTIFIER, PostgreSQLRDBMSWeekdayOfDateTimeFunction.class);
   }



   /***************************************************************************
    ** Build the JDBC connection string for PostgreSQL.
    **
    ** @return the JDBC connection string
    ***************************************************************************/
   @Override
   public String buildConnectionString()
   {
      StringBuilder url = new StringBuilder("jdbc:postgresql://");
      url.append(getHostName());

      if(getPort() != null)
      {
         url.append(":").append(getPort());
      }
      else
      {
         url.append(":").append(DEFAULT_POSTGRES_PORT);
      }

      url.append("/").append(getDatabaseName());

      return url.toString();
   }



   /***************************************************************************
    ** Get the PostgreSQL JDBC driver class name.
    **
    ** @return the driver class name
    ***************************************************************************/
   @Override
   public String getJdbcDriverClassName()
   {
      return (Driver.class.getName());
   }



   /***************************************************************************
    ** Fluent setter for name.
    **
    ** @param name the backend name
    ** @return this instance for fluent chaining
    ***************************************************************************/
   public PostgreSQLBackendMetaData withName(String name)
   {
      setName(name);
      return (this);
   }



   /***************************************************************************
    ** Fluent setter for hostName.
    **
    ** @param hostName the database host name
    ** @return this instance for fluent chaining
    ***************************************************************************/
   public PostgreSQLBackendMetaData withHostName(String hostName)
   {
      setHostName(hostName);
      return (this);
   }



   /***************************************************************************
    ** Fluent setter for port.
    **
    ** @param port the database port
    ** @return this instance for fluent chaining
    ***************************************************************************/
   public PostgreSQLBackendMetaData withPort(Integer port)
   {
      setPort(port);
      return (this);
   }



   /***************************************************************************
    ** Fluent setter for databaseName.
    **
    ** @param databaseName the database name
    ** @return this instance for fluent chaining
    ***************************************************************************/
   public PostgreSQLBackendMetaData withDatabaseName(String databaseName)
   {
      setDatabaseName(databaseName);
      return (this);
   }



   /***************************************************************************
    ** Fluent setter for username.
    **
    ** @param username the database username
    ** @return this instance for fluent chaining
    ***************************************************************************/
   public PostgreSQLBackendMetaData withUsername(String username)
   {
      setUsername(username);
      return (this);
   }



   /***************************************************************************
    ** Fluent setter for password.
    **
    ** @param password the database password
    ** @return this instance for fluent chaining
    ***************************************************************************/
   public PostgreSQLBackendMetaData withPassword(String password)
   {
      setPassword(password);
      return (this);
   }



   /***************************************************************************
    ** Get the action strategy for PostgreSQL operations.
    **
    ** @return the action strategy instance
    ***************************************************************************/
   public RDBMSActionStrategyInterface getActionStrategy()
   {
      if(getActionStrategyField() == null)
      {
         if(getActionStrategyCodeReference() != null)
         {
            setActionStrategyField(QCodeLoader.getAdHoc(
               RDBMSActionStrategyInterface.class,
               getActionStrategyCodeReference()));
         }
         else
         {
            setActionStrategyField(new PostgreSQLRDBMSActionStrategy());
         }
      }

      return (getActionStrategyField());
   }
}
