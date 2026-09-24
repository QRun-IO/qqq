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

package com.kingsrook.qqq.middleware.picocli;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionOutputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QRecordListMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.apache.commons.io.IOUtils;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Utility methods for unit tests.
 **
 *******************************************************************************/
public class TestUtils
{

   /*******************************************************************************
    ** Prime a test database (e.g., h2, in-memory)
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static void primeTestDatabase() throws Exception
   {
      ConnectionManager connectionManager = new ConnectionManager();
      try(Connection connection = connectionManager.getConnection(TestUtils.defineBackend()))
      {
         InputStream primeTestDatabaseSqlStream = TestUtils.class.getResourceAsStream("/prime-test-database.sql");
         assertNotNull(primeTestDatabaseSqlStream);
         List<String> lines = (List<String>) IOUtils.readLines(primeTestDatabaseSqlStream, StandardCharsets.UTF_8);
         lines = lines.stream().filter(line -> !line.startsWith("-- ")).toList();
         String joinedSQL = String.join("\n", lines);
         for(String sql : joinedSQL.split(";"))
         {
            QueryManager.executeUpdate(connection, sql);
         }
      }
   }



   /*******************************************************************************
    ** Run an SQL Query in the test database
    **
    *******************************************************************************/
   public static void runTestSql(String sql, QueryManager.ResultSetProcessor resultSetProcessor) throws Exception
   {
      ConnectionManager connectionManager = new ConnectionManager();
      try(Connection connection = connectionManager.getConnection(defineBackend()))
      {
         QueryManager.executeStatement(connection, sql, resultSetProcessor);
      }
   }



   /*******************************************************************************
    ** Define the q-instance for testing (h2 rdbms and 'person' table)
    **
    *******************************************************************************/
   public static QInstance defineInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), defineAuthentication());
      qInstance.addBackend(defineBackend());
      qInstance.addTable(defineTablePerson());
      qInstance.addProcess(defineProcessGreetPeople());
      return (qInstance);
   }



   /*******************************************************************************
    ** Define the authentication used in standard tests - using 'mock' type.
    **
    *******************************************************************************/
   private static QAuthenticationMetaData defineAuthentication()
   {
      return new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK);
   }



   /*******************************************************************************
    ** Define the h2 rdbms backend
    **
    *******************************************************************************/
   public static RDBMSBackendMetaData defineBackend()
   {
      return (new RDBMSBackendMetaData()
         .withVendor("h2")
         .withHostName("mem")
         .withDatabaseName("test_database")
         .withUsername("sa")
         .withPassword("")
         .withName("default"));
   }



   /*******************************************************************************
    ** Define the person table
    **
    *******************************************************************************/
   public static QTableMetaData defineTablePerson()
   {
      return new QTableMetaData()
         .withName("person")
         .withLabel("Person")
         .withBackendName(defineBackend().getName())
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date"))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date"))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING).withBackendName("first_name"))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING).withBackendName("last_name"))
         .withField(new QFieldMetaData("birthDate", QFieldType.DATE).withBackendName("birth_date"))
         .withField(new QFieldMetaData("email", QFieldType.STRING));
   }



   /*******************************************************************************
    ** Define the 'greet people' process
    *******************************************************************************/
   private static QProcessMetaData defineProcessGreetPeople()
   {
      return new QProcessMetaData()
         .withName("greet")
         .withTableName("person")
         .withStep(new QBackendStepMetaData()
            .withName("prepare")
            .withCode(new QCodeReference()
               .withName(MockBackendStep.class.getName())
               .withCodeType(QCodeType.JAVA))
            .withInputData(new QFunctionInputMetaData()
               .withRecordListMetaData(new QRecordListMetaData().withTableName("person"))
               .withFieldList(List.of(
                  new QFieldMetaData("greetingPrefix", QFieldType.STRING),
                  new QFieldMetaData("greetingSuffix", QFieldType.STRING)
               )))
            .withOutputMetaData(new QFunctionOutputMetaData()
               .withRecordListMetaData(new QRecordListMetaData()
                  .withTableName("person")
                  .withField(new QFieldMetaData("fullGreeting", QFieldType.STRING))
               )
               .withFieldList(List.of(new QFieldMetaData("outputMessage", QFieldType.STRING))))
         );
   }

}
