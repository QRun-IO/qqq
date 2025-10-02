/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PVSValueFormatAndFields;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.C3P0PooledConnectionProvider;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.apache.commons.io.IOUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Utility class providing test fixtures and helper methods for PostgreSQL
 ** module tests.
 ** 
 ** This class provides:
 ** - Test metadata definitions (backends, tables, joins, etc.)
 ** - Database initialization and priming utilities
 ** - Common test constants and configuration
 ** - QInstance setup for test scenarios
 *******************************************************************************/
public class TestUtils
{
   public static final String DEFAULT_BACKEND_NAME = "default";
   public static final String MEMORY_BACKEND_NAME  = "memory";

   public static final String TABLE_NAME_PERSON              = "personTable";
   public static final String TABLE_NAME_PERSONAL_ID_CARD    = "personalIdCard";
   public static final String TABLE_NAME_STORE               = "store";
   public static final String TABLE_NAME_ORDER               = "order";
   public static final String TABLE_NAME_ORDER_INSTRUCTIONS  = "orderInstructions";
   public static final String TABLE_NAME_ITEM                = "item";
   public static final String TABLE_NAME_ORDER_LINE          = "orderLine";
   public static final String TABLE_NAME_LINE_ITEM_EXTRINSIC = "orderLineExtrinsic";
   public static final String TABLE_NAME_WAREHOUSE           = "warehouse";
   public static final String TABLE_NAME_WAREHOUSE_STORE_INT = "warehouseStoreInt";

   public static final String SECURITY_KEY_STORE_ALL_ACCESS = "storeAllAccess";

   private static PostgreSQLContainer<?> postgres;



   /*******************************************************************************
    ** Primes the test database by executing SQL statements from a resource file.
    ** 
    ** This method reads a SQL file from the classpath, filters out comment lines,
    ** and executes each statement to set up the test database schema and data.
    ** 
    ** @param sqlFileName the name of the SQL file to execute (in resources)
    ** @throws Exception if database initialization fails
    *******************************************************************************/
   public static void primeTestDatabase(String sqlFileName) throws Exception
   {
      PostgreSQLBackendMetaData backend = TestUtils.defineBackend();

      try(Connection connection = ConnectionManager.getConnection(backend))
      {
         InputStream primeTestDatabaseSqlStream = PostgreSQLBackendModule.class.getResourceAsStream("/" + sqlFileName);
         assertNotNull(primeTestDatabaseSqlStream);
         List<String> lines = IOUtils.readLines(primeTestDatabaseSqlStream, StandardCharsets.UTF_8);
         lines = lines.stream().filter(line -> !line.startsWith("-- ")).toList();
         String joinedSQL = String.join("\n", lines);
         for(String sql : joinedSQL.split(";"))
         {
            if(sql.matches("(?s).*[a-zA-Z0-9_].*"))
            {
               QueryManager.executeUpdate(connection, sql);
            }
         }
      }
   }



   /*******************************************************************************
    ** Defines the QInstance for test scenarios.
    ** 
    ** This creates a complete test instance with:
    ** - PostgreSQL and memory backends
    ** - Test tables (person, order, store, etc.)
    ** - Joins and relationships
    ** - Possible value sources
    ** - Authentication configuration
    ** 
    ** @return the configured QInstance for testing
    *******************************************************************************/
   public static QInstance defineInstance()
   {
      QInstance qInstance = new QInstance();
      qInstance.addBackend(defineBackend());
      qInstance.addBackend(defineMemoryBackend());
      qInstance.addTable(defineTablePerson());
      qInstance.addPossibleValueSource(definePvsPerson());
      qInstance.addTable(defineTablePersonalIdCard());
      qInstance.addJoin(defineJoinPersonAndPersonalIdCard());
      addOmsTablesAndJoins(qInstance);
      qInstance.setAuthentication(defineAuthentication());
      return (qInstance);
   }



   /*******************************************************************************
    ** Defines the in-memory backend used in standard tests.
    ** 
    ** @return the memory backend metadata
    *******************************************************************************/
   public static QBackendMetaData defineMemoryBackend()
   {
      return new QBackendMetaData()
         .withName(MEMORY_BACKEND_NAME)
         .withBackendType(MemoryBackendModule.class);
   }



   /*******************************************************************************
    ** Defines the authentication used in standard tests using 'mock' type.
    ** 
    ** @return the authentication metadata for tests
    *******************************************************************************/
   public static QAuthenticationMetaData defineAuthentication()
   {
      return new QAuthenticationMetaData()
         .withName("mock")
         .withType(QAuthenticationType.MOCK);
   }



   /*******************************************************************************
    ** Sets the PostgreSQL test container for use by test utilities.
    ** 
    ** This must be called before defineBackend() to ensure connection details
    ** are available.
    ** 
    ** @param container the PostgreSQL test container
    *******************************************************************************/
   public static void setPostgresContainer(PostgreSQLContainer<?> container)
   {
      postgres = container;
   }



   /*******************************************************************************
    ** Defines the PostgreSQL backend metadata for tests.
    ** 
    ** This creates a backend configuration using the test container's connection
    ** details and sets up a C3P0 connection pool.
    ** 
    ** @return the PostgreSQL backend metadata
    ** @throws IllegalStateException if the PostgreSQL container hasn't been set
    *******************************************************************************/
   public static PostgreSQLBackendMetaData defineBackend()
   {
      if(postgres == null)
      {
         throw new IllegalStateException("PostgreSQL container not initialized. Call setPostgresContainer first.");
      }

      PostgreSQLBackendMetaData postgresBackendMetaData = new PostgreSQLBackendMetaData()
         .withName(DEFAULT_BACKEND_NAME)
         .withHostName(postgres.getHost())
         .withPort(postgres.getFirstMappedPort())
         .withDatabaseName(postgres.getDatabaseName())
         .withUsername(postgres.getUsername())
         .withPassword(postgres.getPassword());

      postgresBackendMetaData.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));

      return postgresBackendMetaData;
   }



   /*******************************************************************************
    ** Defines the Person table metadata for tests.
    ** 
    ** This table includes fields for personal information such as name, email,
    ** birth date, employment status, and salary information.
    ** 
    ** @return the Person table metadata
    *******************************************************************************/
   public static QTableMetaData defineTablePerson()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_PERSON)
         .withLabel("Person")
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("firstName", "lastName")
         .withBackendName(DEFAULT_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date"))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date"))
         .withField(new QFieldMetaData("firstName", QFieldType.STRING).withBackendName("first_name"))
         .withField(new QFieldMetaData("lastName", QFieldType.STRING).withBackendName("last_name"))
         .withField(new QFieldMetaData("birthDate", QFieldType.DATE).withBackendName("birth_date"))
         .withField(new QFieldMetaData("email", QFieldType.STRING).withBackendName("email"))
         .withField(new QFieldMetaData("isEmployed", QFieldType.BOOLEAN).withBackendName("is_employed"))
         .withField(new QFieldMetaData("annualSalary", QFieldType.DECIMAL).withBackendName("annual_salary"))
         .withField(new QFieldMetaData("daysWorked", QFieldType.INTEGER).withBackendName("days_worked"))
         .withField(new QFieldMetaData("homeTown", QFieldType.STRING).withBackendName("home_town"))
         .withField(new QFieldMetaData("startTime", QFieldType.TIME).withBackendName("start_time"))
         .withBackendDetails(new PostgreSQLTableBackendDetails()
            .withTableName("person"));
   }



   /*******************************************************************************
    ** Defines the possible value source for the Person table.
    ** 
    ** @return the Person possible value source
    *******************************************************************************/
   private static QPossibleValueSource definePvsPerson()
   {
      return (new QPossibleValueSource()
         .withName(TABLE_NAME_PERSON)
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName(TABLE_NAME_PERSON)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY));
   }



   /*******************************************************************************
    ** Defines a 1:1 table with Person for personal ID card information.
    ** 
    ** @return the PersonalIdCard table metadata
    *******************************************************************************/
   private static QTableMetaData defineTablePersonalIdCard()
   {
      return new QTableMetaData()
         .withName(TABLE_NAME_PERSONAL_ID_CARD)
         .withLabel("Personal Id Card")
         .withBackendName(DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails()
            .withTableName("personal_id_card"))
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date"))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME).withBackendName("modify_date"))
         .withField(new QFieldMetaData("personId", QFieldType.INTEGER).withBackendName("person_id"))
         .withField(new QFieldMetaData("idNumber", QFieldType.STRING).withBackendName("id_number"));
   }



   /*******************************************************************************
    ** Defines the one-to-one join between Person and PersonalIdCard tables.
    ** 
    ** @return the join metadata
    *******************************************************************************/
   private static QJoinMetaData defineJoinPersonAndPersonalIdCard()
   {
      return new QJoinMetaData()
         .withLeftTable(TABLE_NAME_PERSON)
         .withRightTable(TABLE_NAME_PERSONAL_ID_CARD)
         .withInferredName()
         .withType(JoinType.ONE_TO_ONE)
         .withJoinOn(new JoinOn("id", "personId"));
   }



   /*******************************************************************************
    ** Adds Order Management System (OMS) tables and joins to the QInstance.
    ** 
    ** This includes tables for stores, orders, items, order lines, warehouses,
    ** and their associated joins and relationships.
    ** 
    ** @param qInstance the instance to add tables and joins to
    *******************************************************************************/
   private static void addOmsTablesAndJoins(QInstance qInstance)
   {
      qInstance.addTable(defineBaseTable(TABLE_NAME_STORE, "store")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("name")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(TABLE_NAME_STORE).withFieldName("id"))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_ORDER, "order")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(TABLE_NAME_STORE).withFieldName("storeId"))
         .withAssociation(new Association().withName("orderLine").withAssociatedTableName(TABLE_NAME_ORDER_LINE).withJoinName("orderJoinOrderLine"))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_ITEM).withJoinPath(List.of("orderJoinOrderLine", "orderLineJoinItem")))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_ORDER_INSTRUCTIONS).withJoinPath(List.of("orderJoinCurrentOrderInstructions")).withLabel("Current Order Instructions"))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER).withBackendName("store_id").withPossibleValueSourceName(TABLE_NAME_STORE))
         .withField(new QFieldMetaData("billToPersonId", QFieldType.INTEGER).withBackendName("bill_to_person_id").withPossibleValueSourceName(TABLE_NAME_PERSON))
         .withField(new QFieldMetaData("shipToPersonId", QFieldType.INTEGER).withBackendName("ship_to_person_id").withPossibleValueSourceName(TABLE_NAME_PERSON))
         .withField(new QFieldMetaData("currentOrderInstructionsId", QFieldType.INTEGER).withBackendName("current_order_instructions_id").withPossibleValueSourceName(TABLE_NAME_PERSON))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_ORDER_INSTRUCTIONS, "order_instructions")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(TABLE_NAME_STORE)
            .withFieldName(TABLE_NAME_ORDER + ".storeId")
            .withJoinNameChain(List.of("orderInstructionsJoinOrder")))
         .withField(new QFieldMetaData("orderId", QFieldType.INTEGER).withBackendName("order_id"))
         .withField(new QFieldMetaData("instructions", QFieldType.STRING))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_ORDER).withJoinPath(List.of("orderInstructionsJoinOrder")))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_ITEM, "item")
         .withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType(TABLE_NAME_STORE).withFieldName("storeId"))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_NAME_ORDER).withJoinPath(List.of("orderLineJoinItem", "orderJoinOrderLine")))
         .withField(new QFieldMetaData("sku", QFieldType.STRING))
         .withField(new QFieldMetaData("description", QFieldType.STRING))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER).withBackendName("store_id").withPossibleValueSourceName(TABLE_NAME_STORE))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_ORDER_LINE, "order_line")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(TABLE_NAME_STORE)
            .withFieldName(TABLE_NAME_ORDER + ".storeId")
            .withJoinNameChain(List.of("orderJoinOrderLine")))
         .withAssociation(new Association().withName("extrinsics").withAssociatedTableName(TABLE_NAME_LINE_ITEM_EXTRINSIC).withJoinName("orderLineJoinLineItemExtrinsic"))
         .withField(new QFieldMetaData("orderId", QFieldType.INTEGER).withBackendName("order_id"))
         .withField(new QFieldMetaData("sku", QFieldType.STRING))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER).withBackendName("store_id").withPossibleValueSourceName(TABLE_NAME_STORE))
         .withField(new QFieldMetaData("quantity", QFieldType.INTEGER))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_LINE_ITEM_EXTRINSIC, "line_item_extrinsic")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(TABLE_NAME_STORE)
            .withFieldName(TABLE_NAME_ORDER + ".storeId")
            .withJoinNameChain(List.of("orderJoinOrderLine", "orderLineJoinLineItemExtrinsic")))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("orderLineId", QFieldType.INTEGER).withBackendName("order_line_id"))
         .withField(new QFieldMetaData("key", QFieldType.STRING))
         .withField(new QFieldMetaData("value", QFieldType.STRING))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_WAREHOUSE_STORE_INT, "warehouse_store_int")
         .withField(new QFieldMetaData("warehouseId", QFieldType.INTEGER).withBackendName("warehouse_id"))
         .withField(new QFieldMetaData("storeId", QFieldType.INTEGER).withBackendName("store_id"))
      );

      qInstance.addTable(defineBaseTable(TABLE_NAME_WAREHOUSE, "warehouse")
         .withRecordSecurityLock(new RecordSecurityLock()
            .withSecurityKeyType(TABLE_NAME_STORE)
            .withFieldName(TABLE_NAME_WAREHOUSE_STORE_INT + ".storeId")
            .withJoinNameChain(List.of(QJoinMetaData.makeInferredJoinName(TestUtils.TABLE_NAME_WAREHOUSE, TestUtils.TABLE_NAME_WAREHOUSE_STORE_INT)))
         )
         .withField(new QFieldMetaData("name", QFieldType.STRING).withBackendName("name"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withType(JoinType.ONE_TO_MANY)
         .withLeftTable(TestUtils.TABLE_NAME_WAREHOUSE)
         .withRightTable(TestUtils.TABLE_NAME_WAREHOUSE_STORE_INT)
         .withInferredName()
         .withJoinOn(new JoinOn("id", "warehouseId"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinStore")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_STORE)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("storeId", "id"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinBillToPerson")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_PERSON)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("billToPersonId", "id"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinShipToPerson")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_PERSON)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("shipToPersonId", "id"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("itemJoinStore")
         .withLeftTable(TABLE_NAME_ITEM)
         .withRightTable(TABLE_NAME_STORE)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("storeId", "id"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinOrderLine")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_ORDER_LINE)
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderId"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineJoinItem")
         .withLeftTable(TABLE_NAME_ORDER_LINE)
         .withRightTable(TABLE_NAME_ITEM)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("sku", "sku"))
         .withJoinOn(new JoinOn("storeId", "storeId"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderLineJoinLineItemExtrinsic")
         .withLeftTable(TABLE_NAME_ORDER_LINE)
         .withRightTable(TABLE_NAME_LINE_ITEM_EXTRINSIC)
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "orderLineId"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderJoinCurrentOrderInstructions")
         .withLeftTable(TABLE_NAME_ORDER)
         .withRightTable(TABLE_NAME_ORDER_INSTRUCTIONS)
         .withType(JoinType.ONE_TO_ONE)
         .withJoinOn(new JoinOn("currentOrderInstructionsId", "id"))
      );

      qInstance.addJoin(new QJoinMetaData()
         .withName("orderInstructionsJoinOrder")
         .withRightTable(TABLE_NAME_ORDER_INSTRUCTIONS)
         .withLeftTable(TABLE_NAME_ORDER)
         .withType(JoinType.MANY_TO_ONE)
         .withJoinOn(new JoinOn("id", "orderId"))
      );

      qInstance.addPossibleValueSource(new QPossibleValueSource()
         .withName("store")
         .withType(QPossibleValueSourceType.TABLE)
         .withTableName(TABLE_NAME_STORE)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY)
      );

      qInstance.addSecurityKeyType(new QSecurityKeyType()
         .withName(TABLE_NAME_STORE)
         .withAllAccessKeyName(SECURITY_KEY_STORE_ALL_ACCESS)
         .withPossibleValueSourceName(TABLE_NAME_STORE));
   }



   /*******************************************************************************
    ** Defines a base table with minimal configuration (id field only).
    ** 
    ** This is a helper method for creating simple table definitions that can
    ** be extended with additional fields.
    ** 
    ** @param tableName the QQQ table name
    ** @param backendTableName the database table name
    ** @return the basic table metadata
    *******************************************************************************/
   private static QTableMetaData defineBaseTable(String tableName, String backendTableName)
   {
      return new QTableMetaData()
         .withName(tableName)
         .withBackendName(DEFAULT_BACKEND_NAME)
         .withBackendDetails(new RDBMSTableBackendDetails().withTableName(backendTableName))
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
   }



   /*******************************************************************************
    ** Executes a query to retrieve all records from a table.
    ** 
    ** This is a convenience method for tests that need to verify table contents.
    ** 
    ** @param tableName the name of the table to query
    ** @return list of records from the table
    ** @throws QException if the query fails
    *******************************************************************************/
   public static List<QRecord> queryTable(String tableName) throws QException
   {
      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);
      return (queryOutput.getRecords());
   }
}
